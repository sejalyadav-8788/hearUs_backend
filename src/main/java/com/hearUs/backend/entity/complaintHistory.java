package com.hearUs.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_history")
public class complaintHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long complaintId;
    private String actor;
    private String oldStatus;
    private String newStatus;
    private int escalationLevel;
    private String note;
    private LocalDateTime createdAt = LocalDateTime.now();

    public complaintHistory() {}

    public Long getId() { return id; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}