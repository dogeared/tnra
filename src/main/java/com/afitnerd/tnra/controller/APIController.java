package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class APIController {

    private PostService postService;
    private UserRepository userRepository;

    public APIController(PostService postService, UserRepository userRepository) {
        this.postService = postService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    Principal me(Principal me) {
        return me;
    }

    @GetMapping("/in_progress")
    Optional<Post> getInProgressPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.getOptionalInProgressPost(user);
    }

    @GetMapping("/complete")
    Optional<Post> getCompletePost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.getOptionalCompletePost(user);
    }

    @GetMapping("/start")
    Post startPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.startPost(user);
    }
}
