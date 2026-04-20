package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String email = oidcUser.getEmail() != null ? oidcUser.getEmail().toLowerCase() : null;
            if (email == null) return null;

            User user = getUserByEmail(email);
            if (user == null) {
                // User has a valid Keycloak account but was not invited to this group.
                // Admin must create the user record via "Invite Member" first.
                return null;
            }

            if (!Boolean.TRUE.equals(user.getActive())) {
                return null;
            }

            // Populate name from OIDC claims if not yet set (first login after invite)
            boolean updated = false;
            if (user.getFirstName() == null && oidcUser.getGivenName() != null) {
                user.setFirstName(oidcUser.getGivenName());
                updated = true;
            }
            if (user.getLastName() == null && oidcUser.getFamilyName() != null) {
                user.setLastName(oidcUser.getFamilyName());
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
            return user;
        }
        return null;
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User inviteUser(String email) {
        String normalizedEmail = email != null ? email.toLowerCase().trim() : "";
        User existing = getUserByEmail(normalizedEmail);
        if (existing != null) {
            return existing;
        }
        User user = new User(null, null, normalizedEmail);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Override
    @Deprecated
    public User getOrCreateUserFromOidc(String email, String firstName, String lastName) {
        // Kept for backward compatibility. New code should use inviteUser() for
        // admin-controlled member creation and getCurrentUser() for login.
        return inviteUser(email);
    }

    @Override
    public void deleteUser(User user) {
        if (user != null) {
            // Delete profile image if it exists
            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                // Note: We need to inject FileStorageService here or pass it as parameter
                // For now, we'll just delete the user from the database
                // The file cleanup can be handled by a scheduled task or manually
            }
            userRepository.delete(user);
        }
    }

    @Override
    public List<User> getAllActiveUsers() {
        return userRepository.findByActiveTrueOrderByFirstName();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByActiveDescFirstNameAsc();
    }

    @Override
    public User deactivateUser(User user) {
        user.setActive(false);
        return userRepository.save(user);
    }

    @Override
    public User reactivateUser(User user) {
        user.setActive(true);
        return userRepository.save(user);
    }
}
