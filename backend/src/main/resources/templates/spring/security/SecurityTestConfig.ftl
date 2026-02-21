package ${packageName}.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
<#if security.authenticationType?? && security.authenticationType == "JWT">
import ${packageName}.config.JwtUtil;
</#if>

/**
 * Test-specific security configuration.
 *
 * <p>Provides in-memory users and a {@link PasswordEncoder} so that
 * integration tests can authenticate without depending on external
 * data sources.
 */
@TestConfiguration
public class SecurityTestConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService testUserDetailsService() {
        UserDetails admin = User.builder()
                .username("admin@admin.com")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        UserDetails user = User.builder()
                .username("user@test.com")
                .password(passwordEncoder().encode("user123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }
<#if security.authenticationType?? && security.authenticationType == "JWT">

    @Bean
    public JwtUtil jwtUtil() {
<#if security.signingAlgorithm?? && security.signingAlgorithm == "RS256">
        // RS256 – JwtUtil reads key files from paths configured via
        // jwt.private-key-path / jwt.public-key-path properties.
        // Ensure the test profile supplies valid test key paths.
        try {
            return new JwtUtil(
                    "classpath:keys/private.pem",
                    "classpath:keys/public.pem");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize test JwtUtil", e);
        }
<#else>
        // HS256 – the util picks up jwt.secret from application-test.properties.
        // The default embedded secret is sufficient for tests.
        return new JwtUtil();
</#if>
    }
</#if>
}
