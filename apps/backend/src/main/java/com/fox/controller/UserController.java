package com.fox.controller;

import com.fox.dto.UpdateUserRolesRequest;
import com.fox.dto.UpdateUserRequest;
import com.fox.dto.UserAdminResponse;
import com.fox.dto.UserResponse;
import com.fox.mapper.UserMapper;
import com.fox.security.CustomUserDetails;
import com.fox.service.UserService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints de consulta e administracao de usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Buscar usuario autenticado", description = "Retorna os dados do usuario associado ao JWT enviado no header Authorization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado retornado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado")
    })
    public UserResponse me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return UserMapper.toResponse(userDetails.getUser());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Lista usuarios nao deletados com dados administrativos. Requer role ADMIN.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuarios retornados",
                    content = @Content(schema = @Schema(implementation = UserAdminResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao ADMIN")
    })
    public List<UserAdminResponse> users() {
        return userService.findActiveUsers();
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desabilitar usuario", description = "Marca active=false para o usuario informado. Requer role ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario desabilitado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    public UserResponse disable(@PathVariable UUID id) {
        return userService.disable(id);
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Habilitar usuario", description = "Marca active=true para o usuario informado. Requer role ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario habilitado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    public UserResponse enable(@PathVariable UUID id) {
        return userService.enable(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar usuario", description = "Atualiza nome e email do usuario. Requer role ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario atualizado"),
            @ApiResponse(responseCode = "400", description = "Payload invalido ou email ja cadastrado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    public UserResponse update(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nome e email do usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateUserRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Usuario Teste\",\"email\":\"usuario@test.com\"}")
                    )
            )
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Atualizar roles do usuario",
            description = "Substitui as roles atuais pelas roles informadas. Requer role ADMIN. Roles comuns: USER, ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roles atualizadas",
                    content = @Content(
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = "{\"id\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"name\":\"Usuario Teste\",\"email\":\"usuario@test.com\",\"roles\":[\"USER\",\"ADMIN\"]}")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario ou role nao encontrada")
    })
    public UserResponse updateRoles(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Roles que substituirao as roles atuais do usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateUserRolesRequest.class),
                            examples = @ExampleObject(value = "{\"roles\":[\"USER\",\"ADMIN\"]}")
                    )
            )
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {
        return userService.updateRoles(id, request.getRoles());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover usuario", description = "Executa soft delete preenchendo deleted_at. Requer role ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario removido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
