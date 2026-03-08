package com.example.learnacademy.repository;

import com.example.learnacademy.model.CourseContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseContentRepository extends JpaRepository<CourseContent, Long> {

    CourseContent findByCourseId(Long courseId);

}