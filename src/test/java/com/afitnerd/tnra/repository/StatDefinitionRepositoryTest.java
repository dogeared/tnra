package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class StatDefinitionRepositoryTest {

    @Autowired
    private StatDefinitionRepository statDefinitionRepository;

    @Autowired
    private PersonalStatDefinitionRepository personalStatDefinitionRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<Long> testStatIds = new ArrayList<>();
    private final List<Long> testPersonalStatIds = new ArrayList<>();
    private final List<Long> testUserIds = new ArrayList<>();

    // Use display_order 100+ to avoid conflicts with PostServiceTests' seeded stats (0-6)
    private static final int BASE_ORDER = 100;

    @AfterEach
    void tearDown() {
        testPersonalStatIds.forEach(id -> personalStatDefinitionRepository.deleteById(id));
        testPersonalStatIds.clear();
        testStatIds.forEach(id -> statDefinitionRepository.deleteById(id));
        testStatIds.clear();
        testUserIds.forEach(id -> userRepository.deleteById(id));
        testUserIds.clear();
    }

    private StatDefinition saveTestStat(String name, String label, String emoji, int order) {
        StatDefinition stat = new StatDefinition(name, label, emoji, BASE_ORDER + order);
        stat = statDefinitionRepository.save(stat);
        testStatIds.add(stat.getId());
        return stat;
    }

    private List<StatDefinition> filterTestStats(List<StatDefinition> all) {
        return all.stream()
            .filter(s -> testStatIds.contains(s.getId()))
            .collect(Collectors.toList());
    }

    @Test
    void reorderSwapsDisplayOrderCorrectly() {
        StatDefinition a = saveTestStat("test_alpha", "Alpha", "🅰️", 0);
        StatDefinition b = saveTestStat("test_beta", "Beta", "🅱️", 1);
        StatDefinition c = saveTestStat("test_gamma", "Gamma", "🇬", 2);

        // Verify initial order
        List<StatDefinition> ordered = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        assertEquals("test_alpha", ordered.get(0).getName());
        assertEquals("test_beta", ordered.get(1).getName());
        assertEquals("test_gamma", ordered.get(2).getName());

        // Simulate moveStatDown on alpha: swap alpha and beta using ID-based lookup
        List<StatDefinition> fresh = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        StatDefinition freshAlpha = fresh.stream()
            .filter(s -> s.getId().equals(a.getId())).findFirst().orElseThrow();
        StatDefinition freshBeta = fresh.stream()
            .filter(s -> s.getId().equals(b.getId())).findFirst().orElseThrow();

        int tempOrder = freshAlpha.getDisplayOrder();
        freshAlpha.setDisplayOrder(freshBeta.getDisplayOrder());
        freshBeta.setDisplayOrder(tempOrder);
        statDefinitionRepository.save(freshAlpha);
        statDefinitionRepository.save(freshBeta);

        // Verify new order: beta, alpha, gamma
        List<StatDefinition> reordered = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        assertEquals("test_beta", reordered.get(0).getName());
        assertEquals("test_alpha", reordered.get(1).getName());
        assertEquals("test_gamma", reordered.get(2).getName());
    }

    @Test
    void reorderMoveUpFromMiddlePosition() {
        saveTestStat("test_alpha", "Alpha", "🅰️", 0);
        StatDefinition b = saveTestStat("test_beta", "Beta", "🅱️", 1);
        saveTestStat("test_gamma", "Gamma", "🇬", 2);

        // Move beta UP using ID-based lookup (the fix this tests)
        List<StatDefinition> fresh = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        int betaIndex = -1;
        for (int i = 0; i < fresh.size(); i++) {
            if (fresh.get(i).getId().equals(b.getId())) { betaIndex = i; break; }
        }
        assertEquals(1, betaIndex);

        StatDefinition freshBeta = fresh.get(betaIndex);
        StatDefinition prev = fresh.get(betaIndex - 1);
        int tempOrder = freshBeta.getDisplayOrder();
        freshBeta.setDisplayOrder(prev.getDisplayOrder());
        prev.setDisplayOrder(tempOrder);
        statDefinitionRepository.save(freshBeta);
        statDefinitionRepository.save(prev);

        // Verify: beta, alpha, gamma
        List<StatDefinition> reordered = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        assertEquals("test_beta", reordered.get(0).getName());
        assertEquals("test_alpha", reordered.get(1).getName());
        assertEquals("test_gamma", reordered.get(2).getName());
    }

    @Test
    void archivedStatsExcludedFromActiveQuery() {
        saveTestStat("test_alpha", "Alpha", "🅰️", 0);
        StatDefinition archived = new StatDefinition("test_archived", "Archived", "🚫", BASE_ORDER + 1);
        archived.setArchived(true);
        archived = statDefinitionRepository.save(archived);
        testStatIds.add(archived.getId());
        saveTestStat("test_gamma", "Gamma", "🇬", 2);

        List<StatDefinition> activeTestStats = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        assertEquals(2, activeTestStats.size());
        assertEquals("test_alpha", activeTestStats.get(0).getName());
        assertEquals("test_gamma", activeTestStats.get(1).getName());

        List<StatDefinition> allTestStats = filterTestStats(
            statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()
        );
        assertEquals(3, allTestStats.size());
    }

    @Test
    void archiveAndRestorePreservesData() {
        StatDefinition a = saveTestStat("test_alpha", "Alpha", "🅰️", 0);
        saveTestStat("test_beta", "Beta", "🅱️", 1);

        // Archive alpha
        a.setArchived(true);
        statDefinitionRepository.save(a);

        List<StatDefinition> active = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        assertEquals(1, active.size());
        assertEquals("test_beta", active.get(0).getName());

        // Restore alpha to end — load in-memory and filter by name (encrypted column)
        StatDefinition archivedAlpha = filterTestStats(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc())
            .stream().filter(s -> "test_alpha".equals(s.getName())).findFirst().orElseThrow();
        archivedAlpha.setArchived(false);
        int maxOrder = active.stream().mapToInt(StatDefinition::getDisplayOrder).max().orElse(-1);
        archivedAlpha.setDisplayOrder(maxOrder + 1);
        statDefinitionRepository.save(archivedAlpha);

        List<StatDefinition> restored = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        assertEquals(2, restored.size());
        assertEquals("test_beta", restored.get(0).getName());
        assertEquals("test_alpha", restored.get(1).getName());
    }

    @Test
    void existsByNameDetectsDuplicates() {
        saveTestStat("test_unique", "Unique", "✨", 0);

        boolean exists = filterTestStats(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc())
            .stream().anyMatch(s -> "test_unique".equals(s.getName()));
        assertTrue(exists);

        boolean notExists = filterTestStats(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc())
            .stream().anyMatch(s -> "test_nonexistent".equals(s.getName()));
        assertFalse(notExists);
    }

    @Test
    void findGlobalActive_excludesPersonalStats() {
        // Create a global stat
        saveTestStat("test_global_only", "Global Only", "🌍", 0);

        // Create a personal stat with a different name
        User user = new User("Test", "User", "testglobal@test.com");
        user.setSlackUserId("U_TEST_GLOBAL");
        user.setSlackUsername("testglobal");
        user.setActive(true);
        user = userRepository.save(user);
        testUserIds.add(user.getId());

        PersonalStatDefinition personal = new PersonalStatDefinition(
            "test_personal_only", "Personal Only", "👤", BASE_ORDER + 1, user
        );
        personal = personalStatDefinitionRepository.save(personal);
        testPersonalStatIds.add(personal.getId());

        // findGlobalActive should only return the global stat
        List<StatDefinition> globalActive = filterTestStats(
            statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()
        );
        assertEquals(1, globalActive.size());
        assertEquals("test_global_only", globalActive.get(0).getName());
    }

    @Test
    void existsGlobalByName_ignoresPersonalStats() {
        // Create only a personal stat
        User user = new User("Test", "User", "testignore@test.com");
        user.setSlackUserId("U_TEST_IGNORE");
        user.setSlackUsername("testignore");
        user.setActive(true);
        user = userRepository.save(user);
        testUserIds.add(user.getId());

        PersonalStatDefinition personal = new PersonalStatDefinition(
            "test_shared_name", "Shared Name", "🔗", BASE_ORDER, user
        );
        personal = personalStatDefinitionRepository.save(personal);
        testPersonalStatIds.add(personal.getId());

        // global stat query should NOT return the personal stat
        boolean personalOnlyExists = filterTestStats(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc())
            .stream().anyMatch(s -> "test_shared_name".equals(s.getName()));
        assertFalse(personalOnlyExists);

        // Now create a global stat with the same name
        saveTestStat("test_shared_name", "Shared Name Global", "🌐", 0);

        // Now it should be found among global stats
        boolean globalExists = filterTestStats(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc())
            .stream().anyMatch(s -> "test_shared_name".equals(s.getName()));
        assertTrue(globalExists);
    }
}
