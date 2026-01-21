package ${packageName}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    <#if security.authenticationType == "JWT">
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }
    </#if>

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
            .authorizeHttpRequests(auth -> auth
                // Allow Auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                <#if security.rules?? && (security.rules?size > 0)>
                // Custom Rules
                <#list security.rules as rule>
                .requestMatchers(org.springframework.http.HttpMethod.${rule.method}, "${rule.path}").<#if rule.rule == "PERMIT_ALL">permitAll()<#elseif rule.rule == "AUTHENTICATED">authenticated()<#elseif rule.rule == "HAS_ROLE">hasRole("${rule.role}")</#if>
                </#list>
                </#if>
                // Default catch-all
                .anyRequest().authenticated()
            )
            <#if security.authenticationType == "JWT">
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            <#elseif security.authenticationType == "BASIC">
            .httpBasic(org.springframework.security.config.Customizer.withDefaults());
            <#else>
            ;
            </#if>

        return http.build();
    }
}
