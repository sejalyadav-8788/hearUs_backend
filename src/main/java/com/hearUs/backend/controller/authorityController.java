package com.hearUs.backend.controller;

import com.hearUs.backend.entity.complaint;
import com.hearUs.backend.service.complaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authority")
@CrossOrigin("*")
public class authorityController {

    private final complaintService complaintService;

    public authorityController(complaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Coordinator, Warden, HOD, Faculty, Dean and Admin see their assigned complaints
    @PreAuthorize("hasAnyRole('COORDINATOR', 'WARDEN', 'HOD', 'FACULTY', 'DEAN', 'ADMIN')")
    @GetMapping("/complaints")
    public ResponseEntity<List<complaint>> getAssignedComplaints(Authentication authentication) {
        return ResponseEntity.ok(
                complaintService.getAssignedComplaints(authentication.getName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-complaints")
    public ResponseEntity<List<complaint>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/total")
    public ResponseEntity<Long> getTotalComplaints() {
        return ResponseEntity.ok(complaintService.getTotalComplaints());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/open")
    public ResponseEntity<Long> getOpenComplaints() {
        return ResponseEntity.ok(complaintService.getOpenComplaints());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/assigned")
    public ResponseEntity<Long> getAssignedComplaintsCount() {
        return ResponseEntity.ok(complaintService.getAssignedComplaintsCount());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/escalated")
    public ResponseEntity<Long> getEscalatedComplaints() {
        return ResponseEntity.ok(complaintService.getEscalatedComplaints());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/resolved")
    public ResponseEntity<Long> getResolvedComplaints() {
        return ResponseEntity.ok(complaintService.getResolvedComplaints());
    }
}