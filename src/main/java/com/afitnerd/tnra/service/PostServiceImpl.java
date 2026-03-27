package com.afitnerd.tnra.service;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class PostServiceImpl implements PostService {

    private static Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final StatDefinitionRepository statDefinitionRepository;

    public PostServiceImpl(PostRepository postRepository, StatDefinitionRepository statDefinitionRepository) {
        this.postRepository = postRepository;
        this.statDefinitionRepository = statDefinitionRepository;
    }

    @Override
    @Transactional
    public Post startPost(User user) {
        Assert.notNull(user, "User cannot be null");
        ensureNoInProgressPost(user);

        Post post = new Post();
        post.setUser(user);
        return postRepository.save(post);
    }

    @Override
    public Optional<Post> getOptionalInProgressPost(User user) {
        Assert.notNull(user, "User cannot be null");
        return postRepository.findFirstByUserAndStateOrderByFinishDesc(user, PostState.IN_PROGRESS);
    }

    @Override
    public Optional<Post> getOptionalCompletePost(User user) {
        Assert.notNull(user, "User cannot be null");
        return postRepository.findFirstByUserAndStateOrderByFinishDesc(user, PostState.COMPLETE);
    }

    @Override
    public Post getInProgressPost(User user) {
        Assert.notNull(user, "User cannot be null");
        return ensureOneInProgressPost(user);
    }

    @Override
    public Post getLastFinishedPost(User user) {
        Assert.notNull(user, "User cannot be null");
        return ensureOneFinished(user);
    }

    @Override
    public Post finishPost(User user) {
        Assert.notNull(user, "User cannot be null");
        Post post = ensureCompletePost(user);
        post.setState(PostState.COMPLETE);
        post.setFinish(new Date());
        return postRepository.save(post);
    }

    @Override
    public Post savePost(Post post) {
        Assert.notNull(post, "Post cannot be null");
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post updateStatValue(User user, StatDefinition statDef, Integer value) {
        Post post = ensureOneInProgressPost(user);
        post.setStatValue(statDef, value);
        return postRepository.save(post);
    }

    @Override
    public Post replaceIntro(User user, Intro intro) {
        Post post = ensureOneInProgressPost(user);
        mergeReplace(post.getIntro(), intro);
        return postRepository.save(post);
    }

    @Override
    public Post replacePersonal(User user, Category personal) {
        Post post = ensureOneInProgressPost(user);
        mergeReplace(post.getPersonal(), personal);
        return postRepository.save(post);
    }

    @Override
    public Post replaceFamily(User user, Category family) {
        Post post = ensureOneInProgressPost(user);
        mergeReplace(post.getFamily(), family);
        return postRepository.save(post);
    }

    @Override
    public Post replaceWork(User user, Category work) {
        Post post = ensureOneInProgressPost(user);
        mergeReplace(post.getWork(), work);
        return postRepository.save(post);
    }

    @Override
    public Post updateIntro(User user, Intro intro) {
        Post post = ensureOneInProgressPost(user);
        intro = mergeAppendString(post.getIntro(), intro);
        return postRepository.save(post);
    }

    @Override
    public Post updatePersonal(User user, Category personal) {
        Post post = ensureOneInProgressPost(user);
        personal = mergeAppendString(post.getPersonal(), personal);
        return postRepository.save(post);
    }

    @Override
    public Post updateFamily(User user, Category family) {
        Post post = ensureOneInProgressPost(user);
        family = mergeAppendString(post.getFamily(), family);
        return postRepository.save(post);
    }

    @Override
    public Post updateWork(User user, Category work) {
        Post post = ensureOneInProgressPost(user);
        work = mergeAppendString(post.getWork(), work);
        return postRepository.save(post);
    }

    @Override
    public Page<Post> getPostsPage(User user, Pageable pageable) {
        Assert.notNull(user, "User cannot be null");
        Assert.notNull(pageable, "Pageable cannot be null");
        return postRepository.findByUserOrderByFinishDesc(user, pageable);
    }

    @Override
    public Page<Post> getCompletedPostsPage(User user, Pageable pageable) {
        Assert.notNull(user, "User cannot be null");
        Assert.notNull(pageable, "Pageable cannot be null");
        return postRepository.findByUserAndStateOrderByFinishDesc(user, PostState.COMPLETE, pageable);
    }

    private <T> T mergeAppendString(T origOne, T newOne) {
        Assert.notNull(origOne, "Orig " + origOne.getClass().getName() + " must not be null." );
        Assert.notNull(newOne, "New " + origOne.getClass().getName() + " must not be null." );

        PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(origOne.getClass());
        for (PropertyDescriptor pd : descriptors) {
            if ("class".equals(pd.getName())) { continue; }
            try {
                String newGetterResult = (String) pd.getReadMethod().invoke(newOne);
                if (newGetterResult != null) {
                    String origGetterResult = pd.getReadMethod().invoke(origOne) == null ? "" :
                            "" + pd.getReadMethod().invoke(origOne) + "\n";
                    pd.getWriteMethod().invoke(origOne, origGetterResult + newGetterResult);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Reflection error for {}: {}", pd.getName(), e.getMessage(), e);
            }
        }
        return origOne;
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

    private Post ensureCompletePost(User user) {
        Post post = ensureOneInProgressPost(user);

        String errorBase = "Post for " + getUserDisplayName(user) + " is not complete: ";

        ensureIntro(errorBase, post);
        ensurePersonal(errorBase, post);
        ensureFamily(errorBase, post);
        ensureWork(errorBase, post);
        ensureStats(errorBase, post);

        return post;
    }

    private void ensureIntro(String errorBase, Post post) {
        String introBase = errorBase + "intro - ";
        if (ObjectUtils.isEmpty(post.getIntro().getWidwytk())) {
            throw new PostException(introBase + "widwytk");
        }
        if (ObjectUtils.isEmpty(post.getIntro().getKryptonite())) {
            throw new PostException(introBase + "kryptonite");
        }
        if (ObjectUtils.isEmpty(post.getIntro().getWhatAndWhen())) {
            throw new PostException(introBase + "what and when");
        }
    }

    private void ensurePersonal(String errorBase, Post post) {
        String personalBase = errorBase + "personal - ";
        if (ObjectUtils.isEmpty(post.getPersonal().getBest())) {
            throw new PostException(personalBase + "best");
        }
        if (ObjectUtils.isEmpty(post.getPersonal().getWorst())) {
            throw new PostException(personalBase + "worst");
        }
    }

    private void ensureFamily(String errorBase, Post post) {
        String familyBase = errorBase + "family - ";
        if (ObjectUtils.isEmpty(post.getFamily().getBest())) {
            throw new PostException(familyBase + "best");
        }
        if (ObjectUtils.isEmpty(post.getFamily().getWorst())) {
            throw new PostException(familyBase + "worst");
        }
    }

    private void ensureWork(String errorBase, Post post) {
        String workBase = errorBase + "work - ";
        if (ObjectUtils.isEmpty(post.getWork().getBest())) {
            throw new PostException(workBase + "best");
        }
        if (ObjectUtils.isEmpty(post.getWork().getWorst())) {
            throw new PostException(workBase + "worst");
        }
    }

    private void ensureStats(String errorBase, Post post) {
        String statsBase = errorBase + "stats - ";
        List<StatDefinition> activeStats = statDefinitionRepository.findByArchivedFalseOrderByDisplayOrderAsc();
        for (StatDefinition statDef : activeStats) {
            Integer value = post.getStatValue(statDef.getName());
            if (value == null) {
                throw new PostException(statsBase + statDef.getLabel().toLowerCase());
            }
        }
    }

    private Post ensureOneFinished(User user) {
        Optional<Post> post = postRepository.findFirstByUserAndStateOrderByFinishDesc(user, PostState.COMPLETE);
        return post.orElseThrow(() -> new PostException(getUserDisplayName(user) + " has no finished posts."));
    }

    private void ensureNoInProgressPost(User user) {
        List<Post> posts = postRepository.findByUserAndState(user, PostState.IN_PROGRESS);
        if (posts != null && !posts.isEmpty()) {
            ensureDeterminateState(posts, user);
            throw new PostException(
                "Can't start new post for " + getUserDisplayName(user) + ". Existing post already in progress."
            );
        }
    }

    private Post ensureOneInProgressPost(User user) {
        List<Post> posts = postRepository.findByUserAndState(user, PostState.IN_PROGRESS);
        if (posts == null || posts.isEmpty()) {
            throw new PostException("Expected an in progress post for " + getUserDisplayName(user) + " but found none.");
        }
        ensureDeterminateState(posts, user);
        return posts.get(0);
    }

    private String getUserDisplayName(User user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()
                && user.getLastName() != null && !user.getLastName().isBlank()) {
            return user.getFirstName() + " " + user.getLastName();
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return "unknown user";
    }

    private void ensureDeterminateState(List<Post> posts, User user) {
        if (posts != null && posts.size() > 1) {
            throw new PostException(
                getUserDisplayName(user) + " is in an indeterminate state with " + posts.size() + " posts in progress."
            );
        }
    }
}
