package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Refresh token para renovacao ou logout")
public class RefreshTokenRequest {

    @NotBlank
    @Schema(example = "6f5d71f0-2c6a-465a-9a50-b3a8991a8b14")
    private String refreshToken;
}
