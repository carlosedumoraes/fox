package com.fox.controller;

import com.fox.dto.ProcessReasonResponse;
import com.fox.repository.ProcessReasonRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/process-reasons")
@Tag(name = "Process Reasons", description = "Motivos disponiveis para processos")
@SecurityRequirement(name = "bearerAuth")
public class ProcessReasonController {

    private final ProcessReasonRepository processReasonRepository;

    public ProcessReasonController(ProcessReasonRepository processReasonRepository) {
        this.processReasonRepository = processReasonRepository;
    }

    @GetMapping
    @Operation(summary = "Listar motivos de processo", description = "Retorna os motivos cadastrados em process_reason para uso nos formularios.")
    public List<ProcessReasonResponse> list() {
        return processReasonRepository.findAll().stream()
                .map(reason -> new ProcessReasonResponse(reason.getId(), reason.getCode(), reason.getName()))
                .toList();
    }
}
