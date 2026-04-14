package com.shin.user.service;

import com.shin.user.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CreatorService {
         CreateCreatorResponse createCreatorWithUser(
                 MultipartFile avatar,
                 MultipartFile banner,
                 CreateCreatorRequest request,
                 String locale
         );

         UpdateCreatorResponse updateCreator(
                 UUID id,
                 UpdateCreatorRequest request,
                 MultipartFile avatar,
                 MultipartFile banner
         );

         void deleteCreator(
                 UUID requesterId
         );

         GetCreatorByIdResponse getCreatorById(
                 UUID creatorId,
                 UUID userId
         );

        GetMeResponse getMe(
                 UUID userId
         );

}
