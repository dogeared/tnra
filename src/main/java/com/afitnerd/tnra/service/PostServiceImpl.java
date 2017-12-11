package com.afitnerd.tnra.service;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    private PostRepository postRepository;

    public PostServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public Post startPost(User user) {
        Assert.notNull(user, "User cannot be null");
        ensureNoInProgressPost(user);

        Post post = new Post();
        post.setUser(user);
        return postRepository.save(post);
    }

    @Override
    public Post getInProgressPost(User user) {
        Assert.notNull(user, "User cannot be null");
        return ensureOneInProgressPost(user);
    }

    @Override
    public void finishPost(Post post) {

    }

    @Override
    public Post updateStats(User user, Stats stats) {
        Post post = ensureOneInProgressPost(user);
        stats = mergeStats(post.getStats(), stats);
        post.setStats(stats);
        return postRepository.save(post);
    }

    @Override
    public void updateIntro(User user, Intro intro) {

    }

    @Override
    public void updatePersonal(User user, Category personal) {

    }

    @Override
    public void updateFamnily(User user, Category personal) {

    }

    @Override
    public void updateWork(User user, Category personal) {

    }

    private Stats mergeStats(Stats origStats, Stats newStats) {
        Assert.notNull(origStats, "Orig stats must not be null");
        Assert.notNull(newStats, "New stats must not be null");
        // TODO this can be better (reflection)
        if (newStats.getExercise() != null) {
            origStats.setExercise(newStats.getExercise());
        }
        if (newStats.getGtg() != null) {
            origStats.setGtg(newStats.getGtg());
        }
        if (newStats.getMeditate() != null) {
            origStats.setMeditate(newStats.getMeditate());
        }
        if (newStats.getMeetings() != null) {
            origStats.setMeetings(newStats.getMeetings());
        }
        if (newStats.getPray() != null) {
            origStats.setPray(newStats.getPray());
        }
        if (newStats.getRead() != null) {
            origStats.setRead(newStats.getRead());
        }
        if (newStats.getSponsor() != null) {
            origStats.setSponsor(newStats.getSponsor());
        }
        return origStats;
    }

    private void ensureNoInProgressPost(User user) {
        // check to see if there's already post in progress
        List<Post> posts = postRepository.findByUserAndState(user, PostState.IN_PROGRESS);
        if (posts != null && !posts.isEmpty()) {
            ensureDeterminateState(posts, user);
            throw new PostException(
                "Can't start new post for " + user.getEmail() + ". Existing post already in progress."
            );
        }
    }

    private Post ensureOneInProgressPost(User user) {
        // check to see if there's already post in progress
        List<Post> posts = postRepository.findByUserAndState(user, PostState.IN_PROGRESS);
        if (posts == null || posts.isEmpty()) {
            throw new PostException("Expected an in progress post for " + user.getEmail() + " but found none.");
        }
        ensureDeterminateState(posts, user);
        return posts.get(0);
    }

    private void ensureDeterminateState(List<Post> posts, User user) {
        if (posts != null && posts.size() > 1) {
            throw new PostException(
                user.getEmail() + " is in an indeterminate state with " + posts.size() + " posts in progress."
            );
        }
    }
}
