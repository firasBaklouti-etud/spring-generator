package ${packageName}.service;

import ${packageName}.dto.RoleDto;
import ${packageName}.entity.Role;
import ${packageName}.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing roles (CRUD operations)
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Get all roles
     */
    @Transactional(readOnly = true)
    public List<RoleDto> findAll() {
        return roleRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find role by ID
     */
    @Transactional(readOnly = true)
    public Optional<RoleDto> findById(Long id) {
        return roleRepository.findById(id).map(this::toDto);
    }

    /**
     * Find role by name
     */
    @Transactional(readOnly = true)
    public Optional<RoleDto> findByName(String name) {
        return roleRepository.findByName(name).map(this::toDto);
    }

    /**
     * Create a new role
     */
    public RoleDto create(RoleDto dto) {
        Role role = new Role(dto.getName());
        role.setDescription(dto.getDescription());
        if (dto.getPermissions() != null) {
            role.setPermissions(new HashSet<>(dto.getPermissions()));
        }
        Role saved = roleRepository.save(role);
        return toDto(saved);
    }

    /**
     * Update an existing role
     */
    public Optional<RoleDto> update(Long id, RoleDto dto) {
        return roleRepository.findById(id).map(role -> {
            if (dto.getName() != null) {
                role.setName(dto.getName());
            }
            if (dto.getDescription() != null) {
                role.setDescription(dto.getDescription());
            }
            if (dto.getPermissions() != null) {
                role.setPermissions(new HashSet<>(dto.getPermissions()));
            }
            Role saved = roleRepository.save(role);
            return toDto(saved);
        });
    }

    /**
     * Delete a role by ID
     */
    public void deleteById(Long id) {
        roleRepository.deleteById(id);
    }

    private RoleDto toDto(Role role) {
        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions()
        );
    }
}
