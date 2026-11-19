package com.hearUs.backend.service;

import com.hearUs.backend.dto.complaintRequest;
import com.hearUs.backend.dto.mlAnalysis;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class complaintService {

    private static final List<String> ALLOWED_STATUS =
            List.of("IN_PROGRESS", "RESOLVED", "CLOSED");

    // ML safety rules that mean "a person is at risk": route as sensitive (Dean first)
    private static final Set<String> SENSITIVE_HITS =
            Set.of("ragging", "sexual", "violence", "distress", "caste");

    // Only trust the ML category above this confidence
    private static final double ML_CONFIDENT = 0.60;

    // Backend priorities, lowest first
    private static final List<String> PRIORITY_ORDER =
            List.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private static final Map<String, String> ML_URGENCY_TO_PRIORITY = Map.of(
            "Low", "LOW", "Medium", "MEDIUM", "High", "HIGH", "Critical", "URGENT");

    // ML category -> backend category (Administration has no direct match)
    private static final Map<String, String> ML_CATEGORY_TO_BACKEND = Map.of(
            "Academics", "ACADEMICS",
            "Infrastructure", "INFRASTRUCTURE",
            "Hostel", "HOSTEL",
            "Harassment", "SAFETY");

    private final complaintRepository complaintRepository;
    private final userRepository userRepository;
    private final s3Service s3Service;
    private final escalationService escalationService;
    private final complaintHistoryRepository historyRepository;
    private final mlService mlService;

    public complaintService(
            complaintRepository complaintRepository,
            userRepository userRepository,
            s3Service s3Service,
            escalationService escalationService,
            complaintHistoryRepository historyRepository,
            mlService mlService) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
        this.escalationService = escalationService;
        this.historyRepository = historyRepository;
        this.mlService = mlService;
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

        // Second opinion from the ML service. It can raise the priority, mark
        // a safety case as sensitive and fill in an "OTHER" category, but never
        // lowers what the student chose. If the service is down, this is skipped.
        Optional<mlAnalysis> ml =
                mlService.analyze(request.getTitle(), request.getDescription());
        ml.ifPresent(a -> applyMl(newComplaint, a));

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
        newComplaint.setDeadline(escalationService.newDeadline(newComplaint));

        // Route through the chain:
        // Coordinator -> HOD -> Dean -> Admin, Warden -> Dean -> Admin (hostel),
        // Dean -> Admin (sensitive)
        if (escalationService.assignAt(newComplaint, 0)) {
            newComplaint.setStatus("ASSIGNED");
        }

        complaint saved = complaintRepository.save(newComplaint);

        writeHistory(saved, studentEmail, null, saved.getStatus(),
                "Complaint submitted at chain level " + saved.getEscalationLevel());

        ml.ifPresent(a -> {
            try {
                writeHistory(saved, "ML_SERVICE", saved.getStatus(), saved.getStatus(),
                        mlSummary(a));
                // so future complaints about the same issue get linked to this one
                mlService.registerForDuplicates(saved.getId(), saved.getTitle(),
                        saved.getDescription(), a.getCategory());
            } catch (Exception e) {
                // the complaint is already saved; ML extras must never fail the request
                System.err.println("ML post-processing failed for complaint "
                        + saved.getId() + ": " + e.getMessage());
            }
        });

        return saved;
    }

    /** Copies ML results onto the complaint and applies the safety rules. */
    private void applyMl(complaint c, mlAnalysis a) {
        c.setMlCategory(a.getCategory());
        c.setMlConfidence(a.getCategoryConfidence());
        c.setMlUrgency(a.getUrgency());
        c.setMlSafetyFlags(String.join(",", a.getSafetyHits()));
        c.setMlNotify(String.join(", ", a.getNotify()));
        c.setMlProfane(a.isProfane());
        c.setMlNeedsReview(a.isNeedsReview());
        c.setMlReviewReasons(String.join("; ", a.getReviewReasons()));
        c.setMlDuplicateOf(String.join(",", a.getDuplicateIds()));

        // 1. Priority: ML may raise it (e.g. LOW -> URGENT for ragging), never lower it
        String mlPriority = ML_URGENCY_TO_PRIORITY.get(a.getUrgency());
        if (mlPriority != null && rank(mlPriority) > rank(c.getPriority())) {
            c.setPriority(mlPriority);
        }

        // 2. Sensitive: someone may be at risk, so the chain starts at the Dean
        boolean personalSafety = a.getSafetyHits().stream().anyMatch(SENSITIVE_HITS::contains)
                || ("Harassment".equals(a.getCategory())
                    && a.getCategoryConfidence() >= ML_CONFIDENT);
        if (personalSafety) {
            c.setSensitive(true);
        }

        // 3. Category: only fill in when the student picked OTHER (or nothing)
        String current = c.getCategory();
        boolean unspecified = current == null || current.isBlank()
                || "OTHER".equalsIgnoreCase(current);
        String mapped = ML_CATEGORY_TO_BACKEND.get(a.getCategory());
        if (unspecified && mapped != null && a.getCategoryConfidence() >= ML_CONFIDENT) {
            c.setCategory(mapped);
        }
    }

    private static int rank(String priority) {
        return priority == null ? -1 : PRIORITY_ORDER.indexOf(priority.trim().toUpperCase());
    }

    private static String mlSummary(mlAnalysis a) {
        StringBuilder note = new StringBuilder("ML check: category ")
                .append(a.getCategory())
                .append(String.format(" (%.0f%%)", a.getCategoryConfidence() * 100))
                .append(", urgency ").append(a.getUrgency());
        if (!a.getSafetyHits().isEmpty()) {
            note.append(", safety flags ").append(String.join(",", a.getSafetyHits()));
        }
        if (!a.getNotify().isEmpty()) {
            note.append(", notify ").append(String.join(", ", a.getNotify()));
        }
        if (!a.getDuplicateIds().isEmpty()) {
            note.append(", similar to complaint(s) ").append(String.join(",", a.getDuplicateIds()));
        }
        if (a.isNeedsReview()) {
            note.append(", needs review: ").append(String.join("; ", a.getReviewReasons()));
        }
        // complaint_history.note is VARCHAR(255)
        return note.length() > 250 ? note.substring(0, 247) + "..." : note.toString();
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