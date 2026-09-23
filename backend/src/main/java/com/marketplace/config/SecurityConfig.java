package com.marketplace.config;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.marketplace.auth.JwtService;
import com.marketplace.user.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.config.http.SessionCreationPolicy;
import com.marketplace.auth.JwtAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.marketplace.common.api.ApiError;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
            JwtService jwt, UserRepository users,
            ObjectMapper mapper) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Stateless Authorization headers are not automatically attached by browsers.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/candidates/*/photo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/company-types").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/evaluator/verifications/*/review").hasRole("ADMIN")
                        .requestMatchers("/api/evaluator/verifications", "/api/evaluator/verifications/**").hasAnyRole("EVALUATOR", "ADMIN")
                        .requestMatchers("/api/evaluator/**", "/api/evaluators/**").hasRole("EVALUATOR")
                        .requestMatchers("/api/candidates/me", "/api/candidates/me/**").hasRole("CANDIDATE")
                        .requestMatchers("/api/employers/me", "/api/employers/me/**").hasRole("EMPLOYER")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwt, users, mapper),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            mapper.writeValue(response.getOutputStream(), ApiError.of(
                                    401, "UNAUTHORIZED", "Please sign in to continue.", request.getRequestURI()));
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            mapper.writeValue(response.getOutputStream(), ApiError.of(
                                    403, "FORBIDDEN", "You do not have permission to access this resource.", request.getRequestURI()));
                        }))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        for (String origin : allowedOrigins) {
            if (origin.isBlank()) continue;
            URI uri = URI.create(origin.trim());
            if (origin.contains("*") || uri.getHost() == null
                    || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || !uri.getRawPath().isEmpty()) {
                throw new IllegalArgumentException("CORS origins must be exact HTTP(S) origins without paths or wildcards");
            }
            configuration.addAllowedOrigin(origin.trim());
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Accept", "Content-Type", "Authorization"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
