package com.fox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Historico do processo")
public class ProcessHistoryResponse {

    private UUID id;
    private String type;
    private String message;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
