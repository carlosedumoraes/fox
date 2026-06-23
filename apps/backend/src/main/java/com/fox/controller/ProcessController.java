package com.fox.controller;

import com.fox.dto.CreateProcessRequest;
import com.fox.dto.ProcessDashboardResponse;
import com.fox.dto.ProcessHistoryResponse;
import com.fox.dto.ProcessResponse;
import com.fox.dto.ProcessSummaryResponse;
import com.fox.dto.UpdateProcessRequest;
import com.fox.security.CustomUserDetails;
import com.fox.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/processes")
@Tag(name = "Processes", description = "Endpoints do modulo de processos")
@SecurityRequirement(name = "bearerAuth")
public class ProcessController {

    private final ProcessService processService;

    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'ANALISTA', 'SUPERVISOR')")
    @Operation(summary = "Criar processo", description = "Cria um processo real e gera automaticamente o numero DC-YYYY-XXXXXX.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Processo criado", content = @Content(schema = @Schema(implementation = ProcessResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao para criar processo"),
            @ApiResponse(responseCode = "404", description = "Etapa ou motivo nao encontrado")
    })
    public ProcessResponse create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados de abertura do processo. O numero do processo e gerado pelo backend.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateProcessRequest.class),
                            examples = @ExampleObject(value = "{\"operationType\":\"Montador\",\"clientName\":\"AGCO\",\"insuranceCompany\":\"SURA\",\"dealershipName\":\"AGCO Center Campinas\",\"estimatedValue\":18400.00,\"status\":\"OPEN\"}")
                    )
            )
            @Valid @RequestBody CreateProcessRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return processService.createProcess(request, userDetails.getUser());
    }

    @GetMapping
    @Operation(summary = "Listar processos", description = "Lista processos paginados com filtros opcionais por numero, status e etapa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Processos retornados"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado")
    })
    public Page<ProcessSummaryResponse> list(
            @RequestParam(required = false) String processNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID currentStage,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return processService.getProcesses(processNumber, status, currentStage, pageable);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard de processos", description = "Retorna indicadores consolidados de processos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard retornado", content = @Content(schema = @Schema(implementation = ProcessDashboardResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado")
    })
    public ProcessDashboardResponse dashboard() {
        return processService.getDashboard();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar processo por ID", description = "Retorna os dados completos do processo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Processo retornado", content = @Content(schema = @Schema(implementation = ProcessResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "404", description = "Processo nao encontrado")
    })
    public ProcessResponse getById(@PathVariable UUID id) {
        return processService.getProcessById(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'ANALISTA', 'SUPERVISOR')")
    @Operation(summary = "Atualizar processo", description = "Atualiza campos basicos do processo e registra historico automatico para etapa e status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Processo atualizado", content = @Content(schema = @Schema(implementation = ProcessResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuario sem permissao para editar processo"),
            @ApiResponse(responseCode = "404", description = "Processo, etapa ou motivo nao encontrado")
    })
    public ProcessResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProcessRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return processService.updateProcess(id, request, userDetails.getUser());
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historico do processo", description = "Lista eventos de historico registrados para o processo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historico retornado"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, invalido ou expirado"),
            @ApiResponse(responseCode = "404", description = "Processo nao encontrado")
    })
    public List<ProcessHistoryResponse> history(@PathVariable UUID id) {
        return processService.getProcessHistory(id);
    }
}
