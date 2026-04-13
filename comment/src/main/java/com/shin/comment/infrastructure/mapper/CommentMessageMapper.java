package com.shin.comment.infrastructure.mapper;

import com.shin.comment.domain.model.Comment;
import com.shin.comment.infrastructure.dto.CommentDeletedEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMessageMapper {

    CommentDeletedEvent toCommentDeletedEvent(Comment comment);

}
