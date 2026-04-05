package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.TestSection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestSectionRepository extends JpaRepository<TestSection, Long> {

    List<TestSection> findByTestIdOrderBySectionOrderAsc(Long testId);

    @EntityGraph(attributePaths = {"questions", "questions.options"})
    Optional<TestSection> findById(Long id);
}
