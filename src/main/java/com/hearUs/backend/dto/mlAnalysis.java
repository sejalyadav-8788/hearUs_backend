package com.hearUs.backend.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * What the HearUs ML service said about one complaint.
 * Filled by mlService from the JSON returned by POST /analyze.
 */
public class mlAnalysis {

    // ML labels: Academics, Administration, Harassment, Hostel, Infrastructure
    private String category;
    private double categoryConfidence;

    // ML labels: Low, Medium, High, Critical
    private String urgency;

    // Safety keyword rules that fired: ragging, sexual, violence, distress, caste, emergency
    private List<String> safetyHits = new ArrayList<>();

    // Committees the ML service says must be told (ICC, Anti-Ragging Committee, ...)
    private List<String> notify = new ArrayList<>();

    private boolean profane;
    private boolean needsReview;
    private List<String> reviewReasons = new ArrayList<>();

    // IDs of similar open complaints (these are backend complaint IDs)
    private List<String> duplicateIds = new ArrayList<>();

    public mlAnalysis() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getCategoryConfidence() {
        return categoryConfidence;
    }

    public void setCategoryConfidence(double categoryConfidence) {
        this.categoryConfidence = categoryConfidence;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public List<String> getSafetyHits() {
        return safetyHits;
    }

    public void setSafetyHits(List<String> safetyHits) {
        this.safetyHits = safetyHits;
    }

    public List<String> getNotify() {
        return notify;
    }

    public void setNotify(List<String> notify) {
        this.notify = notify;
    }

    public boolean isProfane() {
        return profane;
    }

    public void setProfane(boolean profane) {
        this.profane = profane;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
    }

    public List<String> getReviewReasons() {
        return reviewReasons;
    }

    public void setReviewReasons(List<String> reviewReasons) {
        this.reviewReasons = reviewReasons;
    }

    public List<String> getDuplicateIds() {
        return duplicateIds;
    }

    public void setDuplicateIds(List<String> duplicateIds) {
        this.duplicateIds = duplicateIds;
    }
}
