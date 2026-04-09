package com.vidhan.devbuddy.repository;

import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    List<Snippet> findByUser(User user);
}