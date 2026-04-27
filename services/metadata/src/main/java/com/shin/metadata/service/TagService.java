package com.shin.metadata.service;

import com.shin.metadata.dto.*;
import com.shin.metadata.model.Tag;

import java.util.Set;

public interface TagService {
    Tag findOrCreateTag(TagIdentifier identifier);
    Tag findTagOrThrow(TagIdentifier identifier);
    Set<Tag> findOrCreateTags(Set<TagIdentifier> identifiers);
    Set<Tag> findTagsOrThrow(Set<TagIdentifier> identifiers);
    ListTagsResponse searchTags(String query);
    TagDto createTag(CreateTagRequest request);
}
