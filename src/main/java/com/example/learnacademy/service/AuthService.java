package com.example.learnacademy.service;
import com.example.learnacademy.model.User;
import com.example.learnacademy.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder encoder;

    // SIGN UP
    public Map<String,Object> signUp(String email, String password){

        if(userRepo.findByEmail(email).isPresent())
            throw new RuntimeException("User already exists");

        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(password));

        userRepo.save(u);

        return Map.of("message","Registered successfully");
    }

    // SIGN IN
    public Map<String,Object> signIn(String email, String password){

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // if google user
        if(user.getPassword().equals("google"))
            return Map.of("message","Google user login","userId",user.getId());

        if(!encoder.matches(password,user.getPassword()))
            throw new RuntimeException("Invalid password");

        return Map.of("message","Login successful","userId",user.getId());
    }
}
