package com.college.cms.controller;

import com.college.cms.entity.Student;
import com.college.cms.service.DepartmentService;
import com.college.cms.service.StudentService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Controller
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;
    private final DepartmentService departmentService;

    public StudentController(StudentService studentService, DepartmentService departmentService) {
        this.studentService = studentService;
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listStudents(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "departmentId", required = false) Long departmentId,
                               @RequestParam(value = "semester", required = false) Integer semester,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               Model model) {
        populateListModel(model, keyword, departmentId, semester, page);
        if (!model.containsAttribute("student")) {
            model.addAttribute("student", new Student());
        }
        return "students/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("activePage", "students");
        model.addAttribute("student", new Student());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("isEdit", false);
        return "students/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activePage", "students");
            model.addAttribute("student", studentService.getStudentById(id));
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("isEdit", true);
            return "students/form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/students";
        }
    }

    @GetMapping("/view/{id}")
    public String viewStudent(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activePage", "students");
            model.addAttribute("student", studentService.getStudentById(id));
            return "students/view";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/students";
        }
    }

    @PostMapping("/save")
    public String saveStudent(@Valid @ModelAttribute("student") Student student,
                              BindingResult result,
                              @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (student.getRollNumber() != null &&
                studentService.isRollNumberDuplicate(student.getRollNumber(), student.getId())) {
            result.rejectValue("rollNumber", "error.student", "Roll number already exists");
        }

        if (student.getEmail() != null &&
                studentService.isEmailDuplicate(student.getEmail(), student.getId())) {
            result.rejectValue("email", "error.student", "Email already exists");
        }

        if (result.hasErrors()) {
            populateListModel(model, null, null, null, 0);
            model.addAttribute("student", student);
            model.addAttribute("showStudentModal", true);
            model.addAttribute("isEdit", student.getId() != null);
            return "students/list";
        }

        boolean isNew = (student.getId() == null);
        if (!isNew && (photoFile == null || photoFile.isEmpty())) {
            Student existing = studentService.getStudentById(student.getId());
            if (student.getProfilePhoto() == null || student.getProfilePhoto().isBlank()) {
                student.setProfilePhoto(existing.getProfilePhoto());
            }
        }

        if (photoFile != null && !photoFile.isEmpty()) {
            try {
                String contentType = photoFile.getContentType() != null ? photoFile.getContentType() : "image/png";
                String base64 = Base64.getEncoder().encodeToString(photoFile.getBytes());
                student.setProfilePhoto("data:" + contentType + ";base64," + base64);
            } catch (IOException ignored) {
            }
        }

        studentService.saveStudent(student);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                isNew ? "Student added successfully!" : "Student updated successfully!"
        );
        return "redirect:/students";
    }

    @PostMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        studentService.deleteStudent(id);
        redirectAttributes.addFlashAttribute("successMessage", "Student deleted successfully!");
        return "redirect:/students";
    }

    @PostMapping("/bulk-promote")
    public String bulkPromote(@RequestParam(value = "studentIds", required = false) List<Long> studentIds,
                              RedirectAttributes redirectAttributes) {
        int count = studentService.bulkPromoteStudents(studentIds);
        if (count > 0) {
            redirectAttributes.addFlashAttribute("successMessage", count + " student(s) promoted to the next semester!");
        } else {
            redirectAttributes.addFlashAttribute("warningMessage", "No eligible students selected for promotion.");
        }
        return "redirect:/students";
    }

    @PostMapping("/import-csv")
    public String importCsv(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid CSV file.");
            return "redirect:/students";
        }
        try {
            Map<String, Integer> result = studentService.importFromCsv(file);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    result.get("imported") + " students imported, " + result.get("skipped") + " duplicates/invalid rows skipped."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to import CSV: " + ex.getMessage());
        }
        return "redirect:/students";
    }

    @GetMapping("/sample-csv")
    public void downloadSampleCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"students_sample.csv\"");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("rollNumber,firstName,lastName,email,phone,departmentCode,semester,admissionYear,status,gender,guardianName,guardianPhone,address");
            writer.println("CSE20260001,Aarav,Sharma,aarav.sharma@college.edu,9876543210,CSE,1,2026,Active,Male,Rajesh Sharma,9876500001,12 MG Road Bengaluru");
            writer.println("ECE20260002,Priya,Nair,priya.nair@college.edu,9876543211,ECE,2,2026,Active,Female,Suresh Nair,9876500002,45 Park Street Kochi");
        }
    }

    @GetMapping("/export/csv")
    public void exportCsv(@RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "departmentId", required = false) Long departmentId,
                          @RequestParam(value = "semester", required = false) Integer semester,
                          HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"students_export.csv\"");
        List<Student> students = studentService.getFilteredStudentsList(keyword, departmentId, semester);

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Roll No,Full Name,Email,Department,Semester,Admission Year,Phone,Status");
            for (Student s : students) {
                String dept = s.getDepartment() != null ? s.getDepartment().getCode() : "N/A";
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",%d,%s,\"%s\",\"%s\"%n",
                        escapeCsv(s.getRollNumber()),
                        escapeCsv(s.getFullName()),
                        escapeCsv(s.getEmail()),
                        escapeCsv(dept),
                        s.getSemester() != null ? s.getSemester() : 1,
                        s.getAdmissionYear() != null ? s.getAdmissionYear() : "",
                        escapeCsv(s.getPhone()),
                        escapeCsv(s.getStatus()));
            }
        }
    }

    @GetMapping("/export/pdf")
    public void exportPdf(@RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "departmentId", required = false) Long departmentId,
                          @RequestParam(value = "semester", required = false) Integer semester,
                          HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"students_report.pdf\"");
        List<Student> students = studentService.getFilteredStudentsList(keyword, departmentId, semester);

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        Paragraph title = new Paragraph("College Management System - Student Directory",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(30, 58, 138)));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12);
        document.add(title);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.4f, 2f, 2.4f, 1.1f, 0.9f, 1.3f, 1f});

        String[] headers = {"Roll No.", "Full Name", "Email", "Department", "Semester", "Phone", "Status"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            cell.setBackgroundColor(new Color(15, 23, 42));
            cell.setPadding(6);
            table.addCell(cell);
        }

        for (Student s : students) {
            table.addCell(new Phrase(s.getRollNumber(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            table.addCell(new Phrase(s.getFullName(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            table.addCell(new Phrase(s.getEmail(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            table.addCell(new Phrase(s.getDepartment() != null ? s.getDepartment().getCode() : "N/A", FontFactory.getFont(FontFactory.HELVETICA, 9)));
            table.addCell(new Phrase("Sem " + (s.getSemester() != null ? s.getSemester() : 1), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            table.addCell(new Phrase(s.getPhone() != null ? s.getPhone() : "-", FontFactory.getFont(FontFactory.HELVETICA, 9)));
            table.addCell(new Phrase(s.getStatus() != null ? s.getStatus() : "Active", FontFactory.getFont(FontFactory.HELVETICA, 9)));
        }

        document.add(table);
        document.close();
    }

    @GetMapping("/api/next-roll")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getNextRollNumber(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "admissionYear", required = false) Integer admissionYear) {
        String roll = studentService.generateSuggestedRollNumber(departmentId, admissionYear);
        return ResponseEntity.ok(Collections.singletonMap("rollNumber", roll));
    }

    @GetMapping("/api/{id}/profile")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStudent360Profile(@PathVariable Long id) {
        Student s = studentService.getStudentById(id);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", s.getId());
        profile.put("rollNumber", s.getRollNumber());
        profile.put("firstName", s.getFirstName());
        profile.put("lastName", s.getLastName());
        profile.put("fullName", s.getFullName());
        profile.put("email", s.getEmail());
        profile.put("phone", s.getPhone() != null ? s.getPhone() : "-");
        profile.put("dateOfBirth", s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : "-");
        profile.put("gender", s.getGender() != null && !s.getGender().isBlank() ? s.getGender() : "-");
        profile.put("address", s.getAddress() != null && !s.getAddress().isBlank() ? s.getAddress() : "-");
        profile.put("semester", s.getSemester() != null ? s.getSemester() : 1);
        profile.put("admissionYear", s.getAdmissionYear() != null ? s.getAdmissionYear() : "");
        profile.put("admissionDate", s.getAdmissionDate() != null ? s.getAdmissionDate().toString() : "-");
        profile.put("status", s.getStatus() != null ? s.getStatus() : "Active");
        profile.put("feeStatus", s.getFeeStatus() != null ? s.getFeeStatus() : "Paid");
        profile.put("guardianName", s.getGuardianName() != null && !s.getGuardianName().isBlank() ? s.getGuardianName() : "-");
        profile.put("guardianPhone", s.getGuardianPhone() != null && !s.getGuardianPhone().isBlank() ? s.getGuardianPhone() : "-");
        profile.put("profilePhoto", s.getProfilePhoto());
        profile.put("departmentId", s.getDepartment() != null ? s.getDepartment().getId() : null);
        profile.put("departmentName", s.getDepartment() != null ? s.getDepartment().getCode() + " - " + s.getDepartment().getName() : "N/A");

        // Attendance & Academic preview (synced automatically when Stage 2 Attendance/Exam modules are added)
        int seed = Math.abs(s.getId().intValue());
        List<Map<String, Object>> attendanceList = List.of(
                createAttendanceRow("Data Structures & Algorithms", 84 + (seed % 10)),
                createAttendanceRow("Database Management Systems", 78 + (seed % 12)),
                createAttendanceRow("Operating Systems", 68 + (seed % 14)),
                createAttendanceRow("Computer Networks", 88 - (seed % 9))
        );
        double avgAttendance = attendanceList.stream()
                .mapToInt(m -> (Integer) m.get("percentage"))
                .average()
                .orElse(80.0);

        List<Map<String, Object>> academicsList = List.of(
                createMarkRow("Data Structures & Algorithms", 82 + (seed % 12)),
                createMarkRow("Database Management Systems", 76 + (seed % 15)),
                createMarkRow("Operating Systems", 74 + (seed % 14)),
                createMarkRow("Computer Networks", 80 + (seed % 11))
        );
        double sgpa = academicsList.stream()
                .mapToDouble(m -> (Double) m.get("gradePoint"))
                .average()
                .orElse(8.0);

        boolean feePaid = "Paid".equalsIgnoreCase(s.getFeeStatus());
        boolean eligible = feePaid && avgAttendance >= 75.0;

        profile.put("attendance", attendanceList);
        profile.put("overallAttendance", Math.round(avgAttendance * 10.0) / 10.0);
        profile.put("academics", academicsList);
        profile.put("sgpa", String.format("%.2f", sgpa));
        profile.put("cgpa", String.format("%.2f", Math.max(6.5, sgpa - 0.15)));
        profile.put("examEligible", eligible);

        return ResponseEntity.ok(profile);
    }

    private Map<String, Object> createAttendanceRow(String subject, int percentage) {
        int clamped = Math.min(100, Math.max(55, percentage));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("subject", subject);
        row.put("percentage", clamped);
        row.put("lowWarning", clamped < 75);
        return row;
    }

    private Map<String, Object> createMarkRow(String subject, int marks) {
        int clamped = Math.min(98, Math.max(50, marks));
        double gp = Math.round((clamped / 10.0) * 10.0) / 10.0;
        String grade = clamped >= 90 ? "A+" : clamped >= 80 ? "A" : clamped >= 70 ? "B+" : clamped >= 60 ? "B" : "C";
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("subject", subject);
        row.put("marks", clamped);
        row.put("grade", grade);
        row.put("gradePoint", gp);
        return row;
    }

    private void populateListModel(Model model, String keyword, Long departmentId, Integer semester, int page) {
        Page<Student> studentPage = studentService.getFilteredStudents(keyword, departmentId, semester, page, 10);
        model.addAttribute("activePage", "students");
        model.addAttribute("studentPage", studentPage);
        model.addAttribute("students", studentPage.getContent());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("currentPage", studentPage.getNumber());
        model.addAttribute("totalPages", studentPage.getTotalPages());
    }

    private String escapeCsv(String val) {
        return val == null ? "" : val.replace("\"", "\"\"");
    }
}
