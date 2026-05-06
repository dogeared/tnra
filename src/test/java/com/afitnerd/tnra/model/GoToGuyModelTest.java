package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoToGuyModelTest {

    @Test
    void goToGuyPairGetIdReturnsSetId() {
        GoToGuyPair pair = new GoToGuyPair();
        pair.setId(42L);
        assertEquals(42L, pair.getId());
    }

    @Test
    void goToGuySetGetIdReturnsSetId() {
        GoToGuySet set = new GoToGuySet();
        set.setId(7L);
        assertEquals(7L, set.getId());
    }

    @Test
    void goToGuyPairCallerCalleeRoundTrip() {
        User caller = new User("Alice", "A", "alice@example.com");
        User callee = new User("Bob", "B", "bob@example.com");
        GoToGuyPair pair = new GoToGuyPair();
        pair.setCaller(caller);
        pair.setCallee(callee);

        assertSame(caller, pair.getCaller());
        assertSame(callee, pair.getCallee());
    }

    @Test
    void goToGuySetStartDateAndPairsRoundTrip() {
        GoToGuySet set = new GoToGuySet();
        java.util.Date now = new java.util.Date();
        set.setStartDate(now);
        GoToGuyPair pair = new GoToGuyPair();
        set.setGoToGuyPairs(List.of(pair));

        assertEquals(now, set.getStartDate());
        assertEquals(1, set.getGoToGuyPairs().size());
    }
}
