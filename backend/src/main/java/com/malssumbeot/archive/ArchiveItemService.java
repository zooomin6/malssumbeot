package com.malssumbeot.archive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * verses는 DB 컬럼(verses_snapshot, TEXT)에 JSON 문자열로 저장된다 — 별도 테이블 없이 스냅샷만
 * 남기는 방식이 보관함 개념(그때 본 그대로)에 맞고 코드도 단순하다.
 */
@Service
@Transactional(readOnly = true)
public class ArchiveItemService {

    private final ArchiveItemRepository repository;
    private final ObjectMapper objectMapper;

    public ArchiveItemService(ArchiveItemRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ArchiveItemResponse create(Long userId, ArchiveItemRequest request) {
        ArchiveItem saved = repository.save(new ArchiveItem(
                userId, request.userMessage(), request.barnabasReply(), toJson(request.verses())));
        return toResponse(saved);
    }

    public List<ArchiveItemResponse> findMine(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        ArchiveItem item = repository.findById(id)
                .orElseThrow(() -> new ArchiveItemNotFoundException("보관함 항목을 찾을 수 없습니다: " + id));
        if (!item.getUserId().equals(userId)) {
            throw new ArchiveItemNotFoundException("보관함 항목을 찾을 수 없습니다: " + id);
        }
        repository.delete(item);
    }

    private String toJson(List<VerseSnapshot> verses) {
        if (verses.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(verses);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("구절 스냅샷 직렬화 실패", e);
        }
    }

    private List<VerseSnapshot> fromJson(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<VerseSnapshot>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("구절 스냅샷 역직렬화 실패", e);
        }
    }

    private ArchiveItemResponse toResponse(ArchiveItem item) {
        return new ArchiveItemResponse(item.getId(), item.getUserMessage(), item.getBarnabasReply(),
                fromJson(item.getVersesSnapshot()), item.getCreatedAt());
    }
}
