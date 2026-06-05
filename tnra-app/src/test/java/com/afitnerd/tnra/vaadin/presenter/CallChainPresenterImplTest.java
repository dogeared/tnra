package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.GoToGuySetRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallChainPresenterImplTest {

    @Mock
    private GoToGuySetRepository goToGuySetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    private CallChainPresenterImpl presenter;

    @BeforeEach
    void setUp() {
        presenter = new CallChainPresenterImpl(goToGuySetRepository, userRepository, fileStorageService);
    }

    @Test
    void delegatesFileUrlAndCurrentSetLookups() {
        GoToGuySet expected = new GoToGuySet();
        when(fileStorageService.getFileUrl("avatar.png")).thenReturn("https://cdn/avatar.png");
        when(goToGuySetRepository.findTopByOrderByStartDateDesc()).thenReturn(expected);

        assertEquals("https://cdn/avatar.png", presenter.getFileUrl("avatar.png"));
        assertSame(expected, presenter.getCurrentGoToGuySet());
    }

    @Test
    void returnsAllActiveUsersAsList() {
        User u1 = user(1L);
        User u2 = user(2L);
        when(userRepository.findByActiveTrue()).thenReturn(List.of(u1, u2));

        List<User> users = presenter.getAllActiveUsers();
        assertEquals(2, users.size());
        assertTrue(users.contains(u1));
        assertTrue(users.contains(u2));
    }

    @Test
    void createsNewSetAssociatesPairsAndSavesTwice() {
        GoToGuyPair pair = new GoToGuyPair();
        List<GoToGuyPair> pairs = new ArrayList<>();
        pairs.add(pair);

        when(goToGuySetRepository.save(any(GoToGuySet.class))).thenAnswer(inv -> inv.getArgument(0));

        GoToGuySet saved = presenter.createNewGoToGuySet(pairs);

        assertNotNull(saved.getStartDate());
        assertEquals(1, saved.getGoToGuyPairs().size());
        assertSame(saved, pair.getGoToGuySet());
        verify(goToGuySetRepository, times(2)).save(any(GoToGuySet.class));
    }

    @Test
    void validatePairEnforcesBusinessRules() {
        User a = user(1L);
        User b = user(2L);
        User c = user(3L);

        GoToGuyPair existing = new GoToGuyPair();
        existing.setCaller(a);
        existing.setCallee(b);

        assertFalse(presenter.validatePair(null, b, List.of(existing)));
        assertFalse(presenter.validatePair(a, null, List.of(existing)));
        assertFalse(presenter.validatePair(a, a, List.of(existing)));
        assertFalse(presenter.validatePair(c, b, List.of(existing)));
        assertTrue(presenter.validatePair(c, a, List.of(existing)));
    }

    @Test
    void addAndRemovePairPersistUpdatedSet() {
        when(goToGuySetRepository.save(any(GoToGuySet.class))).thenAnswer(inv -> inv.getArgument(0));

        GoToGuySet set = new GoToGuySet();
        set.setGoToGuyPairs(null);
        GoToGuyPair pair = new GoToGuyPair();

        GoToGuySet updated = presenter.addPairToSet(set, pair);
        assertEquals(1, updated.getGoToGuyPairs().size());
        assertSame(updated, pair.getGoToGuySet());

        GoToGuySet removed = presenter.removePairFromSet(updated, pair);
        assertTrue(removed.getGoToGuyPairs().isEmpty());

        ArgumentCaptor<GoToGuySet> captor = ArgumentCaptor.forClass(GoToGuySet.class);
        verify(goToGuySetRepository, times(2)).save(captor.capture());
        assertEquals(2, captor.getAllValues().size());
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
