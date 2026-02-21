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
<#if security.authenticationType == "KEYCLOAK_RS" || security.authenticationType == "KEYCLOAK_OAUTH">
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
</#if>
<#assign hasSocialLogins = security.socialLogins?? && (security.socialLogins?size > 0)>
<#if hasSocialLogins>
import ${packageName}.security.CustomOAuth2UserService;
</#if>
<#assign isAnnotationBased = security.securityStyle?? && security.securityStyle == "ANNOTATION">

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

<#if security.authenticationType == "JWT">
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
<#if hasSocialLogins>
    private final CustomOAuth2UserService customOAuth2UserService;
</#if>

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          AuthenticationProvider authenticationProvider<#if hasSocialLogins>,
                          CustomOAuth2UserService customOAuth2UserService</#if>) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
<#if hasSocialLogins>
        this.customOAuth2UserService = customOAuth2UserService;
</#if>
    }
<#elseif security.authenticationType == "KEYCLOAK_RS" || security.authenticationType == "KEYCLOAK_OAUTH">
    // Keycloak configuration is handled via application.properties
</#if>

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                // Allow Auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
<#if hasSocialLogins>
                // Allow Social Auth callback endpoints
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
</#if>
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
<#if security.authenticationType == "JWT">
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
<#if hasSocialLogins>
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService))
            )
</#if>
<#elseif security.authenticationType == "BASIC">
            .httpBasic(org.springframework.security.config.Customizer.withDefaults())
<#elseif security.authenticationType == "FORM_LOGIN">
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
<#elseif security.authenticationType == "KEYCLOAK_RS">
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
            )
<#elseif security.authenticationType == "KEYCLOAK_OAUTH">
            .oauth2Login(org.springframework.security.config.Customizer.withDefaults())
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
            )
</#if>
            ;

        return http.build();
    }

<#if security.authenticationType == "KEYCLOAK_RS" || security.authenticationType == "KEYCLOAK_OAUTH">
    /**
     * Converter that maps Keycloak realm roles to Spring Security authorities.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
</#if>
}
