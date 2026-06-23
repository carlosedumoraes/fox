package com.fox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados completos do processo")
public class ProcessResponse {

    private UUID id;
    private String processNumber;
    private String operationType;
    private String clientName;
    private String insuranceCompany;
    private String dealershipName;
    private UUID currentStageId;
    private String currentStageName;
    private UUID currentReasonId;
    private String currentReasonName;
    private String insuranceClaimNumber;
    private String chassis;
    private String invoiceNumber;
    private String ossNumber;
    private String carrierName;
    private LocalDate occurrenceDate;
    private LocalDate claimDate;
    private String city;
    private String state;
    private BigDecimal estimatedValue;
    private String description;
    private String status;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
