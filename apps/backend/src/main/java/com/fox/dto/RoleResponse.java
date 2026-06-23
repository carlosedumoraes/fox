package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Role disponivel no sistema")
public class RoleResponse {
    @Schema(example = "ADMIN")
    private String name;
    @Schema(example = "Administrador")
    private String description;

    public RoleResponse(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
