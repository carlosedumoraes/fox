package com.fox.service;

import com.fox.dto.RoleResponse;
import com.fox.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAllByOrderByNameAsc().stream()
                .map(role -> new RoleResponse(role.getName(), role.getDescription()))
                .toList();
    }
}
