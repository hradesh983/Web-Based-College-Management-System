package com.college.cms.repository;

import com.college.cms.entity.Faculty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    @Query("SELECT f FROM Faculty f LEFT JOIN f.department d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(f.employeeId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:departmentId IS NULL OR d.id = :departmentId) AND " +
           "(:designation IS NULL OR :designation = '' OR f.designation = :designation)")
    Page<Faculty> findFiltered(@Param("keyword") String keyword,
                               @Param("departmentId") Long departmentId,
                               @Param("designation") String designation,
                               Pageable pageable);

    Optional<Faculty> findByEmployeeId(String employeeId);

    Optional<Faculty> findByEmail(String email);

    long countByDepartmentId(Long departmentId);
}
