package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@EnableScheduling
public class PQRefreshServiceImpl implements PQRefreshService {

    PQService pqService;

    UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(PQRefreshServiceImpl.class);

    public PQRefreshServiceImpl(PQService pqService, UserRepository userRepository) {
        this.pqService = pqService;
        this.userRepository = userRepository;
    }

    @Override
    public void refreshAuth(User user) {
        String refreshToken = user.getPqRefreshToken();
        if (refreshToken == null) {
            log.info("No refresh token for user with id: {}", user.getId());
            return;
        }
        try {
            PQAuthenticationResponse response = pqService.refresh(refreshToken);
            user.setPqAccessToken(response.getAccessToken());
            user.setPqRefreshToken(response.getRefreshToken());
            userRepository.save(user);
        } catch (IOException e) {
            log.error(
                "Unable to refresh tokens for user id: {}. Message: {}", user.getId(), e.getMessage(), e
            );
        }

    }

    @Override
    @Scheduled(cron = "${pq.refresh.schedule}")
    public void refreshAuthAll() {
        userRepository.findAll().forEach(this::refreshAuth);
    }
}
