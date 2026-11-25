package com.college.cms.controller;

import com.college.cms.service.DepartmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DepartmentService departmentService;

    public DashboardController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("totalStudents", 0L);
        model.addAttribute("totalFaculty", 0L);
        model.addAttribute("totalCourses", 0L);
        model.addAttribute("totalDepartments", departmentService.countDepartments());
        return "dashboard";
    }
}
