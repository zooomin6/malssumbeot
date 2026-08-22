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

class PrayerRequestServiceTest {

    private final PrayerRequestRepository repository = mock(PrayerRequestRepository.class);
    private final PrayerRequestService service = new PrayerRequestService(repository);

    private PrayerRequest requestOwnedBy(long userId, long id) {
        PrayerRequest request = new PrayerRequest(userId, "가족을 위해 기도해주세요");
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    @Test
    void 생성하면_repository에_저장한다() {
        when(repository.save(any(PrayerRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        PrayerRequest saved = service.create(1L, new PrayerRequestRequest("가족을 위해 기도해주세요"));

        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getContent()).isEqualTo("가족을 위해 기도해주세요");
    }

    @Test
    void 본인_소유면_삭제된다() {
        PrayerRequest request = requestOwnedBy(1L, 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(request));

        service.delete(1L, 10L);

        verify(repository).delete(request);
    }

    @Test
    void 타인_소유면_찾을수없음_예외() {
        PrayerRequest request = requestOwnedBy(2L, 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.delete(1L, 10L))
                .isInstanceOf(PrayerRequestNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void 존재하지_않으면_찾을수없음_예외() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 99L))
                .isInstanceOf(PrayerRequestNotFoundException.class);
    }
}
