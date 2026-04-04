package com.example.learnacademy.service;
import com.example.learnacademy.model.User;
import com.example.learnacademy.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
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

    public Map<String, Object> getPersonalDetails(Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildUserProfileResponse(user);
    }

    public Map<String, Object> getPersonalDetails(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return buildUserProfileResponse(user);
    }

    public Map<String, Object> updatePersonalDetails(Long userId, Map<String, String> body) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        applyPersonalDetails(user, body);

        userRepo.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Personal details updated successfully");
        result.put("user", buildUserProfileResponse(user));
        return result;
    }

    public Map<String, Object> updatePersonalDetails(String email, Map<String, String> body) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        applyPersonalDetails(user, body);

        userRepo.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Personal details updated successfully");
        result.put("user", buildUserProfileResponse(user));
        return result;
    }

    private void applyPersonalDetails(User user, Map<String, String> body) {

        user.setFirstName(body.get("firstName"));
        user.setLastName(body.get("lastName"));
        user.setPhoneNumber(body.get("phoneNumber"));
        user.setDateOfBirth(body.get("dateOfBirth"));
        user.setGender(body.get("gender"));
        user.setProfileImageUrl(body.get("profileImageUrl"));
        user.setBio(body.get("bio"));
        user.setAddressLine1(body.get("addressLine1"));
        user.setAddressLine2(body.get("addressLine2"));
        user.setCity(body.get("city"));
        user.setState(body.get("state"));
        user.setCountry(body.get("country"));
        user.setPostalCode(body.get("postalCode"));
    }

    private Map<String, Object> buildUserProfileResponse(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("firstName", user.getFirstName());
        result.put("lastName", user.getLastName());
        result.put("phoneNumber", user.getPhoneNumber());
        result.put("dateOfBirth", user.getDateOfBirth());
        result.put("gender", user.getGender());
        result.put("profileImageUrl", user.getProfileImageUrl());
        result.put("bio", user.getBio());
        result.put("addressLine1", user.getAddressLine1());
        result.put("addressLine2", user.getAddressLine2());
        result.put("city", user.getCity());
        result.put("state", user.getState());
        result.put("country", user.getCountry());
        result.put("postalCode", user.getPostalCode());
        return result;
    }
}
