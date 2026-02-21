package ${packageName}.security;

<#if definedRoles?? && (definedRoles?size > 0)>
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * In-memory user configuration for development/testing.
 * One user is created per defined role with a default password.
 *
 * <p>This is generated because no user table was selected.
 * For production, select a user table in the generator or implement
 * a custom {@link UserDetailsService} backed by a database.
 */
@Configuration
public class InMemoryUserConfig {

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        <#list definedRoles as role>
        UserDetails ${role.name?lower_case}User = User.builder()
                .username("${role.name?lower_case}")
                .password(passwordEncoder.encode("password"))
                .roles("${role.name}")
                .build();
        </#list>

        return new InMemoryUserDetailsManager(
                <#list definedRoles as role>${role.name?lower_case}User<#if role_has_next>, </#if></#list>
        );
    }
}
<#else>
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * In-memory user configuration with default USER and ADMIN roles.
 * For production use, configure a database-backed UserDetailsService.
 */
@Configuration
public class InMemoryUserConfig {

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("password"))
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }
}
</#if>
