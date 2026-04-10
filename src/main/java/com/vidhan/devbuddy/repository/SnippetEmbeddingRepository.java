package com.vidhan.devbuddy.repository;

import com.vidhan.devbuddy.entity.SnippetEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SnippetEmbeddingRepository extends JpaRepository<SnippetEmbedding, Long> {

    Optional<SnippetEmbedding> findBySnippetId(Long snippetId);

    List<SnippetEmbedding> findAllBySnippetIdIn(Set<Long> snippetIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM SnippetEmbedding se WHERE se.snippet.id = :snippetId")
    void deleteBySnippetId(@Param("snippetId") Long snippetId);
}