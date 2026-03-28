package com.afitnerd.tnra;

import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.server.AppShellSettings;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
public class TnraApplication implements AppShellConfigurator {

    @Value("${application.timezone:UTC}")
    private String applicationTimeZone;

    @Override
    public void configurePage(AppShellSettings settings) {
        // Inject font variables as inline <style> PREPENDED to <head> before
        // any Vaadin/Lumo stylesheets. Prevents flash where Lumo's default
        // font briefly renders before our override takes effect.
        // All system sans-serif — zero external fonts, zero flash.
        settings.addInlineWithContents(
            Inline.Position.PREPEND,
            "html, :root { " +
                "--lumo-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif !important; " +
                "--tnra-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif; " +
                "--tnra-font-display: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif; " +
                "--tnra-font-data: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif; " +
                "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif; " +
            "} " +
            "h1, h2, h3 { font-family: var(--tnra-font-display); font-weight: 700; letter-spacing: -0.02em; }",
            Inline.Wrapping.STYLESHEET
        );
    }

    @Bean
    public TaskExecutor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        return executor;
    }

    public static void main(String[] args) {
        SpringApplication.run(TnraApplication.class, args);
    }

    @PostConstruct
    public void executeAfterMain() {
        TimeZone.setDefault(TimeZone.getTimeZone(applicationTimeZone));
    }

    @Bean
    public CommandLineRunner demo(
        UserRepository userRepository, PostRepository postRepository
    ) {
        return (args) -> {
            // placeholder for testing
        };
    }
}
