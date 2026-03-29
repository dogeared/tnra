package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PersonalStatDefinitionRepositoryTest {

    @Autowired
    private PersonalStatDefinitionRepository personalStatDefinitionRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<Long> testStatIds = new ArrayList<>();
    private final List<Long> testUserIds = new ArrayList<>();

    private static final int BASE_ORDER = 200;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = new User("Alice", "Smith", "alice@test.com");
        userA.setSlackUserId("U_ALICE");
        userA.setSlackUsername("alice");
        userA.setActive(true);
        userA = userRepository.save(userA);
        testUserIds.add(userA.getId());

        userB = new User("Bob", "Jones", "bob@test.com");
        userB.setSlackUserId("U_BOB");
        userB.setSlackUsername("bob");
        userB.setActive(true);
        userB = userRepository.save(userB);
        testUserIds.add(userB.getId());
    }

    @AfterEach
    void tearDown() {
        testStatIds.forEach(id -> personalStatDefinitionRepository.deleteById(id));
        testStatIds.clear();
        testUserIds.forEach(id -> userRepository.deleteById(id));
        testUserIds.clear();
    }

    private PersonalStatDefinition saveTestStat(String name, String label, String emoji, int order, User user) {
        PersonalStatDefinition stat = new PersonalStatDefinition(name, label, emoji, BASE_ORDER + order, user);
        stat = personalStatDefinitionRepository.save(stat);
        testStatIds.add(stat.getId());
        return stat;
    }

    @Test
    void findByUserAndArchivedFalse_returnsOnlyActivePersonalStatsForUser() {
        // Create active stats for userA
        saveTestStat("test_run", "Running", "🏃", 0, userA);
        saveTestStat("test_read", "Reading", "📖", 1, userA);

        // Create an archived stat for userA
        PersonalStatDefinition archived = saveTestStat("test_archived", "Archived", "🚫", 2, userA);
        archived.setArchived(true);
        personalStatDefinitionRepository.save(archived);

        // Create active stat for userB
        saveTestStat("test_swim", "Swimming", "🏊", 0, userB);

        // Query for userA's active stats
        List<PersonalStatDefinition> userAStats =
            personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(userA);

        assertEquals(2, userAStats.size());
        assertEquals("test_run", userAStats.get(0).getName());
        assertEquals("test_read", userAStats.get(1).getName());

        // Query for userB's active stats
        List<PersonalStatDefinition> userBStats =
            personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(userB);

        assertEquals(1, userBStats.size());
        assertEquals("test_swim", userBStats.get(0).getName());
    }

    @Test
    void existsByNameAndUser_returnsTrueForExistingName() {
        saveTestStat("test_yoga", "Yoga", "🧘", 0, userA);

        assertTrue(personalStatDefinitionRepository.existsByNameAndUser("test_yoga", userA));
    }

    @Test
    void existsByNameAndUser_returnsFalseForDifferentUser() {
        saveTestStat("test_yoga", "Yoga", "🧘", 0, userA);

        // Same name but different user should not collide
        assertFalse(personalStatDefinitionRepository.existsByNameAndUser("test_yoga", userB));
    }

    @Test
    void existsByNameAndArchivedFalse_detectsActivePersonalStatAcrossUsers() {
        saveTestStat("test_meditation", "Meditation", "🧘", 0, userA);

        // Should detect active stat regardless of which user owns it
        assertTrue(personalStatDefinitionRepository.existsByNameAndArchivedFalse("test_meditation"));
        assertFalse(personalStatDefinitionRepository.existsByNameAndArchivedFalse("test_nonexistent"));

        // Archive the stat and verify it's no longer detected
        List<PersonalStatDefinition> stats =
            personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(userA);
        PersonalStatDefinition meditation = stats.stream()
            .filter(s -> s.getName().equals("test_meditation")).findFirst().orElseThrow();
        meditation.setArchived(true);
        personalStatDefinitionRepository.save(meditation);

        assertFalse(personalStatDefinitionRepository.existsByNameAndArchivedFalse("test_meditation"));
    }
}
