package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

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
            String email = oidcUser.getEmail();
            String firstName = oidcUser.getGivenName();
            String lastName = oidcUser.getFamilyName();
            
            return getOrCreateUserFromOidc(email, firstName, lastName);
        }
        return null;
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User getOrCreateUserFromOidc(String email, String firstName, String lastName) {
        User user = getUserByEmail(email);
        if (user == null) {
            // Create new user from OIDC data
            user = new User(firstName, lastName, email);
            user.setActive(true);
            // Set default values for required fields
            user.setSlackUserId("oidc-" + email);
            user.setSlackUsername(email.split("@")[0]);
            user = userRepository.save(user);
        }
        return user;
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
}
