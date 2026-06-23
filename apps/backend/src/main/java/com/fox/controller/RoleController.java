package com.fox.controller;

import com.fox.dto.RoleResponse;
import com.fox.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Consulta de roles disponiveis no sistema")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "Listar roles", description = "Retorna as roles cadastradas no banco para uso na administracao de usuarios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles retornadas"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado")
    })
    public List<RoleResponse> roles() {
        return roleService.findAll();
    }
}
