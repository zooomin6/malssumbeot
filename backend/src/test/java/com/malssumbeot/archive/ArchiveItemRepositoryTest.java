package com.malssumbeot.archive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ArchiveItemRepositoryTest {

    @Autowired
    private ArchiveItemRepository repository;

    @Test
    void 본인_보관함만_최신순으로_조회한다() {
        repository.save(new ArchiveItem(1L, "질문1", "먼저 저장한 답변", null));
        repository.save(new ArchiveItem(1L, "질문2", "나중에 저장한 답변", null));
        repository.save(new ArchiveItem(2L, "질문3", "다른 사람 답변", null));

        var found = repository.findByUserIdOrderByCreatedAtDesc(1L);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getBarnabasReply()).isEqualTo("나중에 저장한 답변");
        assertThat(found.get(1).getBarnabasReply()).isEqualTo("먼저 저장한 답변");
    }
}
