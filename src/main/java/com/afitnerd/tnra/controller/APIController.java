package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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

    @GetMapping("/my_last_post")
    Post getMyLastPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        Post post = postService.getLastFinishedPost(user);
        post.getUser().setPosts(null);
        return post;
    }
}
