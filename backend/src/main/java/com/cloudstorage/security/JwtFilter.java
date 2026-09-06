package com.cloudstorage.security;

import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;

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
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository users;

    public JwtFilter(
            JwtService jwt,
            UserRepository users
    ) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws ServletException, IOException {

        String authorization =
                req.getHeader("Authorization");

        System.out.println(
                "JWT FILTER: "
                        + req.getMethod()
                        + " "
                        + req.getRequestURI()
                        + " | Authorization present = "
                        + (authorization != null)
        );

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // No JWT
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            chain.doFilter(req, res);
            return;
        }

        String token =
                authorization.substring(7).trim();

        try {

            String email = jwt.extract(token);

            System.out.println(
                    "JWT EMAIL: " + email
            );

            if (email == null || email.isBlank()) {
                chain.doFilter(req, res);
                return;
            }

            User user =
                    users.findByEmail(
                        email.trim().toLowerCase()
                    ).orElse(null);

            if (user == null) {

                System.out.println(
                    "JWT USER NOT FOUND: " + email
                );

                chain.doFilter(req, res);
                return;
            }

            System.out.println(
                "JWT USER FOUND: " + user.getEmail()
            );

            String role = user.getRole();

            if (role == null || role.isBlank()) {
                role = "USER";
            }

            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(
                            new SimpleGrantedAuthority(role)
                        )
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            System.out.println(
                "JWT AUTHENTICATED: "
                    + user.getEmail()
                    + " / "
                    + role
            );

        } catch (Exception e) {

            System.out.println(
                "JWT ERROR: "
                    + e.getClass().getSimpleName()
                    + " - "
                    + e.getMessage()
            );

            SecurityContextHolder
                    .clearContext();
        }

        chain.doFilter(req, res);
    }
}