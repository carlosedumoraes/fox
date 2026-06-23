package com.fox.controller;

import com.fox.dto.AuthLoginRequest;
import com.fox.dto.AuthRegisterRequest;
import com.fox.dto.AuthResponse;
import com.fox.dto.ForgotPasswordRequest;
import com.fox.dto.RefreshTokenRequest;
import com.fox.dto.ResetPasswordRequest;
import com.fox.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticacao, refresh token e recuperacao de senha")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Cria um usuario ativo, associa a role USER e retorna tokens JWT.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario registrado com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"refreshToken\":\"6f5d71f0-2c6a-465a-9a50-b3a8991a8b14\",\"user\":{\"id\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"name\":\"Usuario Teste\",\"email\":\"usuario@test.com\",\"roles\":[\"USER\"]}}")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Payload invalido ou email ja cadastrado"),
            @ApiResponse(responseCode = "404", description = "Role USER nao encontrada")
    })
    public AuthResponse register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados para criar usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AuthRegisterRequest.class),
                            examples = @ExampleObject(value = "{\"name\":\"Usuario Teste\",\"email\":\"usuario@test.com\",\"password\":\"12345678\"}")
                    )
            )
            @Valid @RequestBody AuthRegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login do usuario", description = "Autentica email e senha, registra historico de login e retorna access token e refresh token.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login realizado com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"refreshToken\":\"6f5d71f0-2c6a-465a-9a50-b3a8991a8b14\",\"user\":{\"id\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"name\":\"Usuario Teste\",\"email\":\"usuario@test.com\",\"roles\":[\"USER\"]}}")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
    })
    public AuthResponse login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Credenciais do usuario",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AuthLoginRequest.class),
                            examples = @ExampleObject(value = "{\"email\":\"usuario@test.com\",\"password\":\"12345678\"}")
                    )
            )
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Valida o refresh token, revoga o token antigo e retorna um novo par de tokens.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token renovado com sucesso",
                    content = @Content(
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9...\",\"refreshToken\":\"80ed7825-fb09-4978-8a35-54e998819cc2\",\"user\":{\"id\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"name\":\"Usuario Teste\",\"email\":\"usuario@test.com\",\"roles\":[\"USER\"]}}")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido ou expirado")
    })
    public AuthResponse refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh token atual",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(value = "{\"refreshToken\":\"6f5d71f0-2c6a-465a-9a50-b3a8991a8b14\"}")
                    )
            )
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga o refresh token informado.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout realizado com sucesso",
                    content = @Content(examples = @ExampleObject(value = "{\"message\":\"Logged out successfully\"}"))
            ),
            @ApiResponse(responseCode = "400", description = "Payload invalido")
    })
    public Map<String, String> logout(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh token que sera revogado",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(value = "{\"refreshToken\":\"6f5d71f0-2c6a-465a-9a50-b3a8991a8b14\"}")
                    )
            )
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return authService.logout(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar reset de senha", description = "Gera um token de reset quando o email existir, sem revelar se o email esta cadastrado.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resposta generica retornada",
                    content = @Content(examples = @ExampleObject(value = "{\"message\":\"If the email exists, a password reset token was generated\"}"))
            ),
            @ApiResponse(responseCode = "400", description = "Payload invalido")
    })
    public Map<String, String> forgotPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Email para recuperacao de senha",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ForgotPasswordRequest.class),
                            examples = @ExampleObject(value = "{\"email\":\"usuario@test.com\"}")
                    )
            )
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha", description = "Valida o token de reset, troca a senha e revoga refresh tokens ativos.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Senha atualizada com sucesso",
                    content = @Content(examples = @ExampleObject(value = "{\"message\":\"Password updated successfully\"}"))
            ),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "Token invalido ou expirado")
    })
    public Map<String, String> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Token de reset e nova senha",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ResetPasswordRequest.class),
                            examples = @ExampleObject(value = "{\"token\":\"a798a970-0365-4472-8b03-76c1ddbb38f9\",\"newPassword\":\"12345678\"}")
                    )
            )
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return authService.resetPassword(request);
    }
}
