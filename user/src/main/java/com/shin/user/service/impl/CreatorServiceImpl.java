package com.shin.user.service.impl;

import com.shin.user.dto.*;
import com.shin.user.exceptions.*;
import com.shin.user.model.Creator;
import com.shin.user.model.User;
import com.shin.user.repository.CreatorRepository;
import com.shin.user.repository.UserRepository;
import com.shin.user.service.CreatorService;
import com.shin.user.service.SecurityService;
import com.shin.user.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CreatorServiceImpl implements CreatorService {

    private final CreatorRepository creatorRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final SecurityService securityService;

    @Override
    public CreateCreatorResponse createCreatorWithUser(
            MultipartFile avatar,
            MultipartFile banner,
            CreateCreatorRequest request,
            String locale
    ) {
        if(locale == null || locale.isBlank()){
            log.info("Locale received empty during creator creation");
            throw new InvalidLocaleException("Locale cannot be null or empty.");
        }

        if(userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicatedEmailException("A user already exists with this email.");
        }

        var password = securityService.encryptPassword(request.password());
        User newUser = User.builder()
                .displayName(request.displayName())
                .email(request.email())
                .password(password)
                .locale(locale)
                .showAdultContent(request.showAdultContent() != null ? request.showAdultContent() : false)
                .build();

        var userSaved = userRepository.save(newUser);
        log.info("New user created with id: {}", userSaved.getId());

        final var idFirstDigits = userSaved.getId().toString().substring(0, 4);

        String username = request.username() != null && !request.username().isBlank()
                ? request.username().replace(" ", "_").toLowerCase() + "@" + idFirstDigits
                : userSaved.getDisplayName() + "@" + idFirstDigits;

        Creator creator = Creator.builder()
                .id(userSaved.getId())
                .username(username)
                .description(request.description())
                .channelUrl(userSaved.getId().toString())
                .build();

        var creatorSaved = creatorRepository.save(creator);
        log.info("New creator profile created with id: {}", creatorSaved.getId());

        if(avatar != null && !avatar.isEmpty()){
            storageService.uploadAvatar(avatar, userSaved.getId());
        }

        if(banner != null && !banner.isEmpty()){
            storageService.uploadBanner(banner, userSaved.getId());
        }

        String[] pictures = storageService.getAvatarAndBannerUrls(userSaved.getId());

        return new CreateCreatorResponse(
                userSaved.getId(),
                userSaved.getDisplayName(),
                userSaved.getEmail(),
                creatorSaved.getUsername(),
                creatorSaved.getChannelUrl(),
                pictures[0],
                pictures[1],
                userSaved.getLanguageTag(),
                userSaved.getShowAdultContent(),
                creatorSaved.getCreatedAt()
        );
    }

    @Override
    public UpdateCreatorResponse updateCreator(
            UUID id,
            UUID requesterId,
            UpdateCreatorRequest request,
            MultipartFile avatar,
            MultipartFile banner
    ) {
        if(!id.equals(requesterId)) {
            log.info("User with id {} attempted to update creator with id {}", requesterId, id);
            throw new UnauthorizedOperationException("Unauthorized operation");
        }

        var creator = findCreatorByIdOrThrow(id);
        var user = findUserByIdOrThrow(id);

        if(request != null) {
            if(request.username() != null && !request.username().isBlank()) {
                String newUsername = request.username().trim() + "@" + id.toString().substring(0, 4);
                creator.setUsername(newUsername);
            }

            if(request.description() != null) {
                creator.setDescription(request.description().trim());
            }

            if(request.displayName() != null && !request.displayName().isBlank()) {
                user.setDisplayName(request.displayName().trim());
            }

            if(request.email() != null && !request.email().isBlank()) {
                boolean emailAlreadyInUse = userRepository.findByEmail(request.email())
                        .map(existingUser -> !existingUser.getId().equals(id))
                        .orElse(false);

                if(emailAlreadyInUse) {
                    throw new DuplicatedEmailException("A user already exists with this email.");
                }

                user.setEmail(request.email().toLowerCase().trim());
            }

            if(request.showAdultContent() != null) {
                user.setShowAdultContent(request.showAdultContent());
            }
        }

        if(avatar != null && !avatar.isEmpty()) {
            storageService.uploadAvatar(avatar, id);
        }

        if(banner != null && !banner.isEmpty()) {
            storageService.uploadBanner(banner, id);
        }

        var updatedCreator = creatorRepository.save(creator);
        var updatedUser = userRepository.save(user);

        String[] pictures = storageService.getAvatarAndBannerUrls(id);

        log.info("Creator and user updated: {}", id);

        return new UpdateCreatorResponse(
                updatedUser.getId(),
                updatedUser.getDisplayName(),
                updatedCreator.getUsername(),
                updatedCreator.getDescription(),
                updatedCreator.getChannelUrl(),
                pictures[0],
                pictures[1],
                updatedUser.getLanguageTag(),
                updatedUser.getUpdatedAt(),
                updatedCreator.getCreatedAt()
        );
    }

    @Override
    public void deleteCreator(UUID id, UUID requesterId) {
        if(!id.equals(requesterId)) {
            log.info("User with id {} attempted to delete creator with id {}", requesterId, id);
            throw new UnauthorizedOperationException("Unauthorized operation");
        }

        findCreatorByIdOrThrow(id);
        findUserByIdOrThrow(id);

        creatorRepository.deleteById(id);
        userRepository.deleteById(id);
        storageService.deleteCreatorPictures(id);

        log.info("Creator and user deleted: {}", id);
    }

    @Override
    public GetCreatorByIdResponse getCreatorById(UUID id) {
        var creator = findCreatorByIdOrThrow(id);
        var user = findUserByIdOrThrow(id);
        String[] pictures = storageService.getAvatarAndBannerUrls(id);

        return new GetCreatorByIdResponse(
                creator.getId(),
                user.getDisplayName(),
                creator.getUsername(),
                creator.getDescription(),
                creator.getChannelUrl(),
                pictures[0],
                pictures[1],
                user.getLanguageTag(),
                creator.getCreatedAt()
        );
    }

    private Creator findCreatorByIdOrThrow(UUID id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new InvalidIdException("Creator not found with id: " + id));
    }

    private User findUserByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

}
