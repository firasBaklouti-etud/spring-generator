package ${packageName}.controller;

<#if security.principalEntity??>
import ${packageName}.entity.${security.principalEntity};
import ${packageName}.repository.${security.principalEntity}Repository;
</#if>
import ${packageName}.entity.Role;
import ${packageName}.repository.RoleRepository;
import ${packageName}.dto.RoleDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin REST controller for managing user-role assignments.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserRoleController {

<#if security.principalEntity??>
    private final ${security.principalEntity}Repository userRepository;
</#if>
    private final RoleRepository roleRepository;

    public UserRoleController(
<#if security.principalEntity??>
            ${security.principalEntity}Repository userRepository,
</#if>
            RoleRepository roleRepository) {
<#if security.principalEntity??>
        this.userRepository = userRepository;
</#if>
        this.roleRepository = roleRepository;
    }

<#if security.principalEntity??>
    /**
     * Get roles assigned to a user
     */
    @GetMapping("/{id}/roles")
    @Transactional(readOnly = true)
    public ResponseEntity<List<RoleDto>> getUserRoles(@PathVariable ${pkType} id) {
        return userRepository.findById(id)
                .map(user -> {
                    List<RoleDto> roles = user.getRoles().stream()
                            .map(role -> new RoleDto(
                                    role.getId(),
                                    role.getName(),
                                    role.getDescription(),
                                    role.getPermissions()))
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(roles);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Assign a role to a user
     */
    @PostMapping("/{id}/roles")
    @Transactional
    public ResponseEntity<List<RoleDto>> assignRole(
            @PathVariable ${pkType} id,
            @RequestBody java.util.Map<String, String> body) {
        String roleName = body.get("roleName");
        if (roleName == null || roleName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ${security.principalEntity} user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role == null) {
            return ResponseEntity.badRequest().build();
        }

        // Check if user already has this role
        boolean alreadyHasRole = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(roleName));
        if (!alreadyHasRole) {
            List<Role> roles = new java.util.ArrayList<>(user.getRoles());
            roles.add(role);
            user.setRoles(roles);
            userRepository.save(user);
        }

        List<RoleDto> updatedRoles = user.getRoles().stream()
                .map(r -> new RoleDto(r.getId(), r.getName(), r.getDescription(), r.getPermissions()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(updatedRoles);
    }

    /**
     * Remove a role from a user
     */
    @DeleteMapping("/{id}/roles/{roleId}")
    @Transactional
    public ResponseEntity<Void> removeRole(
            @PathVariable ${pkType} id,
            @PathVariable Long roleId) {
        ${security.principalEntity} user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Role> updatedRoles = user.getRoles().stream()
                .filter(r -> !r.getId().equals(roleId))
                .collect(Collectors.toList());
        user.setRoles(updatedRoles);
        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }
</#if>
}
