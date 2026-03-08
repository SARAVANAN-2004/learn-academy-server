package com.example.learnacademy.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "course_type")
    private String courseType;

    private String title;

    @Column(name = "image_url")
    private String imageUrl;

    private String category;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String requirements;

    @Column(name = "target_audience", columnDefinition = "text")
    private String targetAudience;

    @Column(name = "time_commitment")
    private String timeCommitment;

    private String instructor;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    private BigDecimal price;

    private BigDecimal rating;

    // ===== Getters =====

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCourseType() {
        return courseType;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getRequirements() {
        return requirements;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public String getTimeCommitment() {
        return timeCommitment;
    }

    public String getInstructor() {
        return instructor;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getRating() {
        return rating;
    }

    // ===== Setters =====

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public void setTimeCommitment(String timeCommitment) {
        this.timeCommitment = timeCommitment;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }
}