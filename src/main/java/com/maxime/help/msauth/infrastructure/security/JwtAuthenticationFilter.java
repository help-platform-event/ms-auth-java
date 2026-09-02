package com.maxime.help.msauth.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Populates the {@link SecurityContextHolder} from a valid {@code Authorization: Bearer} token. */
class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenIssuer accessTokenIssuer;

    JwtAuthenticationFilter(AccessTokenIssuer accessTokenIssuer) {
        this.accessTokenIssuer = accessTokenIssuer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            accessTokenIssuer
                    .parse(header.substring(BEARER_PREFIX.length()))
                    .ifPresent(
                            claims -> {
                                var authorities =
                                        List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
                                var authentication =
                                        new UsernamePasswordAuthenticationToken(claims, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            });
        }
        chain.doFilter(request, response);
    }
}
