package com.malssumbeot.journal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PrayerRequestRepositoryTest {

    @Autowired
    private PrayerRequestRepository repository;

    @Test
    void 본인_기도제목만_최신순으로_조회한다() {
        repository.save(new PrayerRequest(1L, "먼저 쓴 기도"));
        repository.save(new PrayerRequest(1L, "나중에 쓴 기도"));
        repository.save(new PrayerRequest(2L, "다른 사람 기도"));

        var found = repository.findByUserIdOrderByCreatedAtDesc(1L);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getContent()).isEqualTo("나중에 쓴 기도");
        assertThat(found.get(1).getContent()).isEqualTo("먼저 쓴 기도");
    }
}
