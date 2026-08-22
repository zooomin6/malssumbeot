package com.malssumbeot.journal;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrayerRequestService {

    private final PrayerRequestRepository repository;

    public PrayerRequestService(PrayerRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PrayerRequest create(Long userId, PrayerRequestRequest request) {
        return repository.save(new PrayerRequest(userId, request.content()));
    }

    public List<PrayerRequest> findMine(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        PrayerRequest request = repository.findById(id)
                .orElseThrow(() -> new PrayerRequestNotFoundException("기도 제목을 찾을 수 없습니다: " + id));
        if (!request.getUserId().equals(userId)) {
            throw new PrayerRequestNotFoundException("기도 제목을 찾을 수 없습니다: " + id);
        }
        repository.delete(request);
    }
}
