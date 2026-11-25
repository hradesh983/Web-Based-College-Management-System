package com.college.cms.service;

import com.college.cms.entity.Course;
import com.college.cms.entity.Department;
import com.college.cms.repository.CourseRepository;
import com.college.cms.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;

    public CourseService(CourseRepository courseRepository, DepartmentRepository departmentRepository) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
    }

    public Page<Course> getFilteredCourses(String keyword, Long departmentId, Integer semester, int page, int size) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("id").descending());
        return courseRepository.findFiltered(cleanKeyword, departmentId, semester, pageable);
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll(Sort.by("courseName").ascending());
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid course ID: " + id));
    }

    public boolean isCourseCodeDuplicate(String courseCode, Long currentId) {
        Optional<Course> existing = courseRepository.findByCourseCode(courseCode);
        return existing.isPresent() && !existing.get().getId().equals(currentId);
    }

    public String generateSuggestedCourseCode(Long departmentId, Integer semester) {
        String deptCode = "GEN";
        int sem = (semester != null) ? semester : 1;
        long seq = courseRepository.count() + 1;

        if (departmentId != null) {
            Optional<Department> deptOpt = departmentRepository.findById(departmentId);
            if (deptOpt.isPresent()) {
                deptCode = deptOpt.get().getCode().toUpperCase();
            }
        }

        String candidate;
        do {
            candidate = String.format("%s-S%d-%03d", deptCode, sem, seq);
            seq++;
        } while (courseRepository.findByCourseCode(candidate).isPresent());

        return candidate;
    }

    public Course saveCourse(Course course) {
        if (course.getStatus() == null || course.getStatus().isBlank()) {
            course.setStatus("Active");
        }
        if (course.getCredits() == null) {
            course.setCredits(3);
        }
        if (course.getSemester() == null) {
            course.setSemester(1);
        }
        if (course.getType() == null || course.getType().isBlank()) {
            course.setType("Theory");
        }
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    public long countCourses() {
        return courseRepository.count();
    }
}
