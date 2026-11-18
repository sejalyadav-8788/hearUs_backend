package com.hearUs.backend.controller;

import com.hearUs.backend.entity.department;
import com.hearUs.backend.repository.departmentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin("*")
public class departmentController {

    private final departmentRepository departmentRepository;

    public departmentController(departmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public List<String> list() {
        return departmentRepository.findAll().stream()
                .map(department::getName)
                .sorted()
                .toList();
    }
}