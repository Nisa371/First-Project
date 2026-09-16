package com.marketplace.auth;

import java.io.IOException;
import java.util.List;
import com.marketplace.common.api.ApiError;
import com.marketplace.user.*;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final ObjectMapper mapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.startsWith("Bearer ")) throw new IllegalArgumentException();
                var user = users.findById(jwt.userId(header.substring(7))).orElseThrow(IllegalArgumentException::new);
                if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                    reject(response, request, 403, "ACCOUNT_INACTIVE", "Your account is not active.");
                    return;
                }
                var auth = UsernamePasswordAuthenticationToken.authenticated(user.getId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                reject(response, request, 401, "INVALID_TOKEN", "Your session has expired or is invalid. Please sign in again.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), ApiError.of(status, error, message, request.getRequestURI()));
    }
}
