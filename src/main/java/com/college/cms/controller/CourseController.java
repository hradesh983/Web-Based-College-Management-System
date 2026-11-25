package com.college.cms.controller;

import com.college.cms.entity.Course;
import com.college.cms.service.CourseService;
import com.college.cms.service.DepartmentService;
import com.college.cms.service.FacultyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final DepartmentService departmentService;
    private final FacultyService facultyService;

    public CourseController(CourseService courseService,
                            DepartmentService departmentService,
                            FacultyService facultyService) {
        this.courseService = courseService;
        this.departmentService = departmentService;
        this.facultyService = facultyService;
    }

    @GetMapping
    public String listCourses(@RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "departmentId", required = false) Long departmentId,
                              @RequestParam(value = "semester", required = false) Integer semester,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              Model model) {
        populateListModel(model, keyword, departmentId, semester, page);
        if (!model.containsAttribute("course")) {
            model.addAttribute("course", new Course());
        }
        return "courses/list";
    }

    @PostMapping("/save")
    public String saveCourse(@Valid @ModelAttribute("course") Course course,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (course.getCourseCode() != null &&
                courseService.isCourseCodeDuplicate(course.getCourseCode(), course.getId())) {
            result.rejectValue("courseCode", "error.course", "Course code already exists");
        }

        if (result.hasErrors()) {
            populateListModel(model, null, null, null, 0);
            model.addAttribute("course", course);
            model.addAttribute("showCourseModal", true);
            model.addAttribute("isEdit", course.getId() != null);
            return "courses/list";
        }

        boolean isNew = (course.getId() == null);
        courseService.saveCourse(course);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                isNew ? "Course added successfully!" : "Course updated successfully!"
        );
        return "redirect:/courses";
    }

    @PostMapping("/delete/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.deleteCourse(id);
        redirectAttributes.addFlashAttribute("successMessage", "Course deleted successfully!");
        return "redirect:/courses";
    }

    @GetMapping("/api/next-code")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getNextCourseCode(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "semester", required = false) Integer semester) {
        String code = courseService.generateSuggestedCourseCode(departmentId, semester);
        return ResponseEntity.ok(Collections.singletonMap("courseCode", code));
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourseDetails(@PathVariable Long id) {
        Course c = courseService.getCourseById(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", c.getId());
        data.put("courseCode", c.getCourseCode());
        data.put("courseName", c.getCourseName());
        data.put("credits", c.getCredits());
        data.put("semester", c.getSemester());
        data.put("type", c.getType() != null ? c.getType() : "Theory");
        data.put("status", c.getStatus() != null ? c.getStatus() : "Active");
        data.put("description", c.getDescription() != null && !c.getDescription().isBlank() ? c.getDescription() : "-");
        data.put("departmentId", c.getDepartment() != null ? c.getDepartment().getId() : null);
        data.put("departmentName", c.getDepartment() != null ? c.getDepartment().getCode() + " - " + c.getDepartment().getName() : "N/A");
        data.put("facultyId", c.getFaculty() != null ? c.getFaculty().getId() : null);
        data.put("facultyName", c.getFaculty() != null ? c.getFaculty().getFullName() : "Not Assigned");
        data.put("facultyDesignation", c.getFaculty() != null ? c.getFaculty().getDesignation() : "");
        return ResponseEntity.ok(data);
    }

    private void populateListModel(Model model, String keyword, Long departmentId, Integer semester, int page) {
        Page<Course> coursePage = courseService.getFilteredCourses(keyword, departmentId, semester, page, 10);
        model.addAttribute("activePage", "courses");
        model.addAttribute("coursePage", coursePage);
        model.addAttribute("courseList", coursePage.getContent());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("facultyList", facultyService.getAllFaculty());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("currentPage", coursePage.getNumber());
        model.addAttribute("totalPages", coursePage.getTotalPages());
    }
}
