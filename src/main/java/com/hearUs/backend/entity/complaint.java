package com.hearUs.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    private String subcategory;

    private String priority;

    private boolean sensitive;

    private String status;

    private String department;

    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime deadline;

    private Long studentId;

    private Long assignedAuthorityId;
    private int escalationLevel;
    private String attachmentKey;

    // NEW: set when the complaint is marked RESOLVED
    private LocalDateTime resolvedAt;

    // NEW: not stored in the database; filled in by complaintService
    @Transient
    private String assignedToRole;

    // ---------- ML service results (all nullable: old rows simply have no ML data) ----------
    private String mlCategory;          // Academics / Administration / Harassment / Hostel / Infrastructure
    private Double mlConfidence;        // 0..1 confidence of mlCategory
    private String mlUrgency;           // Low / Medium / High / Critical
    private String mlSafetyFlags;       // e.g. "ragging,violence"
    private String mlNotify;            // committees to inform, e.g. "Anti-Ragging Committee"
    private Boolean mlProfane;          // abusive words found in the text
    private Boolean mlNeedsReview;      // a human should double-check this complaint
    @Column(columnDefinition = "TEXT")
    private String mlReviewReasons;
    private String mlDuplicateOf;       // IDs of similar open complaints, e.g. "12,15"

    public complaint() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getAssignedAuthorityId() {
        return assignedAuthorityId;
    }

    public void setAssignedAuthorityId(Long assignedAuthorityId) {
        this.assignedAuthorityId = assignedAuthorityId;
    }

    public int getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(int escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public String getAttachmentKey() {
        return attachmentKey;
    }

    public void setAttachmentKey(String attachmentKey) {
        this.attachmentKey = attachmentKey;
    }

    // NEW
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    // NEW
    public String getAssignedToRole() {
        return assignedToRole;
    }

    public void setAssignedToRole(String assignedToRole) {
        this.assignedToRole = assignedToRole;
    }

    // ---------- ML service results ----------
    public String getMlCategory() { return mlCategory; }
    public void setMlCategory(String mlCategory) { this.mlCategory = mlCategory; }

    public Double getMlConfidence() { return mlConfidence; }
    public void setMlConfidence(Double mlConfidence) { this.mlConfidence = mlConfidence; }

    public String getMlUrgency() { return mlUrgency; }
    public void setMlUrgency(String mlUrgency) { this.mlUrgency = mlUrgency; }

    public String getMlSafetyFlags() { return mlSafetyFlags; }
    public void setMlSafetyFlags(String mlSafetyFlags) { this.mlSafetyFlags = mlSafetyFlags; }

    public String getMlNotify() { return mlNotify; }
    public void setMlNotify(String mlNotify) { this.mlNotify = mlNotify; }

    public Boolean getMlProfane() { return mlProfane; }
    public void setMlProfane(Boolean mlProfane) { this.mlProfane = mlProfane; }

    public Boolean getMlNeedsReview() { return mlNeedsReview; }
    public void setMlNeedsReview(Boolean mlNeedsReview) { this.mlNeedsReview = mlNeedsReview; }

    public String getMlReviewReasons() { return mlReviewReasons; }
    public void setMlReviewReasons(String mlReviewReasons) { this.mlReviewReasons = mlReviewReasons; }

    public String getMlDuplicateOf() { return mlDuplicateOf; }
    public void setMlDuplicateOf(String mlDuplicateOf) { this.mlDuplicateOf = mlDuplicateOf; }
}
