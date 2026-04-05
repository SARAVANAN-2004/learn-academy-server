package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.TestEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestEnrollmentRepository extends JpaRepository<TestEnrollment, Long> {

    List<TestEnrollment> findByUserId(Long userId);

    List<TestEnrollment> findByTestIdIn(List<Long> testIds);

    long countByTestId(Long testId);

    Optional<TestEnrollment> findByUserIdAndTestId(Long userId, Long testId);

    void deleteByUserIdAndTestId(Long userId, Long testId);
}
