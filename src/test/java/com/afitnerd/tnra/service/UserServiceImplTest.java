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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void getCurrentUserReturnsExistingInvitedUser() {
        OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("user@example.com");
        User existing = new User("Test", "User", "user@example.com");
        existing.setActive(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(existing);
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        UserServiceImpl service = new UserServiceImpl(userRepository);

        assertSame(existing, service.getCurrentUser());
    }

    @Test
    void getCurrentUserReturnsNullForDeactivatedUser() {
        OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("deactivated@example.com");
        User deactivated = new User("Test", "User", "deactivated@example.com");
        deactivated.setActive(false);
        when(userRepository.findByEmail("deactivated@example.com")).thenReturn(deactivated);
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        UserServiceImpl service = new UserServiceImpl(userRepository);

        assertNull(service.getCurrentUser());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getCurrentUserReturnsNullForUninvitedUser() {
        OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("stranger@example.com");
        when(userRepository.findByEmail("stranger@example.com")).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        UserServiceImpl service = new UserServiceImpl(userRepository);

        assertNull(service.getCurrentUser());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getCurrentUserPopulatesNameOnFirstLogin() {
        OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("invited@example.com");
        when(oidcUser.getGivenName()).thenReturn("Jane");
        when(oidcUser.getFamilyName()).thenReturn("Doe");
        // Invited user has email but no name yet
        User invited = new User(null, null, "invited@example.com");
        invited.setActive(true);
        when(userRepository.findByEmail("invited@example.com")).thenReturn(invited);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        UserServiceImpl service = new UserServiceImpl(userRepository);
        User result = service.getCurrentUser();

        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(userRepository).save(invited);
    }

    @Test
    void getCurrentUserDoesNotOverwriteExistingName() {
        OidcUser oidcUser = org.mockito.Mockito.mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("user@example.com");
        User existing = new User("Original", "Name", "user@example.com");
        existing.setActive(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(existing);
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        UserServiceImpl service = new UserServiceImpl(userRepository);
        User result = service.getCurrentUser();

        assertEquals("Original", result.getFirstName());
        assertEquals("Name", result.getLastName());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getCurrentUserReturnsNullWithoutOidcAuthentication() {
        SecurityContextHolder.clearContext();
        UserServiceImpl service = new UserServiceImpl(userRepository);

        assertNull(service.getCurrentUser());
    }

    @Test
    void inviteUserCreatesNewUserByEmail() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserServiceImpl service = new UserServiceImpl(userRepository);
        User result = service.inviteUser("new@example.com");

        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertNull(result.getFirstName());
        assertTrue(result.getActive());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void inviteUserReturnsExistingIfAlreadyInvited() {
        User existing = new User("Test", "User", "existing@example.com");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(existing);

        UserServiceImpl service = new UserServiceImpl(userRepository);
        User result = service.inviteUser("existing@example.com");

        assertSame(existing, result);
        verify(userRepository, never()).save(any(User.class));
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

    @Test
    void deactivateUserSetsActiveFalse() {
        User user = new User("Test", "User", "test@example.com");
        user.setActive(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserServiceImpl service = new UserServiceImpl(userRepository);
        User result = service.deactivateUser(user);

        assertEquals(false, result.getActive());
        verify(userRepository).save(user);
    }

    @Test
    void reactivateUserSetsActiveTrue() {
        User user = new User("Test", "User", "test@example.com");
        user.setActive(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserServiceImpl service = new UserServiceImpl(userRepository);
        User result = service.reactivateUser(user);

        assertEquals(true, result.getActive());
        verify(userRepository).save(user);
    }

    @Test
    void getAllUsersDelegatesToRepository() {
        List<User> allUsers = List.of(
            new User("Active", "User", "active@example.com"),
            new User("Inactive", "User", "inactive@example.com")
        );
        when(userRepository.findAllByOrderByActiveDescFirstNameAsc()).thenReturn(allUsers);

        UserServiceImpl service = new UserServiceImpl(userRepository);
        assertEquals(2, service.getAllUsers().size());
        verify(userRepository).findAllByOrderByActiveDescFirstNameAsc();
    }
}
