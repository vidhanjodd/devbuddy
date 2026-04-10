package com.vidhan.devbuddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.entity.SnippetEmbedding;
import com.vidhan.devbuddy.repository.SnippetEmbeddingRepository;
import com.vidhan.devbuddy.repository.SnippetRepository;
import com.vidhan.devbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 5: Semantic Search using Groq's embedding API.
 *
 * Strategy:
 * - When a snippet is saved/updated, generate an embedding via Groq and store it
 *   in the snippet_embeddings table as a JSON array string.
 * - At search time, embed the query, then compute cosine similarity against all
 *   stored embeddings for that user — fully in-memory (no PGVector needed).
 * - Returns snippets ranked by semantic similarity, filtered above a threshold.
 *
 * The embedding model used is "nomic-embed-text-v1.5" via Groq's OpenAI-compatible
 * embeddings endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final SnippetEmbeddingRepository embeddingRepository;
    private final SnippetRepository          snippetRepository;
    private final UserRepository             userRepository;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.embedding.model:nomic-embed-text-v1.5}")
    private String embeddingModel;

    @Value("${groq.embedding.url:https://api.groq.com/openai/v1/embeddings}")
    private String embeddingUrl;

    private static final double SIMILARITY_THRESHOLD = 0.30;
    private static final int    MAX_RESULTS          = 20;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    @Transactional
    public void indexSnippet(Snippet snippet) {
        try {
            String textToEmbed = buildEmbeddingText(snippet);
            double[] vector    = embed(textToEmbed);
            if (vector == null) return;

            SnippetEmbedding se = embeddingRepository
                    .findBySnippetId(snippet.getId())
                    .orElse(new SnippetEmbedding());

            se.setSnippet(snippet);
            se.setVectorJson(serializeVector(vector));
            embeddingRepository.save(se);

        } catch (Exception e) {
            log.warn("Failed to index snippet {}: {}", snippet.getId(), e.getMessage());
        }
    }

    @Transactional
    public void removeIndex(Long snippetId) {
        embeddingRepository.deleteBySnippetId(snippetId);
    }

    public List<SemanticResult> search(String query, String username) {
        if (query == null || query.isBlank()) return List.of();

        double[] queryVector = embed(query);
        if (queryVector == null) return List.of();

        var user = userRepository.findByUsername(username).orElseThrow();
        List<Snippet> userSnippets = snippetRepository.findByUser(user);
        Set<Long> userSnippetIds  = userSnippets.stream()
                .map(Snippet::getId)
                .collect(Collectors.toSet());

        List<SnippetEmbedding> embeddings = embeddingRepository.findAllBySnippetIdIn(userSnippetIds);

        List<SemanticResult> results = new ArrayList<>();
        for (SnippetEmbedding se : embeddings) {
            try {
                double[] docVector = deserializeVector(se.getVectorJson());
                double   score     = cosineSimilarity(queryVector, docVector);
                if (score >= SIMILARITY_THRESHOLD) {
                    results.add(new SemanticResult(se.getSnippet(), score));
                }
            } catch (Exception e) {
                log.warn("Skipping malformed embedding for snippet {}", se.getSnippet().getId());
            }
        }

        results.sort(Comparator.comparingDouble(SemanticResult::score).reversed());
        return results.stream().limit(MAX_RESULTS).toList();
    }


    public record SemanticResult(Snippet snippet, double score) {
        public int scorePercent() {
            return (int) Math.round(score * 100);
        }
    }


    private String buildEmbeddingText(Snippet snippet) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(snippet.getTitle()).append("\n");
        sb.append("Language: ").append(snippet.getLanguage()).append("\n");

        if (!snippet.getTags().isEmpty()) {
            String tags = snippet.getTags().stream()
                    .map(t -> t.getName())
                    .collect(Collectors.joining(", "));
            sb.append("Tags: ").append(tags).append("\n");
        }

        String code = snippet.getContent();
        if (code != null && !code.isBlank()) {
            sb.append("Code:\n");
            sb.append(code, 0, Math.min(code.length(), 800));
        }

        return sb.toString();
    }

    private double[] embed(String text) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", embeddingModel);
            body.put("input", text);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() != 200) {
                log.warn("Embedding API returned {}: {}", res.statusCode(), res.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(res.body());
            JsonNode embNode = root.path("data").get(0).path("embedding");

            double[] vector = new double[embNode.size()];
            for (int i = 0; i < embNode.size(); i++) {
                vector[i] = embNode.get(i).asDouble();
            }
            return vector;

        } catch (Exception e) {
            log.warn("Embedding call failed: {}", e.getMessage());
            return null;
        }
    }

    private String serializeVector(double[] vector) throws Exception {
        ArrayNode arr = objectMapper.createArrayNode();
        for (double v : vector) arr.add(v);
        return objectMapper.writeValueAsString(arr);
    }

    private double[] deserializeVector(String json) throws Exception {
        JsonNode arr = objectMapper.readTree(json);
        double[] v   = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) v[i] = arr.get(i).asDouble();
        return v;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return 0.0;
        double dot  = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dot / denom;
    }
}