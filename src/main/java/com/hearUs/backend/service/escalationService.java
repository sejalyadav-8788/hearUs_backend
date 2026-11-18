package com.hearUs.backend.service;

import com.hearUs.backend.entity.complaint;
import com.hearUs.backend.entity.complaintHistory;
import com.hearUs.backend.entity.user;
import com.hearUs.backend.repository.complaintHistoryRepository;
import com.hearUs.backend.repository.complaintRepository;
import com.hearUs.backend.repository.userRepository;
import com.hearUs.backend.security.EscalationChain;
import com.hearUs.backend.security.Roles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class escalationService {

    private final complaintRepository complaintRepository;
    private final userRepository userRepository;
    private final complaintHistoryRepository historyRepository;
    private final EscalationChain chain;
    private final long escalationMinutes;

    public escalationService(
            complaintRepository complaintRepository,
            userRepository userRepository,
            complaintHistoryRepository historyRepository,
            EscalationChain chain,
            @Value("${escalation.minutes:1440}") long escalationMinutes) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.chain = chain;
        this.escalationMinutes = escalationMinutes;
    }

    public LocalDateTime newDeadline() {
        return LocalDateTime.now().plusMinutes(escalationMinutes);
    }

    @Scheduled(fixedRate = 60000)
    public void checkOverdueComplaints() {
        for (complaint c : complaintRepository.findByDeadlineBeforeAndStatusNotIn(
                LocalDateTime.now(), List.of("RESOLVED", "CLOSED"))) {
            try {
                escalateComplaint(c);
            } catch (Exception e) {
                System.err.println("Escalation failed for complaint "
                        + c.getId() + ": " + e.getMessage());
            }
        }
    }

    private void escalateComplaint(complaint c) {
        String oldStatus = c.getStatus();
        int oldLevel = c.getEscalationLevel();

        if (assignAt(c, oldLevel + 1)) {
            complaintRepository.save(c);

            complaintHistory h = new complaintHistory();
            h.setComplaintId(c.getId());
            h.setActor("SYSTEM");
            h.setOldStatus(oldStatus);
            h.setNewStatus(c.getStatus());
            h.setEscalationLevel(c.getEscalationLevel());
            h.setNote("Deadline missed: escalated from level " + oldLevel
                    + " to " + c.getEscalationLevel());
            historyRepository.save(h);

            System.out.println("Complaint " + c.getId()
                    + " escalated to level " + c.getEscalationLevel());
        }
        // else: the chain is exhausted, so it stays with the last authority
    }

    /**
     * Assigns the complaint at the given level of its chain, skipping levels
     * where nobody holds the role. Returns false if the chain has run out.
     */
    public boolean assignAt(complaint c, int level) {
        Optional<String> role;

        while ((role = chain.roleAt(c, level)).isPresent()) {
            user handler = findHandler(role.get(), c.getDepartment());

            if (handler != null) {
                c.setAssignedAuthorityId(handler.getId());
                c.setEscalationLevel(level);
                c.setStatus(level == 0 ? "ASSIGNED" : "ESCALATED");
                c.setDeadline(newDeadline());
                return true;
            }
            level++;
        }
        return false;
    }

    private user findHandler(String role, String department) {
        boolean departmental = Roles.COORDINATOR.equals(role) || Roles.HOD.equals(role);

        if (departmental) {
            return userRepository
                    .findFirstByRoleIgnoreCaseAndDepartmentIgnoreCase(role, department)
                    .orElse(null);
        }
        return userRepository.findByRole(role).stream().findFirst().orElse(null);
    }
}