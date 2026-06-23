package com.fox.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados administrativos do usuario")
public class UserAdminResponse {
    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;
    @Schema(example = "Usuario Teste")
    private String name;
    @Schema(example = "usuario@test.com")
    private String email;
    @Schema(example = "true")
    private boolean active;
    @Schema(example = "[\"USER\", \"ADMIN\"]")
    private Set<String> roles;
    @Schema(example = "2026-05-13T11:00:00")
    private LocalDateTime createdAt;

    public UserAdminResponse(UUID id, String name, String email, boolean active, Set<String> roles, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.active = active;
        this.roles = roles;
        this.createdAt = createdAt;
    }
}
