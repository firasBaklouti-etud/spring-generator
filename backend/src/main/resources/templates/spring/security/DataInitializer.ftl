package ${packageName}.config;

import ${packageName}.entity.Role;
import ${packageName}.repository.RoleRepository;
<#if security.principalEntity??>
import ${packageName}.entity.${security.principalEntity};
import ${packageName}.repository.${security.principalEntity}Repository;
</#if>
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds default roles and admin user on application startup.
 * Idempotent: checks existence before creating.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
<#if security.principalEntity??>
    private final ${security.principalEntity}Repository userRepository;
</#if>

    public DataInitializer(
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder<#if security.principalEntity??>,
            ${security.principalEntity}Repository userRepository</#if>) {
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
<#if security.principalEntity??>
        this.userRepository = userRepository;
</#if>
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initializing default roles and admin user...");

        // Seed default roles
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role role = new Role("ADMIN");
            role.setDescription("Administrator with full access");
            Set<String> permissions = new HashSet<>();
<#if security.definedRoles??>
<#list security.definedRoles as definedRole>
<#if definedRole.name == "ADMIN" && definedRole.permissions??>
<#list definedRole.permissions as perm>
            permissions.add("${perm}");
</#list>
</#if>
</#list>
</#if>
            if (permissions.isEmpty()) {
                permissions.add("READ");
                permissions.add("WRITE");
                permissions.add("DELETE");
                permissions.add("ADMIN");
            }
            role.setPermissions(permissions);
            log.info("Created ADMIN role with permissions: {}", permissions);
            return roleRepository.save(role);
        });

        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role role = new Role("USER");
            role.setDescription("Standard user with basic access");
            Set<String> permissions = new HashSet<>();
<#if security.definedRoles??>
<#list security.definedRoles as definedRole>
<#if definedRole.name == "USER" && definedRole.permissions??>
<#list definedRole.permissions as perm>
            permissions.add("${perm}");
</#list>
</#if>
</#list>
</#if>
            if (permissions.isEmpty()) {
                permissions.add("READ");
            }
            role.setPermissions(permissions);
            log.info("Created USER role with permissions: {}", permissions);
            return roleRepository.save(role);
        });

<#if security.definedRoles??>
<#list security.definedRoles as definedRole>
<#if definedRole.name != "ADMIN" && definedRole.name != "USER">
        roleRepository.findByName("${definedRole.name}").orElseGet(() -> {
            Role role = new Role("${definedRole.name}");
<#if definedRole.description??>
            role.setDescription("${definedRole.description}");
</#if>
            Set<String> permissions = new HashSet<>();
<#if definedRole.permissions??>
<#list definedRole.permissions as perm>
            permissions.add("${perm}");
</#list>
</#if>
            role.setPermissions(permissions);
            log.info("Created ${definedRole.name} role");
            return roleRepository.save(role);
        });
</#if>
</#list>
</#if>

<#if security.principalEntity??>
        // Seed default admin user
        String adminUsername = "admin@admin.com";
        if (userRepository.findBy${security.usernameField?cap_first}(adminUsername).isEmpty()) {
            ${security.principalEntity} admin = new ${security.principalEntity}();
            admin.set${security.usernameField?cap_first}(adminUsername);
            admin.set${security.passwordField?cap_first}(passwordEncoder.encode("admin123"));
<#if security.usernameField != "email">
            admin.setEmail(adminUsername);
</#if>
<#if security.usernameField != "username" && security.usernameField != "email">
            admin.setUsername("admin");
</#if>
            admin.setRoles(List.of(adminRole));
            userRepository.save(admin);
            log.info("Created default admin user: {} (password: admin123)", adminUsername);
        } else {
            log.info("Admin user already exists, skipping creation.");
        }
</#if>

        log.info("Data initialization complete.");
    }
}
