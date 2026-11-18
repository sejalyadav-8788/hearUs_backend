package com.hearUs.backend.repository;

import com.hearUs.backend.entity.complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface complaintRepository extends JpaRepository<complaint, Long> {

    List<complaint> findByStudentId(Long studentId);

    List<complaint> findByAssignedAuthorityId(Long authorityId);

    List<complaint> findByDepartment(String department);

    long countByStatus(String status);

    long countByStatusIn(List<String> statuses);

    List<complaint> findByDeadlineBeforeAndStatusNotIn(LocalDateTime now, List<String> statuses);
}