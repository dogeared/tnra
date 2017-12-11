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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    @Test
    public void testUpdateIntro_success() {
        Post post = postService.startPost(user);

        post.getIntro().setWidwytk("widwytk");
        post.getIntro().setKryptonite("kryptonite");
        post.getIntro().setWhatAndWhen("what and when");
        postService.updateIntro(user, post.getIntro());

        Post post2 = postService.getInProgressPost(user);

        post2.getIntro().setWidwytk("widwytk");
        post2.getIntro().setKryptonite("kryptonite");
        post2.getIntro().setWhatAndWhen("what and when");
        postService.updateIntro(user, post2.getIntro());

        Post post3 = postService.getInProgressPost(user);

        assertEquals("widwytk\nwidwytk", post3.getIntro().getWidwytk());
        assertEquals("kryptonite\nkryptonite", post3.getIntro().getKryptonite());
        assertEquals("what and when\nwhat and when", post3.getIntro().getWhatAndWhen());
    }

    @Test
    public void testReplacePersonal_success() {
        Post post = postService.startPost(user);

        post.getPersonal().setBest("best");
        post.getPersonal().setWorst("worst");
        postService.replacePersonal(user, post.getPersonal());

        Post post2 = postService.getInProgressPost(user);

        assertEquals("best", post2.getPersonal().getBest());
        assertEquals("worst", post2.getPersonal().getWorst());
    }

    @Test
    public void testUpdatePersonal_success() {
        Post post = postService.startPost(user);

        post.getPersonal().setBest("best");
        post.getPersonal().setWorst("worst");
        postService.replacePersonal(user, post.getPersonal());

        Post post2 = postService.getInProgressPost(user);
        postService.updatePersonal(user, post2.getPersonal());

        Post post3 = postService.getInProgressPost(user);

        assertEquals("best\nbest", post3.getPersonal().getBest());
        assertEquals("worst\nworst", post3.getPersonal().getWorst());
    }

    @Test
    public void testReplaceFamily_success() {
        Post post = postService.startPost(user);

        post.getFamily().setBest("best");
        post.getFamily().setWorst("worst");
        postService.replaceFamily(user, post.getFamily());

        Post post2 = postService.getInProgressPost(user);

        assertEquals("best", post2.getFamily().getBest());
        assertEquals("worst", post2.getFamily().getWorst());
    }

    @Test
    public void testUpdateFamily_success() {
        Post post = postService.startPost(user);

        post.getFamily().setBest("best");
        post.getFamily().setWorst("worst");
        postService.replaceFamily(user, post.getFamily());

        Post post2 = postService.getInProgressPost(user);
        postService.updateFamily(user, post2.getFamily());

        Post post3 = postService.getInProgressPost(user);

        assertEquals("best\nbest", post3.getFamily().getBest());
        assertEquals("worst\nworst", post3.getFamily().getWorst());
    }

    @Test
    public void testReplaceWork_success() {
        Post post = postService.startPost(user);

        post.getWork().setBest("best");
        post.getWork().setWorst("worst");
        postService.replaceWork(user, post.getWork());

        Post post2 = postService.getInProgressPost(user);

        assertEquals("best", post2.getWork().getBest());
        assertEquals("worst", post2.getWork().getWorst());
    }

    @Test
    public void testUpdateWork_success() {
        Post post = postService.startPost(user);

        post.getWork().setBest("best");
        post.getWork().setWorst("worst");
        postService.replaceWork(user, post.getWork());

        Post post2 = postService.getInProgressPost(user);
        postService.updateWork(user, post2.getWork());

        Post post3 = postService.getInProgressPost(user);

        assertEquals("best\nbest", post3.getWork().getBest());
        assertEquals("worst\nworst", post3.getWork().getWorst());
    }

    @Test
    public void testFinish_success() {
        Post post = postService.startPost(user);

        post.getIntro().setWidwytk("widwytk");
        post.getIntro().setKryptonite("kryptonite");
        post.getIntro().setWhatAndWhen("whatandwhen");

        post.getPersonal().setBest("p best");
        post.getPersonal().setWorst("p worst");

        post.getFamily().setBest("f best");
        post.getFamily().setWorst("f worst");

        post.getWork().setBest("w best");
        post.getWork().setWorst("w worst");

        post.getStats().setExercise(7);
        post.getStats().setGtg(7);
        post.getStats().setMeditate(7);
        post.getStats().setMeetings(7);
        post.getStats().setPray(7);
        post.getStats().setRead(7);
        post.getStats().setSponsor(7);
        postService.savePost(post);

        post = postService.finishPost(user);

        assertEquals(PostState.COMPLETE, post.getState());
        assertNotNull(post.getFinish());
    }

    @Test
    public void testFinish_fail_intro_widwytk() {
        Post post = postService.startPost(user);
        finishFailAssert(post, "intro - widwytk");
    }

    @Test
    public void testFinish_fail_intro_empty_widwytk() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "");
        finishFailAssert(post, "intro - widwytk");
    }

    @Test
    public void testFinish_fail_intro_kryptonite() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        finishFailAssert(post, "intro - kryptonite");
    }

    @Test
    public void testFinish_fail_intro_empty_kryptonite() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "");
        finishFailAssert(post, "intro - kryptonite");
    }

    @Test
    public void testFinish_fail_intro_whatandwhen() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        finishFailAssert(post, "intro - what and when");
    }

    @Test
    public void testFinish_fail_intro_empty_whatandwhen() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "");
        finishFailAssert(post, "intro - what and when");
    }

    @Test
    public void testFinish_fail_personal_best() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        finishFailAssert(post, "personal - best");
    }

    @Test
    public void testFinish_fail_personal_empty_best() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "");
        finishFailAssert(post, "personal - best");
    }

    @Test
    public void testFinish_fail_personal_worst() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        finishFailAssert(post, "personal - worst");
    }

    @Test
    public void testFinish_fail_personal_empty_worst() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "");
        finishFailAssert(post, "personal - worst");
    }

    @Test
    public void testFinish_fail_family_best() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        finishFailAssert(post, "family - best");
    }

    @Test
    public void testFinish_fail_family_empty_best() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "");
        finishFailAssert(post, "family - best");
    }

    @Test
    public void testFinish_fail_family_worst() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        finishFailAssert(post, "family - worst");
    }

    @Test
    public void testFinish_fail_family_empty_worst() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "");
        finishFailAssert(post, "family - worst");
    }

    @Test
    public void testFinish_fail_work_best() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        finishFailAssert(post, "work - best");
    }

    @Test
    public void testFinish_fail_work_empty_best() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "");
        finishFailAssert(post, "work - best");
    }

    @Test
    public void testFinish_fail_work_worst() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        finishFailAssert(post, "work - worst");
    }

    @Test
    public void testFinish_fail_work_empty_worst() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setWorst", String.class), "");
        finishFailAssert(post, "work - worst");
    }

    @Test
    public void testFinish_fail_stats_empty_exercise() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setWorst", String.class), "w worst");
        finishFailAssert(post, "stats - exercise");
    }

    @Test
    public void testFinish_fail_stats_empty_gtg() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setWorst", String.class), "w worst");
        post.getStats().setExercise(7);
        postService.savePost(post);
        finishFailAssert(post, "stats - gtg");
    }

    @Test
    public void testFinish_fail_stats_empty_meditate() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setWorst", String.class), "w worst");
        post.getStats().setExercise(7);
        post.getStats().setGtg(7);
        postService.savePost(post);
        finishFailAssert(post, "stats - meditate");
    }

    @Test
    public void testFinish_fail_stats_empty_meetings() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setWorst", String.class), "w worst");
        post.getStats().setExercise(7);
        post.getStats().setGtg(7);
        post.getStats().setMeditate(7);
        postService.savePost(post);
        finishFailAssert(post, "stats - meetings");
    }

    @Test
    public void testFinish_fail_stats_empty_pray() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setWorst", String.class), "w worst");
        post.getStats().setExercise(7);
        post.getStats().setGtg(7);
        post.getStats().setMeditate(7);
        post.getStats().setMeetings(7);
        postService.savePost(post);
        finishFailAssert(post, "stats - pray");
    }

    @Test
    public void testFinish_fail_stats_empty_read() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setWorst", String.class), "w worst");
        post.getStats().setExercise(7);
        post.getStats().setGtg(7);
        post.getStats().setMeditate(7);
        post.getStats().setMeetings(7);
        post.getStats().setPray(7);
        postService.savePost(post);
        finishFailAssert(post, "stats - read");
    }

    @Test
    public void testFinish_fail_stats_empty_sponsor() throws NoSuchMethodException {
        Post post = postService.startPost(user);
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWidwytk", String.class), "widwytk");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setKryptonite", String.class), "kryptonite");
        setStringMethod(post, post.getIntro(), post.getIntro().getClass().getMethod("setWhatAndWhen", String.class), "whatandwhen");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setBest", String.class), "p best");
        setStringMethod(post, post.getPersonal(), post.getPersonal().getClass().getMethod("setWorst", String.class), "p worst");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setBest", String.class), "f best");
        setStringMethod(post, post.getFamily(), post.getFamily().getClass().getMethod("setWorst", String.class), "f worst");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setBest", String.class), "w best");
        setStringMethod(post, post.getWork(), post.getWork().getClass().getMethod("setWorst", String.class), "w worst");
        post.getStats().setExercise(7);
        post.getStats().setGtg(7);
        post.getStats().setMeditate(7);
        post.getStats().setMeetings(7);
        post.getStats().setPray(7);
        post.getStats().setRead(7);
        postService.savePost(post);
        finishFailAssert(post, "stats - sponsor");
    }

    private void finishFailAssert(Post post, String expectedMessage) {
        try {
            post = postService.finishPost(user);
            fail();
        } catch (PostException p) {
            assertEquals("Post for " + user.getEmail() + "is not complete: " + expectedMessage, p.getMessage());
        }
    }

    private void setStringMethod(Post post, Object obj, Method method, String param) {
        try {
            method.invoke(obj, param);
            postService.savePost(post);
        } catch (IllegalAccessException | InvocationTargetException e) {
            fail("Couldn't invoke: " + method.getName() + " on: " + obj.getClass().getName());
        }
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
