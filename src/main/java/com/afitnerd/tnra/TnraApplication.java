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

import java.util.Map;
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

        configureHomeScreenInstall(settings);
    }

    // Cross-platform "add to home screen" support, wired by hand rather than via
    // the @PWA annotation: @PWA triggers a build-frontend PWA step that crashes
    // on Vaadin 24.9.9 (MissingNode->ObjectNode while reading the token file).
    // This serves the same artifacts statically from META-INF/resources:
    //   - Android/Chrome read manifest.webmanifest (icons + short_name "TNRA").
    //   - iOS Safari (and Chrome/Edge on iOS, all WebKit) read the apple-touch-icon
    //     link + apple-mobile-web-app-* meta tags; the title meta is the label.
    // Icons are full-bleed opaque squares (iOS renders transparency as black and
    // rounds corners itself), generated on-brand per DESIGN.md.
    private void configureHomeScreenInstall(AppShellSettings settings) {
        settings.addLink("manifest", "/manifest.webmanifest");

        // iOS home-screen icon + standalone launch behavior.
        settings.addLink("/icons/apple-touch-icon.png",
            Map.of("rel", "apple-touch-icon", "sizes", "180x180"));
        settings.addMetaTag("apple-mobile-web-app-capable", "yes");
        settings.addMetaTag("apple-mobile-web-app-status-bar-style", "default");
        settings.addMetaTag("apple-mobile-web-app-title", "TNRA");

        // Browser chrome tint to match the brand in standalone mode.
        settings.addMetaTag("theme-color", "#2D6A4F");

        // Favicon (also used by some platforms as a fallback install icon).
        settings.addFavIcon("icon", "/icons/icon-192.png", "192x192");
    }

    @Bean
    public TaskExecutor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean
    public TaskExecutor slackTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("slack-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
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
