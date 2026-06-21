package com.afitnerd.tnra.billing.security;

import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.repository.GroupBillingRepository;
import com.afitnerd.tnra.billing.util.HashUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates per-group API callers. A group app sends {@code Authorization: Bearer <token>};
 * we SHA-256 it and look up the group by {@code api_token_hash}. On a match the request is
 * authenticated with the group_slug as the principal, so a caller can only ever touch its own
 * group's data. No token / no match → no authentication is set, and Spring denies the request.
 *
 * The webhook path carries no bearer token, so this filter is a no-op there (it stays permitAll).
 */
@Component
public class GroupTokenAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final GroupBillingRepository groupBillingRepository;

    public GroupTokenAuthFilter(GroupBillingRepository groupBillingRepository) {
        this.groupBillingRepository = groupBillingRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        extractToken(request)
            .map(token -> HashUtil.sha256Hex(token))
            .flatMap(groupBillingRepository::findByApiTokenHash)
            .ifPresent(this::authenticate);
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    private void authenticate(GroupBilling group) {
        var auth = new UsernamePasswordAuthenticationToken(
            group.getGroupSlug(), null, List.of(new SimpleGrantedAuthority("ROLE_GROUP")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
