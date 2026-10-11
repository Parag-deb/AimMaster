package com.aimmaster.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Wires up Spring Security for the whole app. Set up once here (Sprint 2), extended
 * (not redefined) by later sprints if a new public path needs to be added.
 *
 * "@EnableWebSecurity" turns Spring Security's filter chain on for this app.
 * "SecurityFilterChain" is the modern (Spring Security 6) way to configure it - a bean,
 * not a subclass to extend.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /** BCrypt = the standard one-way password hashing algorithm; AuthService uses this to hash/check passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection defends against a browser-cookie-session attack. We use
                // stateless JWTs in an Authorization header instead, so CSRF doesn't apply here.
                .csrf(csrf -> csrf.disable())
                // No HttpSession is created or read - every request must prove itself with its own JWT.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Registering/logging in must be reachable without already having a token.
                        .requestMatchers("/api/auth/**").permitAll()
                        // The H2 web console is a dev-only convenience for inspecting the database.
                        .requestMatchers("/h2-console/**").permitAll()
                        // The WebSocket handshake (Sprint 3) does its own JWT check - see
                        // config.WebSocketAuthInterceptor - so it's left open here.
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/test-client.html").permitAll()
                        // Sprint 7: the real frontend's HTML/CSS/JS files themselves must load
                        // in a plain browser page-navigation, which never carries a JWT (only
                        // our own fetch() calls from within those pages do, via the
                        // Authorization header) - so the pages/assets are public, the API
                        // calls they make are still protected exactly as before.
                        .requestMatchers("/", "/index.html", "/game.html", "/admin.html", "/history.html", "/css/**", "/js/**", "/img/**", "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                // Needed only so the H2 console (which renders itself inside an <iframe>) isn't
                // blocked by the browser's default X-Frame-Options protection, in dev only.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
