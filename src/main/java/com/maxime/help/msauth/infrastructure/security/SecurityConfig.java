package com.maxime.help.msauth.infrastructure.security;

import com.maxime.help.msauth.domain.port.out.AccessTokenIssuer;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JSON API guarded by bearer access tokens — no sessions, no cookies, no CSRF (there is
 * no browser-managed credential for CSRF to protect). Public endpoints are listed as plain string
 * literals rather than shared constants with {@code AuthController}, to avoid introducing a
 * cross-layer dependency between {@code infrastructure} and {@code web} for a handful of paths.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(AccessTokenIssuer accessTokenIssuer) {
        return new JwtAuthenticationFilter(accessTokenIssuer);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                HttpMethod.POST,
                                                "/api/auth/signup",
                                                "/api/auth/login",
                                                "/api/auth/google",
                                                "/api/auth/refresh")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        eh ->
                                eh.authenticationEntryPoint(
                                        (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .build();
    }
}
