package com.malssumbeot.journal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MindRecordRepository extends JpaRepository<MindRecord, Long> {

    List<MindRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
