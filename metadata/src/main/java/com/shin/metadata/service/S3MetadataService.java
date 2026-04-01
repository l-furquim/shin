package com.shin.metadata.service;

import java.util.Map;

public interface S3MetadataService {

    Map<String, String> getObjectMetadata(String bucketName, String objectKey);

}
