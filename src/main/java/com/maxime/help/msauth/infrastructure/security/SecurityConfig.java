package com.maxime.help.msauth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stateless JSON API guarded by bearer access tokens — no sessions, no cookies,
 * no CSRF (there is no browser-managed credential for CSRF to protect). Public
 * endpoints are listed as plain string literals rather than shared constants
 * with {@code AuthController}, to avoid introducing a cross-layer dependency
 * between {@code infrastructure} and {@code web} for a handful of paths.
 */
@Configuration
@EnableWebSecurity
@SuppressWarnings("unused")
public class SecurityConfig {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(AccessTokenIssuer accessTokenIssuer) {
        return new JwtAuthenticationFilter(accessTokenIssuer);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth
                        -> auth
                                // sendError() (e.g. the default 403 AccessDeniedHandler) makes the
                                // container re-dispatch to /error, where the JWT filter doesn't run
                                // again: without this, that anonymous dispatch turns a 403 into a 401.
                                .dispatcherTypeMatchers(DispatcherType.ERROR)
                                .permitAll()
                                .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/google",
                                "/api/auth/refresh")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users")
                                .hasRole("ADMIN")
                                .anyRequest()
                                .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        eh
                        -> eh.authenticationEntryPoint(
                                (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .build();
    }
}
