package com.example.learnacademy.repository;

import com.example.learnacademy.model.CourseContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseContentRepository extends JpaRepository<CourseContent, Long> {

    CourseContent findByCourseId(Long courseId);

    Optional<CourseContent> findOptionalByCourseId(Long courseId);

    List<CourseContent> findByCourseIdIn(List<Long> courseIds);

    void deleteByCourseId(Long courseId);

    void deleteByCourseIdIn(List<Long> courseIds);

}
