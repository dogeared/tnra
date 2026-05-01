package com.afitnerd.tnra.landing.service;

import com.afitnerd.tnra.landing.model.RequestAccess;
import com.afitnerd.tnra.landing.repository.RequestAccessRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RequestAccessServiceImpl implements RequestAccessService {

    static final int MAX_REQUESTS_PER_HOUR = 5;

    private final RequestAccessRepository repository;
    private final MailgunNotificationService mailgunService;

    public RequestAccessServiceImpl(
        RequestAccessRepository repository,
        MailgunNotificationService mailgunService
    ) {
        this.repository = repository;
        this.mailgunService = mailgunService;
    }

    @Override
    public boolean isRateLimited(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return repository.countByIpAddressSince(ipAddress, oneHourAgo) >= MAX_REQUESTS_PER_HOUR;
    }

    @Override
    public RequestAccess submit(RequestAccess request) {
        request.setSubmittedAt(LocalDateTime.now());
        RequestAccess saved = repository.save(request);
        mailgunService.notifyFounder(saved);
        return saved;
    }
}
