package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.JsonViews;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.GoToGuySetRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.EMailService;
import com.afitnerd.tnra.service.PostService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class APIController {

    private final PostService postService;
    private final EMailService eMailService;

    private final UserRepository userRepository;
    private final GoToGuySetRepository gtgSetRepository;

    public APIController(
        PostService postService, EMailService eMailService,
        UserRepository userRepository, GoToGuySetRepository gtgSetRepository
    ) {
        this.postService = postService;
        this.eMailService = eMailService;
        this.userRepository = userRepository;
        this.gtgSetRepository = gtgSetRepository;
    }

    @GetMapping("/me")
    Principal me(Principal me) {
        return me;
    }

    @JsonView(JsonViews.Sparse.class)
    @GetMapping("/users")
    public  Iterable<User> users() {
        return userRepository.findByActiveTrue();
    }

    @JsonView(JsonViews.Sparse.class)
    @GetMapping("/gtg_latest")
    public GoToGuySet gtgLatest() {
        return gtgSetRepository.findTopByOrderByStartDateDesc();
    }

    @GetMapping("/in_progress")
    Optional<Post> getInProgressPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.getOptionalInProgressPost(user);
    }

    @PostMapping("/in_progress")
    Post updatePost(Principal me, @RequestBody Post post) {
        User user = userRepository.findByEmail(me.getName());
        Post existingPost = postService.getOptionalInProgressPost(user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No in-progress post found"));
        existingPost.setIntro(post.getIntro());
        existingPost.setPersonal(post.getPersonal());
        existingPost.setFamily(post.getFamily());
        existingPost.setWork(post.getWork());
        return postService.savePost(existingPost);
    }

    @GetMapping("/complete")
    Optional<Post> getCompletePost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.getOptionalCompletePost(user);
    }

    @GetMapping("/start_from_app")
    Post startPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        return postService.startPost(user);
    }

    @PostMapping("/finish_from_app")
    Post finishPost(Principal me) {
        User user = userRepository.findByEmail(me.getName());
        Post post = postService.finishPost(user);
        eMailService.sendMailToAll(post);
        return post;
    }
}
