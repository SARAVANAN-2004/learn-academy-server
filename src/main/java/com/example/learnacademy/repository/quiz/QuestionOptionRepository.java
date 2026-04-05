package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionId(Long questionId);

    Optional<QuestionOption> findByIdAndQuestionId(Long id, Long questionId);
}
