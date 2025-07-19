package com.afitnerd.tnra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String[] allowedPaths = {
            "/", "/index.html", "/login/callback", "/h2-console/**", "/api/v1/pq", "/api/v1/post",
            "/api/v1/start", "/api/v1/finish", "/api/v1/tnra", "/api/v1/email", "/api/v1/show",
            "/api/v1/wid", "/api/v1/kry", "/api/v1/wha",
            "/api/v1/per", "/api/v1/fam", "/api/v1/wor",
            "/api/v1/sta", "/api/v1/exe", "/api/v1/gtg", "/api/v1/med",
            "/api/v1/mee", "/api/v1/pra", "/api/v1/rea", "/api/v1/spo",
            "/fonts/**", "/static/images/**", "/img/**", "/css/**", "/js/**", "/favicon.ico",
            "/VAADIN/**", "/HEARTBEAT/**", "/UIDL/**", "/PUSH/**"
        };
        
        http
            .headers(headers -> headers.frameOptions(options -> options.sameOrigin()))
            .authorizeHttpRequests(requests -> requests
                .requestMatchers(allowedPaths).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.ignoringRequestMatchers(allowedPaths))
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true)
                .failureUrl("/?error=true")
                .userInfoEndpoint(
                    userInfo -> userInfo.userAuthoritiesMapper(grantedAuthoritiesMapper())
                )
            )
            .logout(logout -> logout
                .logoutSuccessHandler(logoutSuccessHandler())
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID"));
        
        return http.build();
    }
    
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        SimpleUrlLogoutSuccessHandler handler = new SimpleUrlLogoutSuccessHandler();
        handler.setDefaultTargetUrl("/");
        return handler;
    }
    
    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            
            authorities.forEach(authority -> {
                // Keep existing authorities
                mappedAuthorities.add(authority);
                
                // Extract groups from OIDC ID token
                if (authority instanceof OidcUserAuthority) {
                    OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;
                    extractGroups(oidcUserAuthority.getIdToken().getClaims(), mappedAuthorities);
                }
                // Extract groups from OAuth2 user info
                else if (authority instanceof OAuth2UserAuthority) {
                    OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;
                    extractGroups(oauth2UserAuthority.getAttributes(), mappedAuthorities);
                }
            });
            
            return mappedAuthorities;
        };
    }
    
    private void extractGroups(Map<String, Object> claims, Set<GrantedAuthority> mappedAuthorities) {
        // Try different possible group claim names
        Object groups = claims.get("groups");
        if (groups == null) {
            groups = claims.get("roles");
        }
        if (groups == null) {
            groups = claims.get("authorities");
        }
        
        if (groups instanceof Collection) {
            Collection<?> groupsCollection = (Collection<?>) groups;
            mappedAuthorities.addAll(
                groupsCollection.stream()
                    .map(Object::toString)
                    .map(group -> group.startsWith("ROLE_") ? group : "ROLE_" + group.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet())
            );
        } else if (groups instanceof String) {
            String groupsString = (String) groups;
            // Handle comma-separated groups
            String[] groupArray = groupsString.split(",");
            mappedAuthorities.addAll(
                Arrays.stream(groupArray)
                    .map(String::trim)
                    .filter(group -> !group.isEmpty())
                    .map(group -> group.startsWith("ROLE_") ? group : "ROLE_" + group.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet())
            );
        }
    }
}
