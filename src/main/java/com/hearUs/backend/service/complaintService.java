package com.hearUs.backend.service;

import com.hearUs.backend.dto.complaintRequest;
import com.hearUs.backend.entity.complaint;
import com.hearUs.backend.entity.complaintHistory;
import com.hearUs.backend.entity.user;
import com.hearUs.backend.repository.complaintHistoryRepository;
import com.hearUs.backend.repository.complaintRepository;
import com.hearUs.backend.repository.userRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class complaintService {

    private static final List<String> ALLOWED_STATUS =
            List.of("IN_PROGRESS", "RESOLVED", "CLOSED");

    private final complaintRepository complaintRepository;
    private final userRepository userRepository;
    private final s3Service s3Service;
    private final escalationService escalationService;
    private final complaintHistoryRepository historyRepository;

    public complaintService(
            complaintRepository complaintRepository,
            userRepository userRepository,
            s3Service s3Service,
            escalationService escalationService,
            complaintHistoryRepository historyRepository) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
        this.escalationService = escalationService;
        this.historyRepository = historyRepository;
    }

    public complaint createComplaint(
            complaintRequest request,
            String studentEmail,
            MultipartFile file) {

        user student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        complaint newComplaint = new complaint();

        newComplaint.setTitle(request.getTitle());
        newComplaint.setDescription(request.getDescription());
        newComplaint.setCategory(request.getCategory());
        newComplaint.setSubcategory(request.getSubcategory());
        newComplaint.setPriority(request.getPriority());
        newComplaint.setSensitive(request.isSensitive());
        newComplaint.setDepartment(request.getDepartment());
        newComplaint.setLocation(request.getLocation());

        // Upload attachment to AWS S3
        if (file != null && !file.isEmpty()) {
            try {
                newComplaint.setAttachmentKey(s3Service.uploadFile(file));
            } catch (IOException e) {
                throw new RuntimeException("File upload failed");
            }
        }

        newComplaint.setStatus("OPEN");
        newComplaint.setCreatedAt(LocalDateTime.now());
        newComplaint.setStudentId(student.getId());

        // Fallback deadline: if nobody can be assigned now, the escalation job
        // still picks this complaint up later and tries the next level.
        newComplaint.setDeadline(escalationService.newDeadline());

        // Route through the chain:
        // Coordinator -> HOD -> Dean -> Admin, Warden -> Dean -> Admin (hostel),
        // Dean -> Admin (sensitive)
        if (escalationService.assignAt(newComplaint, 0)) {
            newComplaint.setStatus("ASSIGNED");
        }

        complaint saved = complaintRepository.save(newComplaint);

        writeHistory(saved, studentEmail, null, saved.getStatus(),
                "Complaint submitted at chain level " + saved.getEscalationLevel());

        return saved;
    }

    public List<complaint> getMyComplaints(String studentEmail) {

        user student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<complaint> list = complaintRepository.findByStudentId(student.getId());

        // Let the student see who currently holds each complaint
        for (complaint c : list) {
            if (c.getAssignedAuthorityId() != null) {
                userRepository.findById(c.getAssignedAuthorityId())
                        .ifPresent(u -> c.setAssignedToRole(u.getRole()));
            }
        }
        return list;
    }

    public List<complaint> getAssignedComplaints(String authorityEmail) {

        user authority = userRepository.findByEmail(authorityEmail)
                .orElseThrow(() -> new RuntimeException("Authority not found"));

        return complaintRepository.findByAssignedAuthorityId(authority.getId());
    }

    public complaint updateStatus(
            Long complaintId,
            String status,
            String authorityEmail) {

        String newStatus = status == null ? "" : status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(newStatus)) {
            throw new RuntimeException("Invalid status: " + status);
        }

        user authority = userRepository.findByEmail(authorityEmail)
                .orElseThrow(() -> new RuntimeException("Authority not found"));

        complaint existing = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        if (!authority.getId().equals(existing.getAssignedAuthorityId())) {
            throw new RuntimeException("You are not authorized to update this complaint");
        }

        String oldStatus = existing.getStatus();
        existing.setStatus(newStatus);

        if ("RESOLVED".equals(newStatus)) {
            existing.setResolvedAt(LocalDateTime.now());
        }

        complaint saved = complaintRepository.save(existing);

        writeHistory(saved, authorityEmail, oldStatus, newStatus,
                "Status updated by " + authority.getRole());

        return saved;
    }

    private void writeHistory(complaint c, String actor,
                              String oldStatus, String newStatus, String note) {
        complaintHistory h = new complaintHistory();
        h.setComplaintId(c.getId());
        h.setActor(actor);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus);
        h.setEscalationLevel(c.getEscalationLevel());
        h.setNote(note);
        historyRepository.save(h);
    }

    public List<complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public long getTotalComplaints() {
        return complaintRepository.count();
    }

    public long getOpenComplaints() {
        return complaintRepository.countByStatus("OPEN");
    }

    public long getAssignedComplaintsCount() {
        return complaintRepository.countByStatus("ASSIGNED");
    }

    public long getEscalatedComplaints() {
        return complaintRepository.countByStatus("ESCALATED");
    }

    public long getResolvedComplaints() {
        return complaintRepository.countByStatus("RESOLVED");
    }
}