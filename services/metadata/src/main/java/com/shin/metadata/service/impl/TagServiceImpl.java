package com.shin.metadata.service.impl;

import com.shin.metadata.dto.*;
import com.shin.metadata.exception.InvalidVideoRequestException;
import com.shin.metadata.model.Tag;
import com.shin.metadata.repository.TagRepository;
import com.shin.metadata.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public Tag findOrCreateTag(TagIdentifier identifier) {
        if (identifier.id() != null) {
            return tagRepository.findById(identifier.id())
                .orElseGet(() -> {
                    if (identifier.name() != null) {
                        return findOrCreateByName(identifier.normalizedName());
                    }
                    throw new InvalidVideoRequestException("Tag with id " + identifier.id() + " not found");
                });
        }

        if (identifier.name() != null) {
            return findOrCreateByName(identifier.normalizedName());
        }

        throw new InvalidVideoRequestException("Tag identifier must have either id or name");
    }

    @Override
    @Transactional(readOnly = true)
    public Tag findTagOrThrow(TagIdentifier identifier) {
        if (identifier.id() != null) {
            return tagRepository.findById(identifier.id())
                .orElseGet(() -> {
                    if (identifier.name() != null) {
                        return findByNameOrThrow(identifier.normalizedName());
                    }
                    throw new InvalidVideoRequestException("Tag with id " + identifier.id() + " not found");
                });
        }

        if (identifier.name() != null) {
            return findByNameOrThrow(identifier.normalizedName());
        }

        throw new InvalidVideoRequestException("Tag identifier must have either id or name");
    }

    @Override
    @Transactional
    public Set<Tag> findOrCreateTags(Set<TagIdentifier> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return new HashSet<>();
        }

        Set<Tag> tags = new HashSet<>();
        for (TagIdentifier identifier : identifiers) {
            tags.add(findOrCreateTag(identifier));
        }
        return tags;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Tag> findTagsOrThrow(Set<TagIdentifier> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return new HashSet<>();
        }

        Set<Tag> tags = new HashSet<>();
        for (TagIdentifier identifier : identifiers) {
            tags.add(findTagOrThrow(identifier));
        }
        return tags;
    }

    private Tag findOrCreateByName(String normalizedName) {
        return tagRepository.findByNameIgnoreCase(normalizedName)
            .orElseGet(() -> {
                Tag newTag = Tag.builder()
                    .name(normalizedName)
                    .videos(new HashSet<>())
                    .build();
                Tag saved = tagRepository.save(newTag);
                log.info("Created new tag: {}", normalizedName);
                return saved;
            });
    }

    @Override
    @Transactional(readOnly = true)
    public ListTagsResponse searchTags(String query) {
        List<Tag> tags = query != null && !query.isBlank()
                ? tagRepository.findByNameContainingIgnoreCase(query)
                : tagRepository.findAll();
        List<TagDto> items = tags.stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .toList();
        return new ListTagsResponse(items);
    }

    @Override
    @Transactional
    public TagDto createTag(CreateTagRequest request) {
        String normalizedName = request.name().trim().toLowerCase();
        if (tagRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new InvalidVideoRequestException("Tag with name '" + normalizedName + "' already exists");
        }
        Tag tag = Tag.builder().name(normalizedName).build();
        tag = tagRepository.save(tag);
        return new TagDto(tag.getId(), tag.getName());
    }

    private Tag findByNameOrThrow(String normalizedName) {
        return tagRepository.findByNameIgnoreCase(normalizedName)
            .orElseThrow(() -> new InvalidVideoRequestException("Tag with name '" + normalizedName + "' not found"));
    }
}
