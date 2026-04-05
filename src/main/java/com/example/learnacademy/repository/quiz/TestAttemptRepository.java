package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.AttemptStatus;
import com.example.learnacademy.model.quiz.TestAttempt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    @EntityGraph(attributePaths = {"test", "test.course", "user"})
    Optional<TestAttempt> findById(Long id);

    Optional<TestAttempt> findByIdAndTestIdAndUserId(Long id, Long testId, Long userId);

    List<TestAttempt> findByTestIdAndStatusOrderByScoreDescStartedAtAsc(Long testId, AttemptStatus status);
}
