package com.college.cms.controller;

import com.college.cms.service.DepartmentService;
import com.college.cms.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DepartmentService departmentService;
    private final StudentService studentService;

    public DashboardController(DepartmentService departmentService, StudentService studentService) {
        this.departmentService = departmentService;
        this.studentService = studentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("totalStudents", studentService.countStudents());
        model.addAttribute("totalFaculty", 0L);
        model.addAttribute("totalCourses", 0L);
        model.addAttribute("totalDepartments", departmentService.countDepartments());
        return "dashboard";
    }
}
