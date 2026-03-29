package com.example.learnacademy.repository;

import com.example.learnacademy.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndCourseIdAndLessonId(
            Long userId, Long courseId, String lessonId);

    List<LessonProgress> findByUserIdAndCourseId(Long userId, Long courseId);
}