package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados para redefinicao de senha")
public class ResetPasswordRequest {

    @NotBlank
    @Schema(example = "a798a970-0365-4472-8b03-76c1ddbb38f9")
    private String token;

    @NotBlank
    @Size(min = 8)
    @Schema(example = "12345678")
    private String newPassword;
}
