package com.malssumbeot.archive;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveItemRepository extends JpaRepository<ArchiveItem, Long> {

    List<ArchiveItem> findByUserIdOrderByCreatedAtDesc(Long userId);
}
