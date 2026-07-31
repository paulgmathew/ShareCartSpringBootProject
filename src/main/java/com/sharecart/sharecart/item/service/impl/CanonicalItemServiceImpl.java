package com.sharecart.sharecart.item.service.impl;

import com.sharecart.sharecart.common.exception.ResourceNotFoundException;
import com.sharecart.sharecart.item.dto.CanonicalItemResponse;
import com.sharecart.sharecart.item.dto.CreateCanonicalItemRequest;
import com.sharecart.sharecart.item.model.CanonicalItem;
import com.sharecart.sharecart.item.repository.CanonicalItemRepository;
import com.sharecart.sharecart.item.service.CanonicalItemService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CanonicalItemServiceImpl implements CanonicalItemService {

    private final CanonicalItemRepository canonicalItemRepository;

    @Override
    public CanonicalItemResponse createCanonicalItem(CreateCanonicalItemRequest request) {
        String normalizedName = normalizeName(request.name());
        CanonicalItem entity = CanonicalItem.builder()
                .name(request.name().trim())
                .normalizedName(normalizedName)
                .description(request.description())
                .build();
        CanonicalItem saved = canonicalItemRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanonicalItemResponse> listCanonicalItems() {
        return canonicalItemRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CanonicalItemResponse getCanonicalItem(UUID id) {
        return canonicalItemRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Canonical item not found with id: " + id));
    }

    private CanonicalItemResponse toResponse(CanonicalItem entity) {
        return new CanonicalItemResponse(entity.getId(), entity.getName(), entity.getNormalizedName(), entity.getDescription());
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }
}
