package com.malssumbeot.journal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrayerRequestRepository extends JpaRepository<PrayerRequest, Long> {

    List<PrayerRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
