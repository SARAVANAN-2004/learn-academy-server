package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = {"options"})
    List<Question> findBySectionIdOrderByQuestionOrderAsc(Long sectionId);

    long countBySectionTestId(Long testId);

    Optional<Question> findByIdAndSectionId(Long id, Long sectionId);
}
