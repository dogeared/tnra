package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.EMailService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VaadinPostPresenterImpl implements VaadinPostPresenter {

    private final UserService userService;
    private final OidcUserService oidcUserService;
    private final PostService postService;
    private final EMailService eMailService;
    private final StatDefinitionRepository statDefinitionRepository;

    @Value("#{ @environment['tnra.emailService.enabled'] ?: true }")
    private boolean emailServiceEnabled;

    public VaadinPostPresenterImpl(
        OidcUserService oidcUserService, UserService userService,
        PostService postService, EMailService eMailService,
        StatDefinitionRepository statDefinitionRepository
    ) {
        this.oidcUserService = oidcUserService;
        this.userService = userService;
        this.postService = postService;
        this.eMailService = eMailService;
        this.statDefinitionRepository = statDefinitionRepository;
    }

    @Override
    public Post finishPost(User user) {
        Post post = postService.finishPost(user);
        if (emailServiceEnabled) {
            eMailService.sendMailToAll(post);
        }
        return post;
    }

    @Override
    public Optional<Post> getOptionalInProgressPost(User me) {
        return postService.getOptionalInProgressPost(me);
    }

    @Override
    public User initializeUser() {
        if (oidcUserService.isAuthenticated()) {
            String email = oidcUserService.getEmail();
            User currentUser = userService.getUserByEmail(email);

            if (currentUser == null) {
                throw new IllegalStateException("No user found with email: " + email);
            }

            return currentUser;
        } else {
            throw new IllegalStateException("User is not authenticated");
        }
    }

    @Override
    public Page<Post> getCompletedPostsPage(User me, Pageable pageable) {
        return postService.getCompletedPostsPage(me, pageable);
    }

    @Override
    public Post startPost(User me) {
        return postService.startPost(me);
    }

    @Override
    public Post savePost(Post post) {
        return postService.savePost(post);
    }

    @Override
    public Post updateStatValue(StatDefinition statDef, Integer value) {
        return postService.updateStatValue(initializeUser(), statDef, value);
    }

    @Override
    public List<StatDefinition> getActiveStatDefinitions() {
        return statDefinitionRepository.findByArchivedFalseOrderByDisplayOrderAsc();
    }

    @Override
    public List<User> getAllActiveUsers() {
        return userService.getAllActiveUsers();
    }
}
