package com.fox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Motivo disponivel para processos")
public class ProcessReasonResponse {

    private UUID id;
    private String code;
    private String name;

    public ProcessReasonResponse(UUID id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }
}
