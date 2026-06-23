package com.fox.repository;

import com.fox.entity.ProcessHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcessHistoryRepository extends JpaRepository<ProcessHistory, UUID> {
    List<ProcessHistory> findByProcessIdOrderByCreatedAtAsc(UUID processId);
}
