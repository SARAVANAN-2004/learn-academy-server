package com.example.learnacademy.repository;

import com.example.learnacademy.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    @Query(value = "SELECT * FROM courses ORDER BY created_at DESC", nativeQuery = true)
    List<Course> findAllLatest();

    List<Course> findByUserId(Integer userId);
}
