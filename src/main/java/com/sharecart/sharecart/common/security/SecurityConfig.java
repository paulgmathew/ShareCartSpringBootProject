package com.sharecart.sharecart.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // disable CSRF — not needed for stateless JWT APIs
                .csrf(AbstractHttpConfigurer::disable)
                // stateless sessions — JWT replaces session cookies
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // auth endpoints open to everyone
                        .requestMatchers("/api/v1/auth/**").permitAll()
                    // allow WebSocket handshake endpoint
                    .requestMatchers("/ws/**").permitAll()
                        // invite preview is public (no auth needed to peek at an invite)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/invites/*").permitAll()
                        // all other endpoints require a valid JWT
                        .anyRequest().authenticated()
                )
                // run JWT filter before Spring Security's default username/password filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
