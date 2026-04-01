package com.shin.user.service.impl;

import com.shin.user.dto.CreateSubscriptionResponse;
import com.shin.user.dto.GetCreatorSubscriptionsResponse;
import com.shin.user.dto.RemoveSubscriptionResponse;
import com.shin.user.UserApplication;
import com.shin.user.repository.CreatorRepository;
import com.shin.user.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UserApplication.class)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.password=",
        "spring.data.redis.repositories.enabled=false",
        "spring.cloud.aws.s3.buckets.creator-pictures=test-bucket",
        "spring.cloud.aws.cloudfront=https://cdn.test.local",
        "api.version=/v1"
})
class SubscriptionServiceImplIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shin_user_test")
            .withUsername("root")
            .withPassword("root")
            .withInitScript("db/test-init/01-create-user-schema.sql");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        if (!postgres.isRunning()) {
            postgres.start();
        }

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.schemas", () -> "user");
    }

    @Autowired
    private SubscriptionServiceImpl subscriptionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockBean(name = "redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private S3Client s3Client;

    private ValueOperations<String, String> valueOperations;
    private Map<String, String> redisMemory;

    @BeforeEach
    void setUpRedisMock() {
        redisMemory = new ConcurrentHashMap<>();
        valueOperations = Mockito.mock(ValueOperations.class);

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(valueOperations.get(Mockito.anyString()))
                .thenAnswer(invocation -> redisMemory.get(invocation.getArgument(0, String.class)));

        Mockito.doAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            String value = invocation.getArgument(1, String.class);
            redisMemory.put(key, value);
            return null;
        }).when(valueOperations).set(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class));

        Mockito.doAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            redisMemory.remove(key);
            return true;
        }).when(redisTemplate).delete(Mockito.anyString());
    }

    @AfterEach
    void cleanUp() {
        subscriptionRepository.deleteAll();
        creatorRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM \"user\".users");
        redisMemory.clear();
    }

    @Test
    void subscribeAndUnsubscribeShouldBeIdempotentAndKeepCountConsistent() {
        UUID followerId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        createUser(followerId, "follower@test.com");
        createUser(channelId, "creator@test.com");
        createCreator(channelId, "creator_channel", "creator-url");

        CreateSubscriptionResponse firstSubscribe = subscriptionService.subscribe(followerId, channelId);
        assertThat(firstSubscribe.subscribed()).isTrue();
        assertThat(firstSubscribe.subscribersCount()).isEqualTo(1L);
        assertThat(subscriptionRepository.countByIdChannelId(channelId)).isEqualTo(1L);
        assertThat(readSubscribersCount(channelId)).isEqualTo(1L);

        CreateSubscriptionResponse secondSubscribe = subscriptionService.subscribe(followerId, channelId);
        assertThat(secondSubscribe.subscribed()).isTrue();
        assertThat(secondSubscribe.subscribersCount()).isEqualTo(1L);
        assertThat(subscriptionRepository.countByIdChannelId(channelId)).isEqualTo(1L);
        assertThat(readSubscribersCount(channelId)).isEqualTo(1L);

        GetCreatorSubscriptionsResponse infoAfterSubscribe = subscriptionService.getSubscriptionInfo(followerId, channelId);
        assertThat(infoAfterSubscribe.subscribed()).isTrue();
        assertThat(infoAfterSubscribe.subscribers()).isEqualTo(1L);

        RemoveSubscriptionResponse firstUnsubscribe = subscriptionService.unsubscribe(followerId, channelId);
        assertThat(firstUnsubscribe.subscribed()).isFalse();
        assertThat(firstUnsubscribe.subscribersCount()).isEqualTo(0L);
        assertThat(subscriptionRepository.countByIdChannelId(channelId)).isEqualTo(0L);
        assertThat(readSubscribersCount(channelId)).isEqualTo(0L);

        RemoveSubscriptionResponse secondUnsubscribe = subscriptionService.unsubscribe(followerId, channelId);
        assertThat(secondUnsubscribe.subscribed()).isFalse();
        assertThat(secondUnsubscribe.subscribersCount()).isEqualTo(0L);
        assertThat(subscriptionRepository.countByIdChannelId(channelId)).isEqualTo(0L);
        assertThat(readSubscribersCount(channelId)).isEqualTo(0L);

        GetCreatorSubscriptionsResponse infoAfterUnsubscribe = subscriptionService.getSubscriptionInfo(followerId, channelId);
        assertThat(infoAfterUnsubscribe.subscribed()).isFalse();
        assertThat(infoAfterUnsubscribe.subscribers()).isEqualTo(0L);
    }

    @Test
    void cacheWritesShouldHappenOnlyAfterTransactionCommit() {
        UUID followerId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        createUser(followerId, "follower2@test.com");
        createUser(channelId, "creator2@test.com");
        createCreator(channelId, "creator_channel_2", "creator-url-2");

        subscriptionService.subscribe(followerId, channelId);
        assertThat(redisMemory).isNotEmpty();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }

    private long readSubscribersCount(UUID channelId) {
        Long count = creatorRepository.findSubscribersCount(channelId);
        return count != null ? count : 0L;
    }

    private void createUser(UUID id, String email) {
        jdbcTemplate.update(
                "INSERT INTO \"user\".users (id, display_name, email, show_adult_content, password, locale) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                "name-" + id,
                email,
                true,
                "password",
                "en"
        );
    }

    private void createCreator(UUID id, String username, String channelUrl) {
        jdbcTemplate.update(
                "INSERT INTO \"user\".creators (id, username, description, channel_url, subscribers_count) VALUES (?, ?, ?, ?, ?)",
                id,
                username,
                "description",
                channelUrl,
                0L
        );
    }
}
