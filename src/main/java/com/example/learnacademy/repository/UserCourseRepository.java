package com.example.learnacademy.repository;

import com.example.learnacademy.model.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {

    List<UserCourse> findByUserId(Long userId);

    List<UserCourse> findByCourseIdIn(List<Long> courseIds);

    long countByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);

    void deleteByCourseIdIn(List<Long> courseIds);

    void deleteByUserIdAndCourseId(Long userId, Long courseId);

}
