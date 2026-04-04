package com.example.learnacademy.repository;

import com.example.learnacademy.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query(value = "SELECT * FROM courses ORDER BY created_at DESC", nativeQuery = true)
    List<Course> findAllLatest();

    List<Course> findByUserId(Long userId);

    List<Course> findByUserIdOrderByIdDesc(Long userId);

    Optional<Course> findByIdAndUserId(Long id, Long userId);

    List<Course> findByIdInAndUserId(List<Long> ids, Long userId);

}
