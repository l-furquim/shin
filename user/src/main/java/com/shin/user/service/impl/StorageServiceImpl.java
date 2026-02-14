package com.shin.user.service.impl;

import com.shin.user.exceptions.PictureUploadException;
import com.shin.user.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class StorageServiceImpl implements StorageService {

    private final S3Client client;

    @Value("${spring.cloud.aws.s3.buckets.creator-pictures}")
    private String bucket;

    @Value("${spring.cloud.aws.cloudfront}")
    private String cloudFrontUrl;

    @Override
    public void uploadAvatar(MultipartFile file, UUID id) {
        try {
            String key = "creators/" + id.toString() + "/" + "avatar.png";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            log.info("Avatar uploaded successfully for creator with ID: {}", id);

        } catch (Exception e) {
            log.error("Error while uploading the avatar: {}", e.getMessage());
            throw new PictureUploadException("Error while uploading the avatar");
        }
    }

    @Override
    public void uploadBanner(MultipartFile file, UUID id) {
        try {
            String key = "creators/" + id.toString() + "/"  + "banner.png";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            log.info("Banner uploaded successfully for creator with ID: {}", id);

        } catch (Exception e) {
            log.error("Error while uploading the banner: {}", e.getMessage());
            throw new PictureUploadException("Error while uploading the avatar");
        }
    }

    @Override
    public String[] getAvatarAndBannerUrls(UUID id) {
        return new String[] {
                cloudFrontUrl + "/creators/" + id.toString() + "/"  + "avatar.png",
                cloudFrontUrl + "/creators/" + id.toString() + "/"  + "banner.png"
        };
    }

    @Override
    public void deleteAvatar(UUID id) {
        try {
            String key = "creators/" + id.toString() + "/"  + "avatar.png";

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            client.deleteObject(deleteRequest);
        } catch (Exception e) {
            log.error("Error deleting from S3: {}", e.getMessage(), e);
            throw new PictureUploadException("Error deleting from storage.");
        }
    }

    @Override
    public void deleteBanner(UUID id) {
        try {
            String key = "creators/" + id.toString() + "/"  + "banner.png";

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            client.deleteObject(deleteRequest);
        } catch (Exception e) {
            log.error("Error deleting from S3: {}", e.getMessage(), e);
            throw new PictureUploadException("Error deleting from storage.");
        }
    }

    @Override
    public void deleteCreatorPictures(UUID id) {
        deleteAvatar(id);
        deleteBanner(id);
    }
}
