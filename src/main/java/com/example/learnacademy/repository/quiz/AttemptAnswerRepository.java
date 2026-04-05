package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.AttemptAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    @EntityGraph(attributePaths = {"question", "question.section", "selectedOption"})
    List<AttemptAnswer> findByAttemptId(Long attemptId);

    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
