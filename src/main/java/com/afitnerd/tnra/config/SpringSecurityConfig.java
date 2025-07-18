package com.afitnerd.tnra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import java.util.UUID;
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
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false))
            .rememberMe(remember -> remember
                .tokenValiditySeconds(2592000)
                .key("tnra-remember-me-key")
                .rememberMeParameter("remember-me"))
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true)
                .failureUrl("/?error=true"))
            .logout(logout -> logout
                .logoutSuccessHandler(logoutSuccessHandler())
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me"));
        
        return http.build();
    }
    
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        SimpleUrlLogoutSuccessHandler handler = new SimpleUrlLogoutSuccessHandler();
        handler.setDefaultTargetUrl("/");
        return handler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        String randomPassword = UUID.randomUUID().toString();
        UserDetails user = User.builder()
            .username("oauth2-user")
            .password(passwordEncoder().encode(randomPassword))
            .roles("USER")
            .disabled(true)
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
