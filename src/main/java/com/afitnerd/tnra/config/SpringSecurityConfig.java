package com.afitnerd.tnra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .antMatchers(
                "/api/v1/post",
                "/api/v1/wid", "/api/v1/kry", "/api/v1/wha",
                "/api/v1/per", "/api/v1/fam", "/api/v1/wor",
                "/api/v1/sta", "/api/v1/show", "/api/v1/start", "/api/v1/finish", "/api/v1/tnra", "/api/v1/email",
                "/api/v1/exe", "/api/v1/gtg", "/api/v1/med", "/api/v1/mee", "/api/v1/pra", "/api/v1/rea", "/api/v1/spo"
            ).permitAll()
            .and()
            .csrf().ignoringAntMatchers(
                "/api/v1/post",
                "/api/v1/wid", "/api/v1/kry", "/api/v1/wha",
                "/api/v1/per", "/api/v1/fam", "/api/v1/wor",
                "/api/v1/sta", "/api/v1/show", "/api/v1/start", "/api/v1/finish", "/api/v1/tnra", "/api/v1/email",
                "/api/v1/exe", "/api/v1/gtg", "/api/v1/med", "/api/v1/mee", "/api/v1/pra", "/api/v1/rea", "/api/v1/spo"
            );
    }
}
