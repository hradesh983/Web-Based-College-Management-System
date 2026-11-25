package com.college.cms.repository;

import com.college.cms.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c LEFT JOIN c.department d LEFT JOIN c.faculty f WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:departmentId IS NULL OR d.id = :departmentId) AND " +
           "(:semester IS NULL OR c.semester = :semester)")
    Page<Course> findFiltered(@Param("keyword") String keyword,
                              @Param("departmentId") Long departmentId,
                              @Param("semester") Integer semester,
                              Pageable pageable);

    Optional<Course> findByCourseCode(String courseCode);
}
