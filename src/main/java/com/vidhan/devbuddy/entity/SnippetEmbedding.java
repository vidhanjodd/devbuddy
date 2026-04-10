package com.vidhan.devbuddy.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "snippet_embeddings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SnippetEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snippet_id", nullable = false, unique = true)
    private Snippet snippet;

    /**
     * The embedding vector serialized as a JSON array string, e.g. "[0.12, -0.03, ...]".
     * Typical Groq nomic-embed-text-v1.5 vectors are 768 dimensions (~6KB text).
     */
    @Column(name = "vector_json", columnDefinition = "TEXT", nullable = false)
    private String vectorJson;
}