package com.college.cms.service;

import com.college.cms.entity.Department;
import com.college.cms.entity.Faculty;
import com.college.cms.repository.DepartmentRepository;
import com.college.cms.repository.FacultyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;

    public FacultyService(FacultyRepository facultyRepository, DepartmentRepository departmentRepository) {
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
    }

    public Page<Faculty> getFilteredFaculty(String keyword, Long departmentId, String designation, int page, int size) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanDesignation = (designation != null && !designation.trim().isEmpty()) ? designation.trim() : null;
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("id").descending());
        return facultyRepository.findFiltered(cleanKeyword, departmentId, cleanDesignation, pageable);
    }

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll(Sort.by("firstName").ascending());
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid faculty ID: " + id));
    }

    public boolean isEmployeeIdDuplicate(String employeeId, Long currentId) {
        Optional<Faculty> existing = facultyRepository.findByEmployeeId(employeeId);
        return existing.isPresent() && !existing.get().getId().equals(currentId);
    }

    public boolean isEmailDuplicate(String email, Long currentId) {
        Optional<Faculty> existing = facultyRepository.findByEmail(email);
        return existing.isPresent() && !existing.get().getId().equals(currentId);
    }

    public String generateSuggestedEmployeeId(Long departmentId) {
        String deptCode = "GEN";
        long seq = facultyRepository.count() + 1;

        if (departmentId != null) {
            Optional<Department> deptOpt = departmentRepository.findById(departmentId);
            if (deptOpt.isPresent()) {
                deptCode = deptOpt.get().getCode().toUpperCase();
                seq = facultyRepository.countByDepartmentId(departmentId) + 1;
            }
        }

        String candidate;
        do {
            candidate = String.format("FAC-%s-%03d", deptCode, seq);
            seq++;
        } while (facultyRepository.findByEmployeeId(candidate).isPresent());

        return candidate;
    }

    public Faculty saveFaculty(Faculty faculty) {
        if (faculty.getStatus() == null || faculty.getStatus().isBlank()) {
            faculty.setStatus("Active");
        }
        if (faculty.getDesignation() == null || faculty.getDesignation().isBlank()) {
            faculty.setDesignation("Assistant Professor");
        }
        if (faculty.getJoiningDate() == null) {
            faculty.setJoiningDate(LocalDate.now());
        }
        return facultyRepository.save(faculty);
    }

    public void deleteFaculty(Long id) {
        facultyRepository.deleteById(id);
    }

    public long countFaculty() {
        return facultyRepository.count();
    }
}
