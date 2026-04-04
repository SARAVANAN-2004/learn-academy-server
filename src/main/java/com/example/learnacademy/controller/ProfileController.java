package com.example.learnacademy.controller;

import com.example.learnacademy.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private AuthService authService;

    @GetMapping
    public Map<String, Object> getProfile(Authentication authentication) {
        return authService.getPersonalDetails(getAuthenticatedEmail(authentication));
    }

    @PutMapping
    public Map<String, Object> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        return authService.updatePersonalDetails(getAuthenticatedEmail(authentication), body);
    }

    private String getAuthenticatedEmail(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }

        return authentication.getName();
    }
}
