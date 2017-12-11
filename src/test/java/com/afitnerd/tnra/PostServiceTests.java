package com.afitnerd.tnra;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.PostService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.junit4.SpringRunner;

import java.beans.PropertyDescriptor;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostServiceTests {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    User user;

    @Before
    public void setup() {
        user = new User("Micah", "Silverman", "micah@afitnerd.com");
        user = userRepository.save(user);
    }

    @Test
    public void testStartPost_success_baseline() {
        Post post = postService.startPost(user);
        assertNotNull(post);
        assertEquals(PostState.IN_PROGRESS, post.getState());
        assertNotNull(post.getStart());
        assertNull(post.getFinish());
    }

    @Test
    public void testStartPost_fail_null_user() {
        try {
            Post post = postService.startPost(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("User cannot be null", e.getMessage());
        }
    }

    @Test
    public void testStartPost_fail_twice() {
        postService.startPost(user);
        try {
            postService.startPost(user);
            fail();
        } catch (PostException p) {
            assertEquals(
                "Can't start new post for micah@afitnerd.com. Existing post already in progress.",
                p.getMessage()
            );
        }

    }

    @Test
    public void testStartPost_fail_indeterminate_state() {
        // artificially put in progress posts in an indeterminate state
        postRepository.save(new Post(user));
        postRepository.save(new Post(user));
        postRepository.save(new Post(user));
        try {
            postService.startPost(user);
            fail();
        } catch (PostException p) {
            assertEquals(
              "micah@afitnerd.com is in an indeterminate state with 3 posts in progress.",
              p.getMessage()
            );
        }
    }

    // TODO What's the best practices way to enforce that the user is already persisted?
    @Test
    public void testStartPost_fail_transient_user() {
        user = new User("Tes", "Silverman", "tes@afitnerd.com");
        try {
            Post post = postService.startPost(user);
            fail();
        } catch (InvalidDataAccessApiUsageException e) {
            assertTrue(e.getMessage().contains("com.afitnerd.tnra.model.User"));
        }
    }

    @Test
    public void testGetInProgressPost_success() {
        postService.startPost(user);
        Post post = postService.getInProgressPost(user);
        assertNotNull(post);
        assertEquals(PostState.IN_PROGRESS, post.getState());
        assertNotNull(post.getStart());
        assertNull(post.getFinish());
    }

    @Test
    public void testGetInProgressPost_fail_none() {
        try {
            postService.getInProgressPost(user);
            fail();
        } catch (PostException p) {
            assertEquals("Expected an in progress post for micah@afitnerd.com but found none.", p.getMessage());
        }
    }

    @Test
    public void testGetInProgressPost_fail_too_many() {
        // artificially put in progress posts in an indeterminate state
        postRepository.save(new Post(user));
        postRepository.save(new Post(user));
        postRepository.save(new Post(user));
        try {
            postService.getInProgressPost(user);
            fail();
        } catch (PostException p) {
            assertEquals("micah@afitnerd.com is in an indeterminate state with 3 posts in progress.", p.getMessage());
        }
    }

    @Test
    public void testReplaceStats_success_partial() {
        Post post = postService.startPost(user);

        Stats stats = new Stats();
        stats.setExercise(2);
        stats.setGtg(3);
        stats.setMeditate(4);
        postService.replaceStats(user, stats);

        Post post2 = postService.getInProgressPost(user);
        Stats stats2 = post2.getStats();
        assertEquals(2, (int) stats2.getExercise());
        assertEquals(3, (int) stats2.getGtg());
        assertEquals(4, (int) stats2.getMeditate());
        assertNull(stats2.getMeetings());
        assertNull(stats2.getPray());
        assertNull(stats2.getRead());
        assertNull(stats2.getSponsor());
    }
    
    @Test
    public void testReplaceStats_success_mergeExercise() {
        testReplaceStats_success_merge("gtg", "exercise");
    }

    @Test
    public void testReplaceStats_success_mergeGtg() {
        testReplaceStats_success_merge("exercise", "gtg");
    }

    @Test
    public void testReplaceStats_success_mergeMeditation() {
        testReplaceStats_success_merge("exercise", "meditate");
    }

    @Test
    public void testReplaceStats_success_mergeMeetings() {
        testReplaceStats_success_merge("exercise", "meetings");
    }

    @Test
    public void testReplaceStats_success_mergePray() {
        testReplaceStats_success_merge("exercise", "pray");
    }

    @Test
    public void testReplaceStats_success_mergeRead() {
        testReplaceStats_success_merge("exercise", "read");
    }

    @Test
    public void testReplaceStats_success_mergeSponsor() {
        testReplaceStats_success_merge("exercise", "sponsor");
    }

    @Test
    public void testReplaceStats_success_complex() {
        Post post = postService.startPost(user);

        Stats stats = new Stats();
        stats.setExercise(2);
        stats.setGtg(3);
        stats.setMeditate(4);
        stats.setMeetings(5);

        postService.replaceStats(user, stats);

        stats.setMeetings(6);
        stats.setPray(7);

        postService.replaceStats(user, stats);

        Post post2 = postService.getInProgressPost(user);
        Stats stats2 = post2.getStats();

        assertEquals(2, (int) stats2.getExercise());
        assertEquals(3, (int) stats2.getGtg());
        assertEquals(4, (int) stats2.getMeditate());
        assertEquals(6, (int) stats2.getMeetings());
        assertEquals(7, (int) stats2.getPray());
        assertNull(stats2.getRead());
        assertNull(stats2.getSponsor());
    }

    @Test
    public void testReplaceStats_fail_noInProgressPost() {
        Stats stats = new Stats();
        stats.setExercise(2);
        stats.setGtg(3);
        stats.setMeditate(4);
        try {
            postService.replaceStats(user, stats);
            fail();
        } catch (PostException p) {
            assertEquals("Expected an in progress post for micah@afitnerd.com but found none.", p.getMessage());
        }
    }

    @Test
    public void testReplaceIntro_success() {
        Post post = postService.startPost(user);

        post.getIntro().setWidwytk("widwytk");
        post.getIntro().setKryptonite("kryptonite");
        post.getIntro().setWhatAndWhen("what and when");
        postService.replaceIntro(user, post.getIntro());

        Post post2 = postService.getInProgressPost(user);

        assertEquals("widwytk", post2.getIntro().getWidwytk());
        assertEquals("kryptonite", post2.getIntro().getKryptonite());
        assertEquals("what and when", post2.getIntro().getWhatAndWhen());
    }
    
    @Test
    public void testReplaceIntro_success_complex() {
        Post post = postService.startPost(user);

        post.getIntro().setWidwytk("widwytk");
        post.getIntro().setKryptonite("kryptonite");
        post.getIntro().setWhatAndWhen("what and when");
        postService.replaceIntro(user, post.getIntro());

        Post post2 = postService.getInProgressPost(user);
        post2.getIntro().setKryptonite("kryptonite2");
        postService.replaceIntro(user, post2.getIntro());
        
        Post post3 = postService.getInProgressPost(user);

        assertEquals("widwytk", post3.getIntro().getWidwytk());
        assertEquals("kryptonite2", post3.getIntro().getKryptonite());
        assertEquals("what and when", post3.getIntro().getWhatAndWhen());
    }

    private void testReplaceStats_success_merge(String stat1, String stat2) {
        try {
            Post post = postService.startPost(user);
            Stats stats = new Stats();

            PropertyDescriptor pd1 = new PropertyDescriptor(stat1, Stats.class);
            pd1.getWriteMethod().invoke(stats, 2);
            postService.replaceStats(user, stats);

            Stats stats2 = new Stats();
            PropertyDescriptor pd2 = new PropertyDescriptor(stat2, Stats.class);
            pd2.getWriteMethod().invoke(stats2, 3);
            postService.replaceStats(user, stats2);

            Post post2 = postService.getInProgressPost(user);
            Stats stats3 = post2.getStats();
            assertEquals(2, pd1.getReadMethod().invoke(stats3));
            assertEquals(3, pd2.getReadMethod().invoke(stats3));

            // assertNull for everything else
            PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(Stats.class);
            for (PropertyDescriptor pd : descriptors) {
                String name = pd.getName();
                if (!stat1.equals(name) && !stat2.equals(name) && !"class".equals(name)) {
                    assertNull(pd.getReadMethod().invoke(stats3));
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
