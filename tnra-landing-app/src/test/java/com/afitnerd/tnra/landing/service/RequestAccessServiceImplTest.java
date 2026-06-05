package com.afitnerd.tnra.landing.service;

import com.afitnerd.tnra.landing.model.RequestAccess;
import com.afitnerd.tnra.landing.repository.RequestAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestAccessServiceImplTest {

    private RequestAccessRepository repository;
    private MailgunNotificationService mailgunService;
    private RequestAccessServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(RequestAccessRepository.class);
        mailgunService = mock(MailgunNotificationService.class);
        service = new RequestAccessServiceImpl(repository, mailgunService);
    }

    // --- isRateLimited ---

    @Test
    void isRateLimited_returnsFalseWhenBelowLimit() {
        when(repository.countByIpAddressSince(eq("1.2.3.4"), any(LocalDateTime.class)))
            .thenReturn((long) RequestAccessServiceImpl.MAX_REQUESTS_PER_HOUR - 1);

        assertFalse(service.isRateLimited("1.2.3.4"));
    }

    @Test
    void isRateLimited_returnsTrueWhenAtLimit() {
        when(repository.countByIpAddressSince(eq("1.2.3.4"), any(LocalDateTime.class)))
            .thenReturn((long) RequestAccessServiceImpl.MAX_REQUESTS_PER_HOUR);

        assertTrue(service.isRateLimited("1.2.3.4"));
    }

    @Test
    void isRateLimited_returnsTrueWhenAboveLimit() {
        when(repository.countByIpAddressSince(eq("1.2.3.4"), any(LocalDateTime.class)))
            .thenReturn((long) RequestAccessServiceImpl.MAX_REQUESTS_PER_HOUR + 10);

        assertTrue(service.isRateLimited("1.2.3.4"));
    }

    @Test
    void isRateLimited_returnsFalseWhenIpNull() {
        assertFalse(service.isRateLimited(null));
        verifyNoInteractions(repository);
    }

    @Test
    void isRateLimited_returnsFalseWhenIpBlank() {
        assertFalse(service.isRateLimited("   "));
        verifyNoInteractions(repository);
    }

    // --- submit ---

    @Test
    void submit_setsSubmittedAtAndSaves() {
        RequestAccess request = new RequestAccess();
        request.setGroupName("Test Group");
        request.setContactName("Alice");
        request.setEmail("alice@example.com");

        when(repository.save(any(RequestAccess.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestAccess result = service.submit(request);

        assertNotNull(result.getSubmittedAt(), "submittedAt must be set before save");
        verify(repository).save(request);
    }

    @Test
    void submit_notifiesFounderAfterSave() {
        RequestAccess request = new RequestAccess();
        request.setGroupName("Test Group");
        request.setContactName("Bob");
        request.setEmail("bob@example.com");

        RequestAccess saved = new RequestAccess();
        when(repository.save(any())).thenReturn(saved);

        service.submit(request);

        verify(mailgunService).notifyFounder(saved);
    }

    @Test
    void submit_notifiesFounderEvenIfGroupNameIsMinimal() {
        RequestAccess request = new RequestAccess();
        request.setGroupName("G");
        request.setContactName("C");
        request.setEmail("c@example.com");

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.submit(request);

        verify(mailgunService).notifyFounder(any(RequestAccess.class));
    }
}
