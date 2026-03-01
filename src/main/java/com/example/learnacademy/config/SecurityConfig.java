package com.example.learnacademy.config;

import com.example.learnacademy.model.User;
import com.example.learnacademy.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class SecurityConfig {

    @Autowired
    private UserRepository userRepo;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})   // enable CORS
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {

                            OAuth2User oAuth2User =
                                    (OAuth2User) authentication.getPrincipal();

                            String email = oAuth2User.getAttribute("email");

                            User user = userRepo.findByEmail(email)
                                    .orElseGet(() -> {
                                        User newUser = new User();
                                        newUser.setEmail(email);
                                        newUser.setPassword("google");
                                        return userRepo.save(newUser);
                                    });

                            response.sendRedirect(
                                    "https://learn-academy-web.vercel.app/dashboard"
                            );
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("https://learn-academy-web.vercel.app")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:3000",
                                "https://learn-academy-web.vercel.app"
                        )
                        .allowedMethods("*")
                        .allowCredentials(true);
            }
        };
    }
}