package com.example.learnacademy.repository;

import com.example.learnacademy.model.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCourseRepository extends JpaRepository<UserCourse, Integer> {

    List<UserCourse> findByUserId(Integer userId);

}
