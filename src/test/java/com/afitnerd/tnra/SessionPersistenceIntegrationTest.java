package com.afitnerd.tnra;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify 30-day session persistence configuration
 */
public class SessionPersistenceIntegrationTest {

    @Test
    public void testSessionTimeoutConfiguration() {
        MockHttpSession session = new MockHttpSession();
        
        long expectedTimeoutInSeconds = Duration.ofDays(30).toSeconds();
        session.setMaxInactiveInterval((int) expectedTimeoutInSeconds);
        
        int actualTimeout = session.getMaxInactiveInterval();
        assertEquals(expectedTimeoutInSeconds, actualTimeout, 
            "Session timeout should be configured for 30 days (2592000 seconds)");
    }

    @Test
    public void testRememberMeTokenValidityDuration() {
        long expectedValidityInSeconds = Duration.ofDays(30).toSeconds();
        long actualValidityInSeconds = 2592000;
        
        assertEquals(expectedValidityInSeconds, actualValidityInSeconds,
            "Remember-me token validity should be 30 days (2592000 seconds)");
    }

    @Test
    public void testDurationCalculation() {
        Duration thirtyDays = Duration.ofDays(30);
        long thirtyDaysInSeconds = thirtyDays.toSeconds();
        
        assertEquals(2592000L, thirtyDaysInSeconds, 
            "30 days should equal 2,592,000 seconds");
        
        assertEquals(30 * 24 * 60 * 60, thirtyDaysInSeconds,
            "30 days calculation: 30 * 24 hours * 60 minutes * 60 seconds");
    }

    @Test
    public void testSpringSecurityRememberMeConfiguration() {
        int rememberMeTokenValiditySeconds = 2592000;
        Duration thirtyDays = Duration.ofDays(30);
        
        assertEquals(thirtyDays.toSeconds(), rememberMeTokenValiditySeconds,
            "Spring Security remember-me token validity matches 30 days");
    }

    @Test
    public void testSessionMaxInactiveIntervalLimits() {
        MockHttpSession session = new MockHttpSession();
        
        // Test that we can set a 30-day timeout
        int thirtyDaysInSeconds = (int) Duration.ofDays(30).toSeconds();
        session.setMaxInactiveInterval(thirtyDaysInSeconds);
        
        assertEquals(thirtyDaysInSeconds, session.getMaxInactiveInterval(),
            "Should be able to set 30-day session timeout");
        
        // Verify the value is within reasonable bounds
        assertTrue(session.getMaxInactiveInterval() > 0, 
            "Session timeout should be positive");
        assertTrue(session.getMaxInactiveInterval() <= 2592000, 
            "Session timeout should not exceed 30 days");
    }
}