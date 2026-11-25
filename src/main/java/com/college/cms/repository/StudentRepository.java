package com.college.cms.repository;

import com.college.cms.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s LEFT JOIN s.department d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:departmentId IS NULL OR d.id = :departmentId) AND " +
           "(:semester IS NULL OR s.semester = :semester)")
    Page<Student> findFiltered(@Param("keyword") String keyword,
                               @Param("departmentId") Long departmentId,
                               @Param("semester") Integer semester,
                               Pageable pageable);

    @Query("SELECT s FROM Student s LEFT JOIN s.department d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:departmentId IS NULL OR d.id = :departmentId) AND " +
           "(:semester IS NULL OR s.semester = :semester)")
    List<Student> findFilteredList(@Param("keyword") String keyword,
                                   @Param("departmentId") Long departmentId,
                                   @Param("semester") Integer semester);

    Optional<Student> findByRollNumber(String rollNumber);

    Optional<Student> findByEmail(String email);

    long countByDepartmentIdAndAdmissionYear(Long departmentId, Integer admissionYear);
}
