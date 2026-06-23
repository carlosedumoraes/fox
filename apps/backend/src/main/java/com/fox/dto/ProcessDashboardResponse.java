package com.fox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Indicadores consolidados de processos")
public class ProcessDashboardResponse {

    private long total;
    private long inProgress;
    private long pending;
    private BigDecimal estimatedValue;

    public ProcessDashboardResponse(long total, long inProgress, long pending, BigDecimal estimatedValue) {
        this.total = total;
        this.inProgress = inProgress;
        this.pending = pending;
        this.estimatedValue = estimatedValue;
    }
}
