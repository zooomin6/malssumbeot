package com.malssumbeot.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArchiveItemServiceTest {

    private final ArchiveItemRepository repository = mock(ArchiveItemRepository.class);
    private final ArchiveItemService service = new ArchiveItemService(repository, new ObjectMapper());

    private ArchiveItem itemOwnedBy(long userId, long id) {
        ArchiveItem item = new ArchiveItem(userId, "질문", "답변", null);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    @Test
    void 구절_스냅샷을_JSON으로_저장하고_그대로_복원한다() {
        when(repository.save(any(ArchiveItem.class))).thenAnswer(inv -> {
            ArchiveItem saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        var verses = List.of(new VerseSnapshot("요 3:16", "하나님이 세상을 이처럼 사랑하사..."));

        ArchiveItemResponse response = service.create(1L, new ArchiveItemRequest("질문", "답변", verses));

        assertThat(response.verses()).containsExactly(new VerseSnapshot("요 3:16", "하나님이 세상을 이처럼 사랑하사..."));
    }

    @Test
    void 구절이_없으면_빈_리스트로_복원한다() {
        when(repository.save(any(ArchiveItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ArchiveItemResponse response = service.create(1L, new ArchiveItemRequest(null, "답변", List.of()));

        assertThat(response.verses()).isEmpty();
    }

    @Test
    void 본인_소유면_삭제된다() {
        ArchiveItem item = itemOwnedBy(1L, 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(item));

        service.delete(1L, 10L);

        verify(repository).delete(item);
    }

    @Test
    void 타인_소유면_찾을수없음_예외() {
        ArchiveItem item = itemOwnedBy(2L, 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.delete(1L, 10L))
                .isInstanceOf(ArchiveItemNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
