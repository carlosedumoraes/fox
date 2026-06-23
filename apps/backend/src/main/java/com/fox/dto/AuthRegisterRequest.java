package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados para registro de usuario")
public class AuthRegisterRequest {

    @NotBlank
    @Schema(example = "Usuario Teste")
    private String name;

    @Email
    @NotBlank
    @Schema(example = "usuario@test.com")
    private String email;

    @NotBlank
    @Size(min = 8)
    @Schema(example = "12345678")
    private String password;
}
