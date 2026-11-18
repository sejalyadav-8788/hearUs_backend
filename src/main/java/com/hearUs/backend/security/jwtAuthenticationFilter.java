package com.hearUs.backend.security;

import com.hearUs.backend.entity.user;
import com.hearUs.backend.repository.userRepository;

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

@Component
public class jwtAuthenticationFilter extends OncePerRequestFilter {

    private final jwtService jwtService;
    private final userRepository userRepository;

    public jwtAuthenticationFilter(jwtService jwtService, userRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    // Only login and signup are public. /api/auth/me needs the token to be read.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login") || path.equals("/api/auth/signup");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);
            user existingUser = userRepository.findByEmail(email).orElse(null);

            if (existingUser != null && existingUser.getRole() != null) {
                String role = existingUser.getRole().trim().toUpperCase();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                existingUser.getEmail(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Invalid or expired token: continue unauthenticated, the config returns 401
            System.out.println("JWT rejected: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}