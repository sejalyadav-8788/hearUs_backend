package com.hearUs.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    public department() {}
    public department(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}