package com.fox.repository;

import com.fox.entity.Process;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProcessRepository extends JpaRepository<Process, UUID>, JpaSpecificationExecutor<Process> {

    boolean existsByProcessNumber(String processNumber);

    long countByStatusIgnoreCase(String status);

    @Query(
            value = "SELECT TOP 1 process_number FROM [process] WHERE process_number LIKE :prefix ORDER BY process_number DESC",
            nativeQuery = true
    )
    Optional<String> findLastProcessNumberByPrefix(@Param("prefix") String prefix);

    @Query(value = "SELECT COALESCE(SUM(estimated_value), 0) FROM [process]", nativeQuery = true)
    BigDecimal sumEstimatedValue();
}
