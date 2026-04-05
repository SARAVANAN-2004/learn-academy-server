package com.example.learnacademy.repository.quiz;

import com.example.learnacademy.model.quiz.LeaderboardEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<LeaderboardEntry> findByTestIdOrderByScoreDescRankAsc(Long testId);

    Optional<LeaderboardEntry> findByTestIdAndUserId(Long testId, Long userId);
}
