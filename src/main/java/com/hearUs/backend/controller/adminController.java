package com.hearUs.backend.controller;

import com.hearUs.backend.entity.complaint;
import com.hearUs.backend.entity.complaintHistory;
import com.hearUs.backend.entity.user;
import com.hearUs.backend.repository.complaintHistoryRepository;
import com.hearUs.backend.repository.complaintRepository;
import com.hearUs.backend.repository.userRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@PreAuthorize("hasRole('ADMIN')")
public class adminController {

    private final complaintRepository complaintRepository;
    private final userRepository userRepository;
    private final complaintHistoryRepository historyRepository;

    public adminController(
            complaintRepository complaintRepository,
            userRepository userRepository,
            complaintHistoryRepository historyRepository) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    // Every complaint on campus, with who submitted it and who holds it now
    @GetMapping("/complaints")
    public List<Map<String, Object>> allComplaints() {

        Map<Long, user> people = new HashMap<>();
        for (user u : userRepository.findAll()) {
            people.put(u.getId(), u);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (complaint c : complaintRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt"))) {

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("description", c.getDescription());
            m.put("category", c.getCategory());
            m.put("subcategory", c.getSubcategory());
            m.put("priority", c.getPriority());
            m.put("sensitive", c.isSensitive());
            m.put("status", c.getStatus());
            m.put("department", c.getDepartment());
            m.put("location", c.getLocation());
            m.put("createdAt", c.getCreatedAt());
            m.put("deadline", c.getDeadline());
            m.put("resolvedAt", c.getResolvedAt());
            m.put("escalationLevel", c.getEscalationLevel());

            // Student identity stays hidden for sensitive complaints
            user student = people.get(c.getStudentId());
            m.put("submittedBy", c.isSensitive()
                    ? "Hidden (sensitive)"
                    : (student != null ? student.getName() : null));

            user handler = people.get(c.getAssignedAuthorityId());
            m.put("handledBy", handler != null ? handler.getName() : null);
            m.put("handledByRole", handler != null ? handler.getRole() : null);

            result.add(m);
        }
        return result;
    }

    // Timeline of one complaint: submission, status changes, escalations
    @GetMapping("/complaints/{id}/history")
    public List<complaintHistory> history(@PathVariable Long id) {
        return historyRepository.findByComplaintIdOrderByCreatedAtAsc(id);
    }
}
