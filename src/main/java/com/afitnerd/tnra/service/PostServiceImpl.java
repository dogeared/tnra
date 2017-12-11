package com.afitnerd.tnra.service;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    private static Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

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
        stats = mergeReplace(post.getStats(), stats);
        post.setStats(stats);
        return postRepository.save(post);
    }

    @Override
    public Post updateIntro(User user, Intro intro) {
        Post post = ensureOneInProgressPost(user);
        intro = mergeReplace(post.getIntro(), intro);
        post.setIntro(intro);
        return postRepository.save(post);
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

    private <T> T mergeReplace(T origOne, T newOne) {
        Assert.notNull(origOne, "Orig " + origOne.getClass().getName() + " must not be null." );
        Assert.notNull(newOne, "New " + origOne.getClass().getName() + " must not be null." );

        PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(origOne.getClass());
        for (PropertyDescriptor pd : descriptors) {
            if ("class".equals(pd.getName())) { continue; }
            try {
                Object getterResult = pd.getReadMethod().invoke(newOne);
                if (getterResult != null) {
                    pd.getWriteMethod().invoke(origOne, getterResult);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Reflection error for {}: {}", pd.getName(), e.getMessage(), e);
            }
        }
        return origOne;
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
