package com.app.expense_tracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    // FIXED: Added JwtService field declaration so it can be used inside the OAuth2 success handler
    private final JwtService jwtService;

    // FIXED: Injected JwtService directly into your existing security constructor setup
    public SecurityConfig(
            JwtFilter jwtFilter,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService) {

        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // DISABLE CSRF
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                // DISABLE FORM LOGIN
                .formLogin(form -> form.disable())

                // DISABLE BASIC AUTH
                .httpBasic(httpBasic -> httpBasic.disable())

                // STATELESS SESSION
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // ROUTE AUTHORIZATION
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ENDPOINTS (Includes standard endpoints and OAuth redirection parameters)
                        .requestMatchers("/auth/**", "/oauth2/**", "/login/oauth2/**", "/api/system/**").permitAll()

                        // ADMIN ONLY
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // DELETE ALL EXPENSES -> ADMIN ONLY
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/expenses").hasRole("ADMIN")

                        // DELETE SINGLE EXPENSE -> ADMIN ONLY
                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/expenses/**"
                        ).hasRole("ADMIN")

                        // ALL OTHERS REQUIRE AUTH
                        .anyRequest().authenticated()
                )

                // ADVANCED OAUTH2 LOGIN SUCCESS REDIRECTION ENGINE
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            org.springframework.security.oauth2.core.user.OAuth2User oAuth2User =
                                    (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();

                            // Extract email attribute returned securely from Google
                            String email = oAuth2User.getAttribute("email");

                            // Generate token safely now that jwtService is properly injected!
                            String token = jwtService.generateToken(email, "USER");

                            // Construct query redirect path to pass parameters cleanly down to React
                            String targetUrl = "https://trackwithme.vercel.app/login?token=" + token + "&role=USER";

                            response.sendRedirect(targetUrl);
                        })
                )

                // CUSTOM ERROR HANDLING
                .exceptionHandling(ex -> ex
                        // 401 UNAUTHORIZED
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.setStatus(401);
                                    response.setContentType("application/json");
                                    response.getWriter().write("""
                                            {
                                              "message":"Unauthorized"
                                            }
                                            """);
                                }
                        )
                        // 403 FORBIDDEN
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {
                                    response.setStatus(403);
                                    response.setContentType("application/json");
                                    response.getWriter().write("""
                                            {
                                              "message":"Access Denied"
                                            }
                                            """);
                                }
                        )
                )

                // AUTH PROVIDER
                .authenticationProvider(
                        authenticationProvider()
                )

                // JWT FILTER
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://trackwithme.vercel.app"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
