package com.vidhan.devbuddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vidhan.devbuddy.dto.AiResponse;
import com.vidhan.devbuddy.entity.Snippet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class GrokAiService {

    // Injected from application.properties
    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    public AiResponse explain(Snippet snippet) {
        String prompt = """
                You are an expert developer. Explain the following %s code clearly and concisely.
                Cover: what it does, how it works, and any important patterns or gotchas.
                Format your response with short paragraphs. No need for a lengthy intro.

                ```%s
                %s
                ```
                """.formatted(snippet.getLanguage(), snippet.getLanguage().toLowerCase(), snippet.getContent());
        return call(prompt);
    }

    public AiResponse optimize(Snippet snippet) {
        String prompt = """
                You are a senior %s developer doing a code review. Analyze this code and suggest concrete improvements.
                Focus on: performance, readability, best practices, and potential edge cases.
                For each suggestion show the improved code snippet.

                ```%s
                %s
                ```
                """.formatted(snippet.getLanguage(), snippet.getLanguage().toLowerCase(), snippet.getContent());
        return call(prompt);
    }

    public AiResponse detectBugs(Snippet snippet) {
        String prompt = """
                You are a security-focused %s engineer. Carefully review this code for bugs, vulnerabilities, and errors.
                For each issue found: describe the problem, its severity (Critical / High / Medium / Low), and provide a fix.
                If no issues are found, say so clearly.

                ```%s
                %s
                ```
                """.formatted(snippet.getLanguage(), snippet.getLanguage().toLowerCase(), snippet.getContent());
        return call(prompt);
    }

    public AiResponse generate(String userPrompt, String language) {
        String prompt = """
                You are an expert %s developer. Write clean, production-ready %s code for the following requirement.
                Return ONLY the code inside a markdown code block, then a brief explanation below it.

                Requirement: %s
                """.formatted(language, language, userPrompt);
        return call(prompt);
    }


    private AiResponse call(String userPrompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 1500);

            ArrayNode messages = body.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return AiResponse.fail("Grok API returned status " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode root    = objectMapper.readTree(response.body());
            String   content = root.path("choices").get(0)
                    .path("message").path("content").asText();

            return AiResponse.ok(content);

        } catch (Exception e) {
            return AiResponse.fail("Failed to reach Grok API: " + e.getMessage());
        }
    }
}