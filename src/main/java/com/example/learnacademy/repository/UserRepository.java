package com.example.learnacademy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.learnacademy.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
