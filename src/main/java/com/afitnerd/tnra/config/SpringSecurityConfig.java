package com.afitnerd.tnra.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Value("${tnra.auth.login-registration-id:keycloak}")
    private String loginRegistrationId;

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SpringSecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.oauth2LoginPage("/oauth2/authorization/" + loginRegistrationId);
        });

        http
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(
                    userInfo -> userInfo.userAuthoritiesMapper(grantedAuthoritiesMapper())
                )
            )
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID"));

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler handler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}");
        return handler;
    }

    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                mappedAuthorities.add(authority);

                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    extractKeycloakRoles(oidcUserAuthority.getIdToken().getClaims(), mappedAuthorities);
                }
            });

            return mappedAuthorities;
        };
    }

    private void extractKeycloakRoles(Map<String, Object> claims, Set<GrantedAuthority> mappedAuthorities) {
        if (claims.get("realm_access") instanceof Map<?, ?> realmAccess
                && realmAccess.get("roles") instanceof Collection<?> roles) {
            roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()))
                .forEach(mappedAuthorities::add);
        }
    }
}
