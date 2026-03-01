package com.example.learnacademy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.learnacademy.model.User;

import java.util.*;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
}
