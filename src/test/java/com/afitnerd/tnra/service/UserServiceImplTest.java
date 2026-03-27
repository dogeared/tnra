package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserReturnsExistingOidcBackedUser() {
        OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("user@example.com");
        when(oidcUser.getGivenName()).thenReturn("Test");
        when(oidcUser.getFamilyName()).thenReturn("User");
        User existing = new User("Test", "User", "user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(existing);
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        UserServiceImpl service = new UserServiceImpl(userRepository);

        assertSame(existing, service.getCurrentUser());
    }

    @Test
    void getCurrentUserCreatesActiveUserWhenMissing() {
        OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("new.user@example.com");
        when(oidcUser.getGivenName()).thenReturn("New");
        when(oidcUser.getFamilyName()).thenReturn("User");
        when(userRepository.findByEmail("new.user@example.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        UserServiceImpl service = new UserServiceImpl(userRepository);
        User user = service.getCurrentUser();

        assertNotNull(user);
        assertEquals("new.user@example.com", user.getEmail());
        assertEquals("New", user.getFirstName());
        assertEquals("User", user.getLastName());
        assertNull(user.getSlackUserId());
        assertNull(user.getSlackUsername());
        assertEquals(Boolean.TRUE, user.getActive());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getCurrentUserReturnsNullWithoutOidcAuthentication() {
        SecurityContextHolder.clearContext();
        UserServiceImpl service = new UserServiceImpl(userRepository);

        assertNull(service.getCurrentUser());
    }

    @Test
    void delegatesSaveDeleteAndActiveUserQueries() {
        UserServiceImpl service = new UserServiceImpl(userRepository);
        User user = new User();
        List<User> activeUsers = List.of(new User("One", "User", "one@example.com"));
        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.findByActiveTrueOrderByFirstName()).thenReturn(activeUsers);

        assertSame(user, service.saveUser(user));
        assertSame(activeUsers, service.getAllActiveUsers());
        service.deleteUser(user);
        service.deleteUser(null);

        verify(userRepository).delete(user);
        verify(userRepository, never()).delete(null);
    }
}
