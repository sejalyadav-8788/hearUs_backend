package com.hearUs.backend.repository;

import com.hearUs.backend.entity.department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface departmentRepository extends JpaRepository<department, Long> {

    Optional<department> findByNameIgnoreCase(String name);
}