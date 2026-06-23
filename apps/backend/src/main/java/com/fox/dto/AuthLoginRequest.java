package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Credenciais para login")
public class AuthLoginRequest {

    @Email
    @NotBlank
    @Schema(example = "usuario@test.com")
    private String email;

    @NotBlank
    @Schema(example = "12345678")
    private String password;
}
