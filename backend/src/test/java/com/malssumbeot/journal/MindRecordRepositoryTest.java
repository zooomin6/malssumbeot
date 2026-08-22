package com.malssumbeot.journal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class MindRecordRepositoryTest {

    @Autowired
    private MindRecordRepository repository;

    @Test
    void 본인_기록만_최신순으로_조회한다() {
        repository.save(new MindRecord(1L, null, "먼저 쓴 기록"));
        repository.save(new MindRecord(1L, "감사한 마음", "나중에 쓴 기록"));
        repository.save(new MindRecord(2L, null, "다른 사람 기록"));

        var found = repository.findByUserIdOrderByCreatedAtDesc(1L);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getContent()).isEqualTo("나중에 쓴 기록");
        assertThat(found.get(1).getContent()).isEqualTo("먼저 쓴 기록");
    }
}
