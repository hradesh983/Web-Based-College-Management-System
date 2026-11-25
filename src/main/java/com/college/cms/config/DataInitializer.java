package com.college.cms.config;

import com.college.cms.entity.Department;
import com.college.cms.entity.User;
import com.college.cms.repository.DepartmentRepository;
import com.college.cms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "System Administrator",
                    "ROLE_ADMIN"
            );
            userRepository.save(admin);
        }

        if (departmentRepository.count() == 0) {
            List<Department> defaults = List.of(
                    new Department("CSE", "Computer Science & Engineering", "Dr. A. Sharma", "Department of Computer Science and Software Engineering"),
                    new Department("IT", "Information Technology", "Dr. R. Verma", "Department of Information Technology and Systems"),
                    new Department("ECE", "Electronics & Communication", "Dr. S. Nair", "Department of Electronics and Communication Engineering"),
                    new Department("ME", "Mechanical Engineering", "Dr. P. Kulkarni", "Department of Mechanical and Manufacturing Engineering")
            );
            departmentRepository.saveAll(defaults);
        }
    }
}
