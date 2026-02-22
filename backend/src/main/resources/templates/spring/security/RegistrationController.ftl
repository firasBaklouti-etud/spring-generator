package ${packageName}.controller;

import ${packageName}.entity.${security.principalEntity};
import ${packageName}.repository.${security.principalEntity}Repository;
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
import ${packageName}.entity.Role;
import ${packageName}.repository.RoleRepository;
</#if>
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class RegistrationController {

    private final ${security.principalEntity}Repository userRepository;
    private final PasswordEncoder passwordEncoder;
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
    private final RoleRepository roleRepository;
</#if>

    public RegistrationController(
            ${security.principalEntity}Repository userRepository,
            PasswordEncoder passwordEncoder<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">,
            RoleRepository roleRepository</#if>) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
        this.roleRepository = roleRepository;
</#if>
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {
        // Check if user already exists
        if (userRepository.findBy${security.usernameField?cap_first}(username).isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        // Create new user
        ${security.principalEntity} user = new ${security.principalEntity}();
        user.set${security.usernameField?cap_first}(username);
        user.set${security.passwordField?cap_first}(passwordEncoder.encode(password));

<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
        // Assign default USER role
        Role defaultRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER")));
        user.setRoles(List.of(defaultRole));
</#if>

        userRepository.save(user);
        return "redirect:/login?registered=true";
    }
}
