package com.shin.user.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {

    void uploadAvatar(MultipartFile file, UUID id);
    void uploadBanner(MultipartFile file, UUID id);

    String[] getAvatarAndBannerUrls(UUID id);

    void deleteAvatar(UUID id);
    void deleteBanner(UUID  id);

    void deleteCreatorPictures(UUID id);

}
