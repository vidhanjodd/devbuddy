package com.vidhan.devbuddy.repository;

import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    List<Snippet> findByUser(User user);

    Page<Snippet> findByUser(User user, Pageable pageable);

    Page<Snippet> findByUserAndTitleContainingIgnoreCase(User user, String title, Pageable pageable);

    @Query("""
        SELECT DISTINCT s FROM Snippet s
        LEFT JOIN s.tags t
        WHERE s.user = :user
          AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.language) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Snippet> searchByUser(@Param("user") User user,
                               @Param("query") String query,
                               Pageable pageable);
}