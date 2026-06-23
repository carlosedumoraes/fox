package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Resposta de autenticacao com tokens e dados do usuario")
public class AuthResponse {
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;
    @Schema(example = "6f5d71f0-2c6a-465a-9a50-b3a8991a8b14")
    private String refreshToken;

    private UserResponse user;

    public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }
}
