package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Roles que devem substituir as roles atuais do usuario")
public class UpdateUserRolesRequest {

    @NotEmpty
    @Schema(example = "[\"USER\", \"ADMIN\"]", description = "Roles existentes no banco. Valores comuns: USER, ADMIN.")
    private List<String> roles;
}
