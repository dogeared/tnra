package com.afitnerd.tnra;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
public class PostServiceTests {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private StatDefinitionRepository statDefinitionRepository;

    User user;

    // Stat definitions seeded in @BeforeEach, keyed by name
    private Map<String, StatDefinition> statDefs;

    @BeforeEach
    public void setup() {
        user = new User("Test", "User", "test@afitnerd.com");
        user = userRepository.save(user);

        // Seed the 7 default stat definitions (matching V3 migration)
        statDefs = new LinkedHashMap<>();
        seedStatDef("exercise", "Exercise", "\uD83D\uDCAA", 0);
        seedStatDef("meditate", "Meditate", "\uD83E\uDDD8", 1);
        seedStatDef("pray",     "Pray",     "\uD83D\uDE4F", 2);
        seedStatDef("read",     "Read",     "\uD83D\uDCDA", 3);
        seedStatDef("gtg",      "GTG",      "\uD83D\uDC65", 4);
        seedStatDef("meetings", "Meetings", "\uD83E\uDD1D", 5);
        seedStatDef("sponsor",  "Sponsor",  "\uD83E\uDD32", 6);
    }

    private void seedStatDef(String name, String label, String emoji, int displayOrder) {
        StatDefinition sd = statDefinitionRepository.findByName(name).orElseGet(() -> {
            StatDefinition newSd = new StatDefinition(name, label, emoji, displayOrder);
            return statDefinitionRepository.save(newSd);
        });
        statDefs.put(name, sd);
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
                "Can't start new post for Test User. Existing post already in progress.",
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
              "Test User is in an indeterminate state with 3 posts in progress.",
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
            assertEquals("Expected an in progress post for Test User but found none.", p.getMessage());
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
            assertEquals("Test User is in an indeterminate state with 3 posts in progress.", p.getMessage());
        }
    }

    @Test
    public void testUpdateStatValue_success_partial() {
        postService.startPost(user);

        postService.updateStatValue(user, statDefs.get("exercise"), 2);
        postService.updateStatValue(user, statDefs.get("gtg"), 3);
        postService.updateStatValue(user, statDefs.get("meditate"), 4);

        Post post2 = postService.getInProgressPost(user);
        assertEquals(2, (int) post2.getStatValue("exercise"));
        assertEquals(3, (int) post2.getStatValue("gtg"));
        assertEquals(4, (int) post2.getStatValue("meditate"));
        assertNull(post2.getStatValue("meetings"));
        assertNull(post2.getStatValue("pray"));
        assertNull(post2.getStatValue("read"));
        assertNull(post2.getStatValue("sponsor"));
    }

    @Test
    public void testUpdateStatValue_success_independent() {
        // Verify multiple stat values can be set independently
        postService.startPost(user);

        postService.updateStatValue(user, statDefs.get("exercise"), 2);
        Post post1 = postService.getInProgressPost(user);
        assertEquals(2, (int) post1.getStatValue("exercise"));
        assertNull(post1.getStatValue("gtg"));

        postService.updateStatValue(user, statDefs.get("gtg"), 3);
        Post post2 = postService.getInProgressPost(user);
        // exercise value is preserved when gtg is set independently
        assertEquals(2, (int) post2.getStatValue("exercise"));
        assertEquals(3, (int) post2.getStatValue("gtg"));

        // all other stats remain null
        assertNull(post2.getStatValue("meditate"));
        assertNull(post2.getStatValue("meetings"));
        assertNull(post2.getStatValue("pray"));
        assertNull(post2.getStatValue("read"));
        assertNull(post2.getStatValue("sponsor"));
    }

    @Test
    public void testUpdateStatValue_success_sequential() {
        postService.startPost(user);

        // Set initial values
        postService.updateStatValue(user, statDefs.get("exercise"), 2);
        postService.updateStatValue(user, statDefs.get("gtg"), 3);
        postService.updateStatValue(user, statDefs.get("meditate"), 4);
        postService.updateStatValue(user, statDefs.get("meetings"), 5);

        // Update some values
        postService.updateStatValue(user, statDefs.get("meetings"), 6);
        postService.updateStatValue(user, statDefs.get("pray"), 7);

        Post post2 = postService.getInProgressPost(user);

        assertEquals(2, (int) post2.getStatValue("exercise"));
        assertEquals(3, (int) post2.getStatValue("gtg"));
        assertEquals(4, (int) post2.getStatValue("meditate"));
        assertEquals(6, (int) post2.getStatValue("meetings"));
        assertEquals(7, (int) post2.getStatValue("pray"));
        assertNull(post2.getStatValue("read"));
        assertNull(post2.getStatValue("sponsor"));
    }

    @Test
    public void testUpdateStatValue_fail_noInProgressPost() {
        try {
            postService.updateStatValue(user, statDefs.get("exercise"), 2);
            fail();
        } catch (PostException p) {
            assertEquals("Expected an in progress post for Test User but found none.", p.getMessage());
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
        confirmNullOtherCategories(post2, CategorType.PERSONAL);
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
        confirmNullOtherCategories(post3, CategorType.PERSONAL);
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
        confirmNullOtherCategories(post2, CategorType.FAMILY);
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
        confirmNullOtherCategories(post3, CategorType.FAMILY);
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
        confirmNullOtherCategories(post2, CategorType.WORK);
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
        confirmNullOtherCategories(post3, CategorType.WORK);
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
    public void testGetLastFinishedPost_success() {
        testFinish_success();
        Post post = postService.getLastFinishedPost(user);
        assertEquals(PostState.COMPLETE, post.getState());
        assertNotNull(post.getFinish());
    }

    @Test
    public void testGetLastFinishedPost_fail() {
        try {
            postService.getLastFinishedPost(user);
            fail();
        } catch (PostException p) {
            assertEquals("Test User has no finished posts.", p.getMessage());
        }
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

    // Stats failure tests follow display_order: exercise(0), meditate(1), pray(2), read(3), gtg(4), meetings(5), sponsor(6)

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
    public void testFinish_fail_stats_empty_meditate() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - meditate");
    }

    @Test
    public void testFinish_fail_stats_empty_pray() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.MEDITATE
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
            Props.EXERCISE, Props.MEDITATE, Props.PRAY
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - read");
    }

    @Test
    public void testFinish_fail_stats_empty_gtg() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.MEDITATE, Props.PRAY, Props.READ
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - gtg");
    }

    @Test
    public void testFinish_fail_stats_empty_meetings() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.MEDITATE, Props.PRAY, Props.READ, Props.GTG
        };
        Post post = setupPostProps(props, true);
        finishFailAssert(post, "stats - meetings");
    }

    @Test
    public void testFinish_fail_stats_empty_sponsor() {
        Props[] props = {
            Props.INTRO_WIDWYTK, Props.INTRO_KRYPTONITE, Props.INTRO_WHAT_AND_WHEN,
            Props.PERSONAL_BEST, Props.PERSONAL_WORST,
            Props.FAMILY_BEST, Props.FAMILY_WORST,
            Props.WORK_BEST, Props.WORK_WORST,
            Props.EXERCISE, Props.MEDITATE, Props.PRAY, Props.READ, Props.GTG, Props.MEETINGS
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

    private enum CategorType {
        PERSONAL, FAMILY, WORK
    }

    private void confirmNullOtherCategories(Post post, CategorType focus) {
        switch (focus) {
            case PERSONAL:
                assertNull(post.getFamily().getBest());
                assertNull(post.getFamily().getWorst());
                assertNull(post.getWork().getBest());
                assertNull(post.getWork().getWorst());
                break;
            case FAMILY:
                assertNull(post.getPersonal().getBest());
                assertNull(post.getPersonal().getWorst());
                assertNull(post.getWork().getBest());
                assertNull(post.getWork().getWorst());
                break;
            case WORK:
                assertNull(post.getPersonal().getBest());
                assertNull(post.getPersonal().getWorst());
                assertNull(post.getFamily().getBest());
                assertNull(post.getFamily().getWorst());
        }
    }

    private Post setupPostProps(Props[] propsAry, boolean empties) {
        List<Props> props = Arrays.asList(propsAry);
        Post post = postService.startPost(user);

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

        // Save text fields first
        postService.savePost(post);

        // Now set stat values via the service (which manages PostStatValue entities)
        if (props.contains(Props.EXERCISE)) {
            postService.updateStatValue(user, statDefs.get("exercise"), 7);
        }

        if (props.contains(Props.MEDITATE)) {
            postService.updateStatValue(user, statDefs.get("meditate"), 7);
        }

        if (props.contains(Props.PRAY)) {
            postService.updateStatValue(user, statDefs.get("pray"), 7);
        }

        if (props.contains(Props.READ)) {
            postService.updateStatValue(user, statDefs.get("read"), 7);
        }

        if (props.contains(Props.GTG)) {
            postService.updateStatValue(user, statDefs.get("gtg"), 7);
        }

        if (props.contains(Props.MEETINGS)) {
            postService.updateStatValue(user, statDefs.get("meetings"), 7);
        }

        if (props.contains(Props.SPONSOR)) {
            postService.updateStatValue(user, statDefs.get("sponsor"), 7);
        }

        return post;
    }

    private void finishFailAssert(Post post, String expectedMessage) {
        try {
            post = postService.finishPost(user);
            fail();
        } catch (PostException p) {
            assertEquals("Post for " + user.getFirstName() + " " + user.getLastName() + " is not complete: " + expectedMessage, p.getMessage());
        }
    }
}
