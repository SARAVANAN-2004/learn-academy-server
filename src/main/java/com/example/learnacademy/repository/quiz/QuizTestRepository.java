package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.QuizTest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizTestRepository extends JpaRepository<QuizTest, Long> {

    @EntityGraph(attributePaths = {"course", "createdBy", "sections", "sections.questions", "sections.questions.options"})
    Optional<QuizTest> findById(Long id);
}
