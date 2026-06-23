package com.fox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Resumo do processo para listagem")
public class ProcessSummaryResponse {

    private UUID id;
    private String processNumber;
    private String clientName;
    private String insuranceCompany;
    private String dealershipName;
    private String currentStageName;
    private String currentReasonName;
    private String status;
    private BigDecimal estimatedValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
