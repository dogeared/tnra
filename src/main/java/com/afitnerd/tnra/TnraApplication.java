package com.afitnerd.tnra;

import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.PostRenderer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.TimeZone;

@SpringBootApplication
public class TnraApplication {

    @Value("${application.timezone:UTC}")
    private String applicationTimeZone;

    public static void main(String[] args) {
        SpringApplication.run(TnraApplication.class, args);
    }

    @PostConstruct
    public void executeAfterMain() {
        TimeZone.setDefault(TimeZone.getTimeZone(applicationTimeZone));
    }

    @Bean
    public CommandLineRunner demo(
        UserRepository userRepository, PostRepository postRepository,
        @Qualifier("emailPostRenderer")PostRenderer emailPostRenderer
    ) {
        return (args) -> {
            // placeholder for testing
        };
    }
}
