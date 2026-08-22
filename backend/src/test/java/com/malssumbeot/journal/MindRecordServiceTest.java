package com.malssumbeot.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MindRecordServiceTest {

    private final MindRecordRepository repository = mock(MindRecordRepository.class);
    private final MindRecordService service = new MindRecordService(repository);

    private MindRecord recordOwnedBy(long userId, long id) {
        MindRecord record = new MindRecord(userId, "감사한 마음", "오늘도 감사해요");
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    @Test
    void 생성하면_repository에_저장한다() {
        when(repository.save(any(MindRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MindRecord saved = service.create(1L, new MindRecordRequest("감사한 마음", "감사한 마음"));

        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getMood()).isEqualTo("감사한 마음");
    }

    @Test
    void 본인_소유면_삭제된다() {
        MindRecord record = recordOwnedBy(1L, 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(record));

        service.delete(1L, 10L);

        verify(repository).delete(record);
    }

    @Test
    void 타인_소유면_찾을수없음_예외() {
        MindRecord record = recordOwnedBy(2L, 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.delete(1L, 10L))
                .isInstanceOf(MindRecordNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void 존재하지_않으면_찾을수없음_예외() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 99L))
                .isInstanceOf(MindRecordNotFoundException.class);
    }
}
