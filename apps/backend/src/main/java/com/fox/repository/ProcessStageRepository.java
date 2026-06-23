package com.fox.repository;

import com.fox.entity.ProcessStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessStageRepository extends JpaRepository<ProcessStage, UUID> {
    Optional<ProcessStage> findByCode(String code);
}
