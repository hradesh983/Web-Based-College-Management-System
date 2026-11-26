package com.college.cms.controller;

import com.college.cms.entity.Faculty;
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
@RequestMapping("/faculty")
public class FacultyController {

    private final FacultyService facultyService;
    private final DepartmentService departmentService;

    public FacultyController(FacultyService facultyService, DepartmentService departmentService) {
        this.facultyService = facultyService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listFaculty(@RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "departmentId", required = false) Long departmentId,
                              @RequestParam(value = "designation", required = false) String designation,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              Model model) {
        populateListModel(model, keyword, departmentId, designation, page);
        if (!model.containsAttribute("faculty")) {
            model.addAttribute("faculty", new Faculty());
        }
        return "faculty/list";
    }

    @PostMapping("/save")
    public String saveFaculty(@Valid @ModelAttribute("faculty") Faculty faculty,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (faculty.getEmployeeId() != null &&
                facultyService.isEmployeeIdDuplicate(faculty.getEmployeeId(), faculty.getId())) {
            result.rejectValue("employeeId", "error.faculty", "Employee ID already exists");
        }

        if (faculty.getEmail() != null &&
                facultyService.isEmailDuplicate(faculty.getEmail(), faculty.getId())) {
            result.rejectValue("email", "error.faculty", "Email already exists");
        }

        if (result.hasErrors()) {
            populateListModel(model, null, null, null, 0);
            model.addAttribute("faculty", faculty);
            model.addAttribute("showFacultyModal", true);
            model.addAttribute("isEdit", faculty.getId() != null);
            return "faculty/list";
        }

        boolean isNew = (faculty.getId() == null);
        facultyService.saveFaculty(faculty);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                isNew ? "Faculty member added successfully!" : "Faculty member updated successfully!"
        );
        return "redirect:/faculty";
    }

    @PostMapping("/delete/{id}")
    public String deleteFaculty(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        facultyService.deleteFaculty(id);
        redirectAttributes.addFlashAttribute("successMessage", "Faculty member deleted successfully!");
        return "redirect:/faculty";
    }

    @GetMapping("/api/next-emp-id")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getNextEmployeeId(
            @RequestParam(value = "departmentId", required = false) Long departmentId) {
        String empId = facultyService.generateSuggestedEmployeeId(departmentId);
        return ResponseEntity.ok(Collections.singletonMap("employeeId", empId));
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFacultyDetails(@PathVariable Long id) {
        Faculty f = facultyService.getFacultyById(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", f.getId());
        data.put("employeeId", f.getEmployeeId());
        data.put("firstName", f.getFirstName());
        data.put("lastName", f.getLastName());
        data.put("fullName", f.getFullName());
        data.put("email", f.getEmail());
        data.put("phone", f.getPhone() != null && !f.getPhone().isBlank() ? f.getPhone() : "-");
        data.put("designation", f.getDesignation());
        data.put("qualification", f.getQualification() != null && !f.getQualification().isBlank() ? f.getQualification() : "-");
        data.put("specialization", f.getSpecialization() != null && !f.getSpecialization().isBlank() ? f.getSpecialization() : "-");
        data.put("joiningDate", f.getJoiningDate() != null ? f.getJoiningDate().toString() : "-");
        data.put("status", f.getStatus() != null ? f.getStatus() : "Active");
        data.put("departmentId", f.getDepartment() != null ? f.getDepartment().getId() : null);
        data.put("departmentName", f.getDepartment() != null ? f.getDepartment().getCode() + " - " + f.getDepartment().getName() : "N/A");
        return ResponseEntity.ok(data);
    }

    private void populateListModel(Model model, String keyword, Long departmentId, String designation, int page) {
        Page<Faculty> facultyPage = facultyService.getFilteredFaculty(keyword, departmentId, designation, page, 10);
        model.addAttribute("activePage", "faculty");
        model.addAttribute("facultyPage", facultyPage);
        model.addAttribute("facultyList", facultyPage.getContent());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedDesignation", designation);
        model.addAttribute("currentPage", facultyPage.getNumber());
        model.addAttribute("totalPages", facultyPage.getTotalPages());
    }
}
