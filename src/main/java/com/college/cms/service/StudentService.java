package com.college.cms.service;

import com.college.cms.entity.Department;
import com.college.cms.entity.Student;
import com.college.cms.repository.DepartmentRepository;
import com.college.cms.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;

    public StudentService(StudentRepository studentRepository, DepartmentRepository departmentRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
    }

    public Page<Student> getFilteredStudents(String keyword, Long departmentId, Integer semester, int page, int size) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("id").descending());
        return studentRepository.findFiltered(cleanKeyword, departmentId, semester, pageable);
    }

    public List<Student> getFilteredStudentsList(String keyword, Long departmentId, Integer semester) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return studentRepository.findFilteredList(cleanKeyword, departmentId, semester);
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student ID: " + id));
    }

    public boolean isRollNumberDuplicate(String rollNumber, Long currentId) {
        Optional<Student> existing = studentRepository.findByRollNumber(rollNumber);
        return existing.isPresent() && !existing.get().getId().equals(currentId);
    }

    public boolean isEmailDuplicate(String email, Long currentId) {
        Optional<Student> existing = studentRepository.findByEmail(email);
        return existing.isPresent() && !existing.get().getId().equals(currentId);
    }

    public Student saveStudent(Student student) {
        if (student.getStatus() == null || student.getStatus().isBlank()) {
            student.setStatus("Active");
        }
        if (student.getFeeStatus() == null || student.getFeeStatus().isBlank()) {
            student.setFeeStatus("Paid");
        }
        if (student.getAdmissionYear() == null) {
            student.setAdmissionYear(LocalDate.now().getYear());
        }
        if (student.getSemester() == null) {
            student.setSemester(1);
        }
        return studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }

    public long countStudents() {
        return studentRepository.count();
    }

    public String generateSuggestedRollNumber(Long departmentId, Integer admissionYear) {
        int year = (admissionYear != null) ? admissionYear : LocalDate.now().getYear();
        String deptCode = "GEN";
        long seq = studentRepository.count() + 1;

        if (departmentId != null) {
            Optional<Department> deptOpt = departmentRepository.findById(departmentId);
            if (deptOpt.isPresent()) {
                deptCode = deptOpt.get().getCode().toUpperCase();
                seq = studentRepository.countByDepartmentIdAndAdmissionYear(departmentId, year) + 1;
            }
        }

        String candidate;
        do {
            candidate = String.format("%s%d%04d", deptCode, year, seq);
            seq++;
        } while (studentRepository.findByRollNumber(candidate).isPresent());

        return candidate;
    }

    @Transactional
    public int bulkPromoteStudents(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return 0;
        }
        int promoted = 0;
        List<Student> students = studentRepository.findAllById(studentIds);
        for (Student student : students) {
            if ("Graduated".equalsIgnoreCase(student.getStatus())) {
                continue;
            }
            int currentSem = student.getSemester() != null ? student.getSemester() : 1;
            if (currentSem < 8) {
                student.setSemester(currentSem + 1);
                promoted++;
            } else {
                student.setStatus("Graduated");
                promoted++;
            }
        }
        studentRepository.saveAll(students);
        return promoted;
    }

    @Transactional
    public Map<String, Integer> importFromCsv(MultipartFile file) throws Exception {
        int imported = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (isHeader) {
                    isHeader = false;
                    if (line.toLowerCase().contains("rollnumber") || line.toLowerCase().contains("roll_number")) {
                        continue;
                    }
                }

                String[] cols = parseCsvLine(line);
                if (cols.length < 4) {
                    skipped++;
                    continue;
                }

                String rollNumber = cols[0].trim();
                String firstName = cols[1].trim();
                String lastName = cols[2].trim();
                String email = cols[3].trim();

                if (rollNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                    skipped++;
                    continue;
                }

                if (studentRepository.findByRollNumber(rollNumber).isPresent() ||
                    studentRepository.findByEmail(email).isPresent()) {
                    skipped++;
                    continue;
                }

                Student student = new Student();
                student.setRollNumber(rollNumber);
                student.setFirstName(firstName);
                student.setLastName(lastName);
                student.setEmail(email);
                if (cols.length > 4) student.setPhone(cols[4].trim());
                if (cols.length > 5 && !cols[5].trim().isEmpty()) {
                    departmentRepository.findByCode(cols[5].trim().toUpperCase())
                            .ifPresent(student::setDepartment);
                }
                if (cols.length > 6 && !cols[6].trim().isEmpty()) {
                    try {
                        int sem = Integer.parseInt(cols[6].trim());
                        student.setSemester(Math.max(1, Math.min(8, sem)));
                    } catch (NumberFormatException ignored) {
                        student.setSemester(1);
                    }
                }
                if (cols.length > 7 && !cols[7].trim().isEmpty()) {
                    try {
                        student.setAdmissionYear(Integer.parseInt(cols[7].trim()));
                    } catch (NumberFormatException ignored) {
                        student.setAdmissionYear(LocalDate.now().getYear());
                    }
                }
                if (cols.length > 8 && !cols[8].trim().isEmpty()) {
                    student.setStatus(cols[8].trim());
                }
                if (cols.length > 9 && !cols[9].trim().isEmpty()) {
                    student.setGender(cols[9].trim());
                }
                if (cols.length > 10 && !cols[10].trim().isEmpty()) {
                    student.setGuardianName(cols[10].trim());
                }
                if (cols.length > 11 && !cols[11].trim().isEmpty()) {
                    student.setGuardianPhone(cols[11].trim());
                }
                if (cols.length > 12 && !cols[12].trim().isEmpty()) {
                    student.setAddress(cols[12].trim());
                }

                saveStudent(student);
                imported++;
            }
        }

        Map<String, Integer> summary = new HashMap<>();
        summary.put("imported", imported);
        summary.put("skipped", skipped);
        return summary;
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}
