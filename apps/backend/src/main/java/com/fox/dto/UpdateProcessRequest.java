package com.fox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados editaveis do processo")
public class UpdateProcessRequest {

    @Schema(example = "Montador")
    private String operationType;

    @Schema(example = "AGCO")
    private String clientName;

    @Schema(example = "SURA")
    private String insuranceCompany;

    @Schema(example = "AGCO Center Campinas")
    private String dealershipName;

    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID currentStageId;

    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID currentReasonId;

    @Schema(example = "Avaria de transporte")
    private String currentReasonName;

    @Schema(example = "SURA-721945")
    private String insuranceClaimNumber;

    @Schema(example = "9BWZZZ377VT004251")
    private String chassis;

    @Schema(example = "NF-889412")
    private String invoiceNumber;

    @Schema(example = "OSS-54021")
    private String ossNumber;

    @Schema(example = "Solistica")
    private String carrierName;

    @Schema(example = "2026-05-02")
    private LocalDate occurrenceDate;

    @Schema(example = "2026-05-03")
    private LocalDate claimDate;

    @Schema(example = "Campinas")
    private String city;

    @Schema(example = "SP")
    private String state;

    @Schema(example = "18400.00")
    private BigDecimal estimatedValue;

    @Schema(example = "Unidade recebida com avaria no para-choque dianteiro.")
    private String description;

    @Schema(example = "IN_PROGRESS")
    private String status;
}
