package com.shin.comment.infrastructure.mapper;

import com.shin.comment.domain.model.Comment;
import com.shin.comment.infrastructure.entity.CommentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentEntityMapper {

    CommentEntity toEntity(Comment comment);

    Comment toDomain(CommentEntity entity);
}
