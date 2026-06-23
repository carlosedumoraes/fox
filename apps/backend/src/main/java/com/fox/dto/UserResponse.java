package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados publicos do usuario")
public class UserResponse {
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;
    @Schema(example = "Usuario Teste")
    private String name;
    @Schema(example = "usuario@test.com")
    private String email;
    @Schema(example = "[\"USER\"]")
    private Set<String> roles;

    public UserResponse(UUID id, String name, String email, Set<String> roles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles;
    }
}
