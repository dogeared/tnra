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
import java.util.Arrays;
import java.util.List;

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
        //user = new User("Micah", "Silverman", "micah@afitnerd.com");
        user = new User("abc123", "afitnerd");
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
                "Can't start new post for afitnerd. Existing post already in progress.",
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
              "afitnerd is in an indeterminate state with 3 posts in progress.",
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
            assertEquals("Expected an in progress post for afitnerd but found none.", p.getMessage());
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
            assertEquals("afitnerd is in an indeterminate state with 3 posts in progress.", p.getMessage());
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
            assertEquals("Expected an in progress post for afitnerd but found none.", p.getMessage());
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
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.GTG, Props.MEDITATE, Props.MEETINGS, Props.PRAY, Props.READ, Props.SPONSOR
        };
        setupPostProps(props, true);
        Post post = postService.finishPost(user);

        assertEquals(PostState.COMPLETE, post.getState());
        assertNotNull(post.getFinish());
    }

    @Test
    public void testFinish_fail_intro_widwytk() {
        Props[] props = {};
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "intro - widwytk");
    }

    @Test
    public void testFinish_fail_intro_empty_widwytk() {
        Props[] props = {};
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "intro - widwytk");
    }

    @Test
    public void testFinish_fail_intro_kryptonite() {
        Props[] props = { Props.INTRO_WIDWYTK };
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "intro - kryptonite");
    }

    @Test
    public void testFinish_fail_intro_empty_kryptonite() {
        Props[] props = { Props.INTRO_WIDWYTK };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "intro - kryptonite");
    }

    @Test
    public void testFinish_fail_intro_whatandwhen() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE
        };
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "intro - what and when");
    }

    @Test
    public void testFinish_fail_intro_empty_whatandwhen() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "intro - what and when");
    }

    @Test
    public void testFinish_fail_personal_best() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN
        };
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "personal - best");
    }

    @Test
    public void testFinish_fail_personal_empty_best() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "personal - best");
    }

    @Test
    public void testFinish_fail_personal_worst() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST
        };
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "personal - worst");
    }

    @Test
    public void testFinish_fail_personal_empty_worst() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "personal - worst");
    }

    @Test
    public void testFinish_fail_family_best() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST
        };
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "family - best");
    }

    @Test
    public void testFinish_fail_family_empty_best() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "family - best");
    }

    @Test
    public void testFinish_fail_family_worst() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST
        };
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "family - worst");
    }

    @Test
    public void testFinish_fail_family_empty_worst() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "family - worst");
    }

    @Test
    public void testFinish_fail_work_best() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST
        };
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "work - best");
    }

    @Test
    public void testFinish_fail_work_empty_best() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "work - best");
    }

    @Test
    public void testFinish_fail_work_worst() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST
        };
        Post post = setupPostProps(props, false);
        finishFailAssert(post, "work - worst");
    }

    @Test
    public void testFinish_fail_work_empty_worst() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "work - worst");
    }

    @Test
    public void testFinish_fail_stats_empty_exercise() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - exercise");
    }

    @Test
    public void testFinish_fail_stats_empty_gtg() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - gtg");
    }

    @Test
    public void testFinish_fail_stats_empty_meditate() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.GTG
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - meditate");
    }

    @Test
    public void testFinish_fail_stats_empty_meetings() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.GTG, Props.MEDITATE
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - meetings");
    }

    @Test
    public void testFinish_fail_stats_empty_pray() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.GTG, Props.MEDITATE, Props.MEETINGS
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - pray");
    }

    @Test
    public void testFinish_fail_stats_empty_read() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.GTG, Props.MEDITATE, Props.MEETINGS, Props.PRAY
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - read");
    }

    @Test
    public void testFinish_fail_stats_empty_sponsor() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.GTG, Props.MEDITATE, Props.MEETINGS, Props.PRAY, Props.READ
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - sponsor");
    }

    private enum Props {
        INTRO_WIDWYTK, INTRO_KRYPTONITE, INTRO_WHAT_AND_WHEN,
        PERSONAL_BEST, PERSONAL_WORST,
        FAMILY_BEST, FAMILY_WORST,
        WORK_BEST, WORK_WORST,
        EXERCISE, GTG, MEDITATE, MEETINGS, PRAY, READ, SPONSOR
    }

    private Post setupPostProps(Props[] propsAry, boolean empties) {
        List<Props> props = Arrays.asList(propsAry);
        Post post = postService.startPost(user);

        String introWid = null;
        if (props.contains(Props.INTRO_WIDWYTK)) {
            post.getIntro().setWidwytk("widwytk");
        } else if (empties) {
            post.getIntro().setWidwytk("");
        }

        if (props.contains(Props.INTRO_KRYPTONITE)) {
            post.getIntro().setKryptonite("kryptonite");
        } else if (empties) {
            post.getIntro().setKryptonite("");
        }

        if (props.contains(Props.INTRO_WHAT_AND_WHEN)) {
            post.getIntro().setWhatAndWhen("whatandwhen");
        } else if (empties) {
            post.getIntro().setWhatAndWhen("");
        }

        if (props.contains(Props.PERSONAL_BEST)) {
            post.getPersonal().setBest("p best");
        } else if (empties) {
            post.getPersonal().setBest("");
        }

        if (props.contains(Props.PERSONAL_WORST)) {
            post.getPersonal().setWorst("p worst");
        } else if (empties) {
            post.getPersonal().setWorst("");
        }

        if (props.contains(Props.FAMILY_BEST)) {
            post.getFamily().setBest("f best");
        } else if (empties) {
            post.getFamily().setBest("");
        }

        if (props.contains(Props.FAMILY_WORST)) {
            post.getFamily().setWorst("f worst");
        } else if (empties) {
            post.getFamily().setWorst("");
        }

        if (props.contains(Props.WORK_BEST)) {
            post.getWork().setBest("w best");
        } else if (empties) {
            post.getWork().setBest("");
        }

        if (props.contains(Props.WORK_WORST)) {
            post.getWork().setWorst("w worst");
        } else if (empties) {
            post.getWork().setWorst("");
        }

        if (props.contains(Props.EXERCISE)) {
            post.getStats().setExercise(7);
        }

        if (props.contains(Props.GTG)) {
            post.getStats().setGtg(7);
        }

        if (props.contains(Props.MEDITATE)) {
            post.getStats().setMeditate(7);
        }

        if (props.contains(Props.MEETINGS)) {
            post.getStats().setMeetings(7);
        }

        if (props.contains(Props.PRAY)) {
            post.getStats().setPray(7);
        }

        if (props.contains(Props.READ)) {
            post.getStats().setRead(7);
        }

        if (props.contains(Props.SPONSOR)) {
            post.getStats().setSponsor(7);
        }

        postService.savePost(post);
        return post;
    }

    private void finishFailAssert(Post post, String expectedMessage) {
        try {
            post = postService.finishPost(user);
            fail();
        } catch (PostException p) {
            assertEquals("Post for " + user.getSlackUserName() + " is not complete: " + expectedMessage, p.getMessage());
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
