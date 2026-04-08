package com.shin.interaction.config;

import com.shin.commons.annotations.TableName;
import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver;
import org.springframework.stereotype.Component;

@Component
class DynamoDBTableNameResolver implements DynamoDbTableNameResolver {

    @Override
    public <T> String resolve(Class<T> clazz) {
        return clazz.getAnnotation(TableName.class).name();
    }
}
