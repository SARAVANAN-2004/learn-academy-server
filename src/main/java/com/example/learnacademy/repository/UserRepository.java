package com.example.learnacademy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.learnacademy.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

}