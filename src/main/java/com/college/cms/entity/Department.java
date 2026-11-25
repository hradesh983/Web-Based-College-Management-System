package com.college.cms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Department code is required")
    @Size(max = 10, message = "Code must be at most 10 characters")
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @NotBlank(message = "Department name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "head_of_department", length = 100)
    private String headOfDepartment;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Department() {
    }

    public Department(String code, String name, String headOfDepartment, String description) {
        this.code = code;
        this.name = name;
        this.headOfDepartment = headOfDepartment;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeadOfDepartment() {
        return headOfDepartment;
    }

    public void setHeadOfDepartment(String headOfDepartment) {
        this.headOfDepartment = headOfDepartment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
