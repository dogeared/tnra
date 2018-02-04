package com.afitnerd.tnra;

import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.PostRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TnraApplication {

    public static void main(String[] args) {
        SpringApplication.run(TnraApplication.class, args);
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
