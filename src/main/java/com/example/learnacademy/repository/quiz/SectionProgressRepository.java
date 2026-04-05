package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.SectionProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionProgressRepository extends JpaRepository<SectionProgress, Long> {

    List<SectionProgress> findByAttemptId(Long attemptId);

    Optional<SectionProgress> findByAttemptIdAndSectionId(Long attemptId, Long sectionId);
}
