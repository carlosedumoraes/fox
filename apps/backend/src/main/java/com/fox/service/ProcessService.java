package com.fox.service;

import com.fox.dto.CreateProcessRequest;
import com.fox.dto.ProcessDashboardResponse;
import com.fox.dto.ProcessHistoryResponse;
import com.fox.dto.ProcessResponse;
import com.fox.dto.ProcessSummaryResponse;
import com.fox.dto.UpdateProcessRequest;
import com.fox.entity.Process;
import com.fox.entity.ProcessHistory;
import com.fox.entity.ProcessReason;
import com.fox.entity.ProcessStage;
import com.fox.entity.User;
import com.fox.exception.ResourceNotFoundException;
import com.fox.repository.ProcessHistoryRepository;
import com.fox.repository.ProcessReasonRepository;
import com.fox.repository.ProcessRepository;
import com.fox.repository.ProcessStageRepository;
import com.fox.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProcessService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_PENDING = "PENDING";

    private final ProcessRepository processRepository;
    private final ProcessStageRepository processStageRepository;
    private final ProcessReasonRepository processReasonRepository;
    private final ProcessHistoryRepository processHistoryRepository;
    private final UserRepository userRepository;

    public ProcessService(
            ProcessRepository processRepository,
            ProcessStageRepository processStageRepository,
            ProcessReasonRepository processReasonRepository,
            ProcessHistoryRepository processHistoryRepository,
            UserRepository userRepository
    ) {
        this.processRepository = processRepository;
        this.processStageRepository = processStageRepository;
        this.processReasonRepository = processReasonRepository;
        this.processHistoryRepository = processHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProcessResponse createProcess(CreateProcessRequest request, User authenticatedUser) {
        User createdBy = userRepository.getReferenceById(authenticatedUser.getId());
        Process process = new Process();

        process.setProcessNumber(generateProcessNumber());
        process.setOperationType(normalize(request.getOperationType()));
        process.setClientName(normalize(request.getClientName()));
        process.setInsuranceCompany(normalize(request.getInsuranceCompany()));
        process.setDealershipName(normalize(request.getDealershipName()));
        process.setCurrentStage(findStage(request.getCurrentStageId()));
        process.setCurrentReason(findReason(request.getCurrentReasonId(), request.getCurrentReasonName()));
        process.setInsuranceClaimNumber(normalize(request.getInsuranceClaimNumber()));
        process.setChassis(normalize(request.getChassis()));
        process.setInvoiceNumber(normalize(request.getInvoiceNumber()));
        process.setOssNumber(normalize(request.getOssNumber()));
        process.setCarrierName(normalize(request.getCarrierName()));
        process.setOccurrenceDate(request.getOccurrenceDate());
        process.setClaimDate(request.getClaimDate());
        process.setCity(normalize(request.getCity()));
        process.setState(normalizeUpper(request.getState()));
        process.setEstimatedValue(request.getEstimatedValue());
        process.setDescription(normalize(request.getDescription()));
        process.setStatus(normalizeUpperOrDefault(request.getStatus(), STATUS_OPEN));
        process.setCreatedBy(createdBy);

        Process savedProcess = processRepository.save(process);
        registerHistory(savedProcess, "CREATED", "Processo criado por " + createdBy.getName(), createdBy);
        return toResponse(savedProcess);
    }

    @Transactional
    public ProcessResponse updateProcess(UUID id, UpdateProcessRequest request, User authenticatedUser) {
        Process process = findProcess(id);
        User updatedBy = userRepository.getReferenceById(authenticatedUser.getId());
        ProcessStage oldStage = process.getCurrentStage();
        String oldStatus = process.getStatus();

        if (request.getOperationType() != null) {
            process.setOperationType(normalize(request.getOperationType()));
        }
        if (request.getClientName() != null) {
            process.setClientName(normalize(request.getClientName()));
        }
        if (request.getInsuranceCompany() != null) {
            process.setInsuranceCompany(normalize(request.getInsuranceCompany()));
        }
        if (request.getDealershipName() != null) {
            process.setDealershipName(normalize(request.getDealershipName()));
        }
        if (request.getCurrentStageId() != null) {
            process.setCurrentStage(findStage(request.getCurrentStageId()));
        }
        if (request.getCurrentReasonId() != null || request.getCurrentReasonName() != null) {
            process.setCurrentReason(findReason(request.getCurrentReasonId(), request.getCurrentReasonName()));
        }
        if (request.getInsuranceClaimNumber() != null) {
            process.setInsuranceClaimNumber(normalize(request.getInsuranceClaimNumber()));
        }
        if (request.getChassis() != null) {
            process.setChassis(normalize(request.getChassis()));
        }
        if (request.getInvoiceNumber() != null) {
            process.setInvoiceNumber(normalize(request.getInvoiceNumber()));
        }
        if (request.getOssNumber() != null) {
            process.setOssNumber(normalize(request.getOssNumber()));
        }
        if (request.getCarrierName() != null) {
            process.setCarrierName(normalize(request.getCarrierName()));
        }
        if (request.getOccurrenceDate() != null) {
            process.setOccurrenceDate(request.getOccurrenceDate());
        }
        if (request.getClaimDate() != null) {
            process.setClaimDate(request.getClaimDate());
        }
        if (request.getCity() != null) {
            process.setCity(normalize(request.getCity()));
        }
        if (request.getState() != null) {
            process.setState(normalizeUpper(request.getState()));
        }
        if (request.getEstimatedValue() != null) {
            process.setEstimatedValue(request.getEstimatedValue());
        }
        if (request.getDescription() != null) {
            process.setDescription(normalize(request.getDescription()));
        }
        if (request.getStatus() != null) {
            process.setStatus(normalizeUpperOrDefault(request.getStatus(), STATUS_OPEN));
        }

        Process savedProcess = processRepository.save(process);
        registerStageChangeIfNeeded(savedProcess, oldStage, savedProcess.getCurrentStage(), updatedBy);
        registerStatusChangeIfNeeded(savedProcess, oldStatus, savedProcess.getStatus(), updatedBy);
        return toResponse(savedProcess);
    }

    @Transactional(readOnly = true)
    public ProcessResponse getProcessById(UUID id) {
        return toResponse(findProcess(id));
    }

    @Transactional(readOnly = true)
    public Page<ProcessSummaryResponse> getProcesses(
            String processNumber,
            String status,
            UUID currentStage,
            Pageable pageable
    ) {
        return processRepository.findAll(filter(processNumber, status, currentStage), pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public ProcessDashboardResponse getDashboard() {
        return new ProcessDashboardResponse(
                processRepository.count(),
                processRepository.countByStatusIgnoreCase(STATUS_IN_PROGRESS),
                processRepository.countByStatusIgnoreCase(STATUS_PENDING),
                processRepository.sumEstimatedValue()
        );
    }

    @Transactional(readOnly = true)
    public List<ProcessHistoryResponse> getProcessHistory(UUID id) {
        if (!processRepository.existsById(id)) {
            throw new ResourceNotFoundException("Process not found");
        }
        return processHistoryRepository.findByProcessIdOrderByCreatedAtAsc(id).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private Process findProcess(UUID id) {
        return processRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Process not found"));
    }

    private ProcessStage findStage(UUID id) {
        if (id == null) {
            return null;
        }
        return processStageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Process stage not found"));
    }

    private ProcessReason findReason(UUID id) {
        if (id == null) {
            return null;
        }
        return processReasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Process reason not found"));
    }

    private ProcessReason findReason(UUID id, String nameOrCode) {
        if (id != null) {
            return findReason(id);
        }

        String normalized = normalize(nameOrCode);
        if (normalized == null) {
            return null;
        }

        return processReasonRepository.findByNameIgnoreCase(normalized)
                .or(() -> processReasonRepository.findByCodeIgnoreCase(normalized))
                .orElseThrow(() -> new ResourceNotFoundException("Process reason not found"));
    }

    private synchronized String generateProcessNumber() {
        String prefix = "DC-" + Year.now().getValue() + "-";
        int lastSequence = processRepository.findLastProcessNumberByPrefix(prefix + "%")
                .map(number -> number.substring(number.length() - 6))
                .map(Integer::parseInt)
                .orElse(0);

        String processNumber;
        int nextSequence = lastSequence;
        do {
            nextSequence++;
            processNumber = prefix + String.format("%06d", nextSequence);
        } while (processRepository.existsByProcessNumber(processNumber));

        return processNumber;
    }

    private Specification<Process> filter(String processNumber, String status, UUID currentStage) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (processNumber != null && !processNumber.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("processNumber")),
                        "%" + processNumber.trim().toLowerCase() + "%"
                ));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("status")),
                        status.trim().toUpperCase()
                ));
            }
            if (currentStage != null) {
                predicates.add(criteriaBuilder.equal(root.get("currentStage").get("id"), currentStage));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void registerStageChangeIfNeeded(Process process, ProcessStage oldStage, ProcessStage newStage, User user) {
        UUID oldStageId = oldStage == null ? null : oldStage.getId();
        UUID newStageId = newStage == null ? null : newStage.getId();

        if (!Objects.equals(oldStageId, newStageId)) {
            registerHistory(
                    process,
                    "STAGE_CHANGED",
                    "Etapa alterada de " + stageName(oldStage) + " para " + stageName(newStage),
                    user
            );
        }
    }

    private void registerStatusChangeIfNeeded(Process process, String oldStatus, String newStatus, User user) {
        if (!Objects.equals(oldStatus, newStatus)) {
            registerHistory(
                    process,
                    "STATUS_CHANGED",
                    "Status alterado de " + valueOrEmpty(oldStatus) + " para " + valueOrEmpty(newStatus),
                    user
            );
        }
    }

    private void registerHistory(Process process, String type, String message, User user) {
        ProcessHistory history = new ProcessHistory();
        history.setProcess(process);
        history.setType(type);
        history.setMessage(message);
        history.setCreatedBy(user);
        processHistoryRepository.save(history);
    }

    private ProcessResponse toResponse(Process process) {
        ProcessResponse response = new ProcessResponse();
        response.setId(process.getId());
        response.setProcessNumber(process.getProcessNumber());
        response.setOperationType(process.getOperationType());
        response.setClientName(process.getClientName());
        response.setInsuranceCompany(process.getInsuranceCompany());
        response.setDealershipName(process.getDealershipName());
        response.setCurrentStageId(process.getCurrentStage() == null ? null : process.getCurrentStage().getId());
        response.setCurrentStageName(process.getCurrentStage() == null ? null : process.getCurrentStage().getName());
        response.setCurrentReasonId(process.getCurrentReason() == null ? null : process.getCurrentReason().getId());
        response.setCurrentReasonName(process.getCurrentReason() == null ? null : process.getCurrentReason().getName());
        response.setInsuranceClaimNumber(process.getInsuranceClaimNumber());
        response.setChassis(process.getChassis());
        response.setInvoiceNumber(process.getInvoiceNumber());
        response.setOssNumber(process.getOssNumber());
        response.setCarrierName(process.getCarrierName());
        response.setOccurrenceDate(process.getOccurrenceDate());
        response.setClaimDate(process.getClaimDate());
        response.setCity(process.getCity());
        response.setState(process.getState());
        response.setEstimatedValue(process.getEstimatedValue());
        response.setDescription(process.getDescription());
        response.setStatus(process.getStatus());
        response.setCreatedById(process.getCreatedBy() == null ? null : process.getCreatedBy().getId());
        response.setCreatedByName(process.getCreatedBy() == null ? null : process.getCreatedBy().getName());
        response.setCreatedAt(process.getCreatedAt());
        response.setUpdatedAt(process.getUpdatedAt());
        return response;
    }

    private ProcessSummaryResponse toSummaryResponse(Process process) {
        ProcessSummaryResponse response = new ProcessSummaryResponse();
        response.setId(process.getId());
        response.setProcessNumber(process.getProcessNumber());
        response.setClientName(process.getClientName());
        response.setInsuranceCompany(process.getInsuranceCompany());
        response.setDealershipName(process.getDealershipName());
        response.setCurrentStageName(process.getCurrentStage() == null ? null : process.getCurrentStage().getName());
        response.setCurrentReasonName(process.getCurrentReason() == null ? null : process.getCurrentReason().getName());
        response.setStatus(process.getStatus());
        response.setEstimatedValue(process.getEstimatedValue());
        response.setCreatedAt(process.getCreatedAt());
        response.setUpdatedAt(process.getUpdatedAt());
        return response;
    }

    private ProcessHistoryResponse toHistoryResponse(ProcessHistory history) {
        ProcessHistoryResponse response = new ProcessHistoryResponse();
        response.setId(history.getId());
        response.setType(history.getType());
        response.setMessage(history.getMessage());
        response.setCreatedById(history.getCreatedBy() == null ? null : history.getCreatedBy().getId());
        response.setCreatedByName(history.getCreatedBy() == null ? null : history.getCreatedBy().getName());
        response.setCreatedAt(history.getCreatedAt());
        return response;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeUpperOrDefault(String value, String defaultValue) {
        String normalized = normalizeUpper(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String stageName(ProcessStage stage) {
        return stage == null ? "Sem etapa" : stage.getName();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "vazio" : value;
    }
}
