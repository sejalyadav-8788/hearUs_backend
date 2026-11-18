package com.hearUs.backend.config;

import com.hearUs.backend.entity.department;
import com.hearUs.backend.entity.user;
import com.hearUs.backend.repository.departmentRepository;
import com.hearUs.backend.repository.userRepository;
import com.hearUs.backend.security.Roles;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final userRepository users;
    private final departmentRepository departments;
    private final PasswordEncoder encoder;

    public DataSeeder(userRepository users, departmentRepository departments,
                      PasswordEncoder encoder) {
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        for (String d : List.of("CSE", "ECE", "ME", "CE", "IT")) {
            if (departments.findByNameIgnoreCase(d).isEmpty()) {
                departments.save(new department(d));
            }
        }

        seed("Student CSE", "student@hearus.test", Roles.STUDENT, "CSE");
        seed("Coordinator CSE", "coord.cse@hearus.test", Roles.COORDINATOR, "CSE");
        seed("HOD CSE", "hod.cse@hearus.test", Roles.HOD, "CSE");
        seed("Warden", "warden@hearus.test", Roles.WARDEN, null);
        seed("Dean", "dean@hearus.test", Roles.DEAN, null);
        seed("Admin", "admin@hearus.test", Roles.ADMIN, null);
    }

    private void seed(String name, String email, String role, String dept) {
        if (users.findByEmail(email).isPresent()) return;
        users.save(new user(name, email, encoder.encode("Test@1234"), role, dept));
    }
}