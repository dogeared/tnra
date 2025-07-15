package com.afitnerd.tnra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

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
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.ignoringRequestMatchers(allowedPaths))
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true)
                .failureUrl("/?error=true"))
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
}
