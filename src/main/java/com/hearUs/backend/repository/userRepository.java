package com.hearUs.backend.repository;

import com.hearUs.backend.entity.user;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface userRepository extends JpaRepository<user, Long> {

    Optional<user> findByEmail(String email);

    List<user> findByRole(String role);

    List<user> findByDepartment(String department);

    Optional<user> findFirstByRoleIgnoreCaseAndDepartmentIgnoreCase(String role, String department);
}