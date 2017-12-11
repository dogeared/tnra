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
            User user = new User("Micah", "Silverman", "micah@afitnerd.com");
            user = userRepository.save(user);

            Post post = new Post();
            post.setUser(user);

            Intro intro = new Intro();
            intro.setWidwytk("Blarg");
            intro.setKryptonite("Blrag");
            intro.setWhatAndWhen("blarg");

            Category personal = new Category();
            personal.setBest("personal best");
            personal.setWorst("personal worst");

            Category family = new Category();
            family.setBest("family best");
            family.setWorst("family worst");
            
            Category work = new Category();
            work.setBest("work best");
            work.setWorst("work worst");

            Stats stats = new Stats();
            stats.setExercise(7);
            stats.setGtg(7);
            stats.setMeditate(7);
            stats.setMeetings(7);
            stats.setPray(7);
            stats.setRead(7);
            stats.setSponsor(7);

            post.setIntro(intro);
            post.setPersonal(personal);
            post.setFamily(family);
            post.setWork(work);
            post.setStats(stats);

            postRepository.save(post);
        };
    }
}
