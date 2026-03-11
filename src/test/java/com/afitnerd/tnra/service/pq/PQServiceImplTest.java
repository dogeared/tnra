package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.model.pq.PQMeResponse;
import com.afitnerd.tnra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PQServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void refreshAuthClearsTokensWhenRefreshTokenMissing() {
        PQServiceImpl service = new PQServiceImpl(userRepository);
        User user = new User();
        user.setId(1L);
        user.setPqAccessToken("access");

        service.refreshAuth(user);

        assertNull(user.getPqAccessToken());
        assertNull(user.getPqRefreshToken());
        verify(userRepository).save(user);
    }

    @Test
    void refreshAuthStoresNewTokensOnSuccess() throws IOException {
        PQServiceImpl service = spy(new PQServiceImpl(userRepository));
        User user = new User();
        user.setId(2L);
        user.setPqRefreshToken("refresh");
        PQAuthenticationResponse response = new PQAuthenticationResponse();
        response.setAccessToken("new-access");
        response.setRefreshToken("new-refresh");
        doReturn(response).when(service).refresh("refresh");

        service.refreshAuth(user);

        assertEquals("new-access", user.getPqAccessToken());
        assertEquals("new-refresh", user.getPqRefreshToken());
        verify(userRepository).save(user);
    }

    @Test
    void refreshAuthClearsTokensWhenRefreshFails() throws IOException {
        PQServiceImpl service = spy(new PQServiceImpl(userRepository));
        User user = new User();
        user.setId(3L);
        user.setPqAccessToken("old-access");
        user.setPqRefreshToken("old-refresh");
        doThrow(new IOException("boom")).when(service).refresh("old-refresh");

        service.refreshAuth(user);

        assertNull(user.getPqAccessToken());
        assertNull(user.getPqRefreshToken());
        verify(userRepository).save(user);
    }

    @Test
    void pqMetricsAllReturnsNullWhenUserHasNoAccessToken() {
        PQServiceImpl service = new PQServiceImpl(userRepository);
        User user = new User("No", "Token", "no@example.com");
        when(userRepository.findAll()).thenReturn(List.of(user));

        Map<String, PQMeResponse> result = service.pqMetricsAll();

        assertEquals(1, result.size());
        assertNull(result.get("No Token"));
    }

    @Test
    void pqMetricsAllRefreshesAndRetriesOnce() throws IOException {
        PQServiceImpl service = spy(new PQServiceImpl(userRepository));
        User user = new User("Retry", "User", "retry@example.com");
        user.setId(4L);
        user.setPqAccessToken("stale");
        user.setPqRefreshToken("refresh");
        when(userRepository.findAll()).thenReturn(List.of(user));
        doThrow(new IOException("stale")).when(service).metrics("stale");
        doAnswer(invocation -> {
            user.setPqAccessToken("fresh");
            return null;
        }).when(service).refreshAuth(user);
        PQMeResponse metrics = new PQMeResponse();
        PQMeResponse.Pq pq = new PQMeResponse.Pq();
        pq.setCharge(BigDecimal.ONE);
        metrics.setPq(pq);
        doReturn(metrics).when(service).metrics("fresh");

        Map<String, PQMeResponse> result = service.pqMetricsAll();

        assertSame(metrics, result.get("Retry User"));
    }

    @Test
    void pqMetricsAllClearsTokensAfterRetryFailure() throws IOException {
        PQServiceImpl service = spy(new PQServiceImpl(userRepository));
        User user = new User("Fail", "User", "fail@example.com");
        user.setId(5L);
        user.setPqAccessToken("stale");
        user.setPqRefreshToken("refresh");
        when(userRepository.findAll()).thenReturn(List.of(user));
        doThrow(new IOException("still stale")).when(service).metrics("stale");
        doNothing().when(service).refreshAuth(user);

        Map<String, PQMeResponse> result = service.pqMetricsAll();

        assertNull(result.get("Fail User"));
        assertNull(user.getPqAccessToken());
        assertNull(user.getPqRefreshToken());
        verify(userRepository).save(user);
    }
}
