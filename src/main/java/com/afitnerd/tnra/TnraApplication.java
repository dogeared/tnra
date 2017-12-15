package com.afitnerd.tnra;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Date;

@SpringBootApplication
public class TnraApplication {

    public static void main(String[] args) {
        SpringApplication.run(TnraApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(UserRepository userRepository, PostRepository postRepository) {
        return (args) -> {
            // placeholder for testing
        };
    }
}
