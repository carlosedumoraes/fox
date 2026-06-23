package com.fox.repository;

import com.fox.entity.ProcessReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessReasonRepository extends JpaRepository<ProcessReason, UUID> {
    Optional<ProcessReason> findByCode(String code);

    Optional<ProcessReason> findByCodeIgnoreCase(String code);

    Optional<ProcessReason> findByNameIgnoreCase(String name);
}
