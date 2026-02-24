package com.demandlane.booklending.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        
                        // User endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        
                        // Book endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/books/**").authenticated()
                        .requestMatchers("/api/v1/books/**").hasRole("ADMIN")
                        
                        // Loan endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/loans/self").hasAnyRole("ADMIN", "MEMBER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/loans/*").hasAnyRole("ADMIN", "MEMBER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/loans/borrow").hasAnyRole("ADMIN", "MEMBER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/loans/return/**").hasAnyRole("ADMIN", "MEMBER")
                        .requestMatchers("/api/v1/loans/**").hasRole("ADMIN")
                        
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
