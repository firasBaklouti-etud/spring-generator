package ${packageName}.controller;

import ${packageName}.dto.AuthRequest;
import ${packageName}.dto.AuthResponse;
import ${packageName}.dto.RegisterRequest;
import ${packageName}.config.JwtUtil;
<#if security.principalEntity??>
import ${packageName}.entity.${security.principalEntity};
import ${packageName}.repository.${security.principalEntity}Repository;
</#if>
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
import ${packageName}.entity.Role;
import ${packageName}.repository.RoleRepository;
</#if>
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
<#if security.principalEntity??>
    private final ${security.principalEntity}Repository userRepository;
</#if>
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
    private final RoleRepository roleRepository;
</#if>

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder<#if security.principalEntity??>,
            ${security.principalEntity}Repository userRepository</#if><#if security.rbacMode?? && security.rbacMode == "DYNAMIC">,
            RoleRepository roleRepository</#if>) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
<#if security.principalEntity??>
        this.userRepository = userRepository;
</#if>
<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
        this.roleRepository = roleRepository;
</#if>
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Extract roles and permissions from authorities
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toList());
        Set<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        return ResponseEntity.ok(new AuthResponse(token, refreshToken, "Bearer", roles, permissions));
    }

<#if security.principalEntity??>
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findBy${security.usernameField?cap_first}(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        // Create new user
        ${security.principalEntity} user = new ${security.principalEntity}();
        user.set${security.usernameField?cap_first}(request.getUsername());
        user.set${security.passwordField?cap_first}(passwordEncoder.encode(request.getPassword()));
<#if security.usernameField != "email">
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
</#if>
<#if security.usernameField != "username" && security.usernameField != "email">
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
</#if>

<#if security.rbacMode?? && security.rbacMode == "DYNAMIC">
        // Assign default USER role
        Role defaultRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER")));
        user.setRoles(List.of(defaultRole));
</#if>

        userRepository.save(user);

        // Generate tokens - user implements UserDetails
        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Extract roles and permissions
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toList());
        Set<String> permissions = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        return ResponseEntity.ok(new AuthResponse(token, refreshToken, "Bearer", roles, permissions));
    }
</#if>

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody java.util.Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            String username = jwtUtil.extractUsername(refreshToken);

            // Verify the token is not expired
            if (jwtUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(401).build();
            }

            // Generate new access token
            return ResponseEntity.ok(new AuthResponse(
                    jwtUtil.generateTokenFromUsername(username),
                    refreshToken,
                    "Bearer"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}
