package ${packageName}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
<#assign isAnnotationBased = security.securityStyle?? && security.securityStyle == "ANNOTATION">

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class FormLoginSecurityConfig {

    private final AuthenticationProvider authenticationProvider;

    public FormLoginSecurityConfig(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                // Allow login and registration pages
                .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                // Allow Swagger/OpenAPI endpoints
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // Allow H2 Console
                .requestMatchers("/h2-console/**").permitAll()
                // Allow Actuator
                .requestMatchers("/actuator/**").permitAll()
<#if !isAnnotationBased>
<#if security.rules?? && (security.rules?size > 0)>
                // Custom Security Rules
<#list security.rules as rule>
<#if rule.method == "ALL">
                .requestMatchers("${rule.path}").<#if rule.rule == "PERMIT_ALL">permitAll()<#elseif rule.rule == "AUTHENTICATED">authenticated()<#elseif rule.rule == "HAS_ROLE">hasRole("${rule.role}")</#if>
<#else>
                .requestMatchers(org.springframework.http.HttpMethod.${rule.method}, "${rule.path}").<#if rule.rule == "PERMIT_ALL">permitAll()<#elseif rule.rule == "AUTHENTICATED">authenticated()<#elseif rule.rule == "HAS_ROLE">hasRole("${rule.role}")</#if>
</#if>
</#list>
</#if>
</#if>
                // Default catch-all
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error=true")
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout=true")
                    .permitAll()
            )
<#if security.rememberMeEnabled?? && security.rememberMeEnabled>
            .rememberMe(rememberMe -> rememberMe
                    .key("${"$"}{app.remember-me.key:uniqueAndSecretKey}")
                    .tokenValiditySeconds(2592000)
<#if security.rememberMeMode?? && security.rememberMeMode == "ALWAYS">
                    .alwaysRemember(true)
</#if>
            )
</#if>
            .authenticationProvider(authenticationProvider)
            ;

        return http.build();
    }
}
