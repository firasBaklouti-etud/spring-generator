package ${packageName}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
<#if security.socialLogins?seq_contains("GOOGLE")>
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
</#if>

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 client registration configuration for social login providers.
 * Registers client credentials for each enabled provider (Google, GitHub, Facebook).
 */
@Configuration
public class OAuth2LoginConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();
<#if security.socialLogins?seq_contains("GOOGLE")>
        registrations.add(googleClientRegistration());
</#if>
<#if security.socialLogins?seq_contains("GITHUB")>
        registrations.add(githubClientRegistration());
</#if>
<#if security.socialLogins?seq_contains("FACEBOOK")>
        registrations.add(facebookClientRegistration());
</#if>
        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

<#if security.socialLogins?seq_contains("GOOGLE")>
    private ClientRegistration googleClientRegistration() {
<#assign googleConfig = (security.socialProviderConfigs["GOOGLE"])!>
        return ClientRegistration.withRegistrationId("google")
                .clientId("${(googleConfig.clientId)!'${"$"}{GOOGLE_CLIENT_ID}'}")
                .clientSecret("${(googleConfig.clientSecret)!'${"$"}{GOOGLE_CLIENT_SECRET}'}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
    }

</#if>
<#if security.socialLogins?seq_contains("GITHUB")>
    private ClientRegistration githubClientRegistration() {
<#assign githubConfig = (security.socialProviderConfigs["GITHUB"])!>
        return ClientRegistration.withRegistrationId("github")
                .clientId("${(githubConfig.clientId)!'${"$"}{GITHUB_CLIENT_ID}'}")
                .clientSecret("${(githubConfig.clientSecret)!'${"$"}{GITHUB_CLIENT_SECRET}'}")
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
    }

</#if>
<#if security.socialLogins?seq_contains("FACEBOOK")>
    private ClientRegistration facebookClientRegistration() {
<#assign facebookConfig = (security.socialProviderConfigs["FACEBOOK"])!>
        return ClientRegistration.withRegistrationId("facebook")
                .clientId("${(facebookConfig.clientId)!'${"$"}{FACEBOOK_CLIENT_ID}'}")
                .clientSecret("${(facebookConfig.clientSecret)!'${"$"}{FACEBOOK_CLIENT_SECRET}'}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/v18.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v18.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();
    }

</#if>
}
