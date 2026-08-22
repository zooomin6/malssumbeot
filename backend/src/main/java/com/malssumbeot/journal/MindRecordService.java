package com.malssumbeot.journal;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MindRecordService {

    private final MindRecordRepository repository;

    public MindRecordService(MindRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MindRecord create(Long userId, MindRecordRequest request) {
        return repository.save(new MindRecord(userId, request.mood(), request.content()));
    }

    public List<MindRecord> findMine(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        MindRecord record = repository.findById(id)
                .orElseThrow(() -> new MindRecordNotFoundException("마음 기록을 찾을 수 없습니다: " + id));
        if (!record.getUserId().equals(userId)) {
            throw new MindRecordNotFoundException("마음 기록을 찾을 수 없습니다: " + id);
        }
        repository.delete(record);
    }
}
