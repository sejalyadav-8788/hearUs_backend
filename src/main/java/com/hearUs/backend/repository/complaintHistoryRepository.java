package com.hearUs.backend.repository;

import com.hearUs.backend.entity.complaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface complaintHistoryRepository extends JpaRepository<complaintHistory, Long> {

    List<complaintHistory> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}