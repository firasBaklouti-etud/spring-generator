package ${packageName}.controller;

<#assign hasRegistration = !(security.registrationEnabled?? && !security.registrationEnabled)>
<#if hasRegistration && security.principalEntity??>
import ${packageName}.entity.${security.principalEntity};
import ${packageName}.repository.${security.principalEntity}Repository;
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
import ${packageName}.entity.Role;
import ${packageName}.repository.RoleRepository;
</#if>
import org.springframework.security.crypto.password.PasswordEncoder;
</#if>
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
<#if hasRegistration && security.principalEntity??>
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
</#if>

import java.util.List;

@Controller
public class AuthenticationController {

<#if hasRegistration && security.principalEntity??>
    private final ${security.principalEntity}Repository userRepository;
    private final PasswordEncoder passwordEncoder;
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
    private final RoleRepository roleRepository;
</#if>

    public AuthenticationController(
            ${security.principalEntity}Repository userRepository,
            PasswordEncoder passwordEncoder<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">,
            RoleRepository roleRepository</#if>) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
        this.roleRepository = roleRepository;
</#if>
    }
</#if>

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

<#if hasRegistration>
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
</#if>

<#if hasRegistration && security.principalEntity??>
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
</#if>
}
