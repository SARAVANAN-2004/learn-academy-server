package com.example.learnacademy.controller;

import com.example.learnacademy.model.User;
import com.example.learnacademy.repository.UserRepository;
import com.example.learnacademy.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService service;

    @Autowired
    private UserRepository userRepo;

    @PostMapping("/signup")
    public Map<String,Object> signUp(@RequestBody Map<String,String> body){
        return service.signUp(body.get("email"), body.get("password"));
    }

    @PostMapping("/signin")
    public Map<String,Object> signIn(
            @RequestBody Map<String,String> body,
            HttpServletRequest request) {

        Map<String,Object> result =
                service.signIn(body.get("email"), body.get("password"));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        body.get("email"),
                        null,
                        null
                );

        SecurityContextHolder.getContext().setAuthentication(auth);

        // VERY IMPORTANT PART
        request.getSession(true)
                .setAttribute("SPRING_SECURITY_CONTEXT",
                        SecurityContextHolder.getContext());

        return result;
    }

    @GetMapping("/me")
    public User currentUser(Authentication authentication) {

        if (authentication == null) {
            return null;
        }

        String email;

        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        System.out.println("Authenticated email: " + email);

        return userRepo.findByEmail(email).orElse(null);
    }
}