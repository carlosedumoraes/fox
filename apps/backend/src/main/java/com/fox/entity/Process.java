package com.fox.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "process")
public class Process {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "process_number", nullable = false, unique = true, length = 30)
    private String processNumber;

    @Column(name = "operation_type", length = 100)
    private String operationType;

    @Column(name = "client_name", length = 150)
    private String clientName;

    @Column(name = "insurance_company", length = 150)
    private String insuranceCompany;

    @Column(name = "dealership_name", length = 150)
    private String dealershipName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_stage_id")
    private ProcessStage currentStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_reason_id")
    private ProcessReason currentReason;

    @Column(name = "insurance_claim_number", length = 100)
    private String insuranceClaimNumber;

    @Column(name = "chassis", length = 100)
    private String chassis;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "oss_number", length = 100)
    private String ossNumber;

    @Column(name = "carrier_name", length = 150)
    private String carrierName;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "claim_date")
    private LocalDate claimDate;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "estimated_value", precision = 18, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "description", columnDefinition = "VARCHAR(MAX)")
    private String description;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = "OPEN";
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
