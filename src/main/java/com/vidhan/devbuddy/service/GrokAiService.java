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
                Format your response with short paragraphs. No lengthy intro needed.

                ```%s
                %s
                ```
                """.formatted(snippet.getLanguage(), snippet.getLanguage().toLowerCase(), snippet.getContent());
        return call(prompt);
    }

    public AiResponse optimize(Snippet snippet) {
        String prompt = """
                You are a senior %s developer doing a code review. Suggest concrete improvements.
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
                You are a security-focused %s engineer. Review this code for bugs, vulnerabilities, and errors.
                For each issue: describe the problem, its severity (Critical / High / Medium / Low), and provide a fix.
                If no issues are found, say so clearly.

                ```%s
                %s
                ```
                """.formatted(snippet.getLanguage(), snippet.getLanguage().toLowerCase(), snippet.getContent());
        return call(prompt);
    }

    public AiResponse generate(String userPrompt, String language) {
        String prompt = """
                You are an expert %s developer. Write clean, production-ready %s code for the requirement below.
                Return ONLY the code inside a markdown code block, then a brief explanation below it.

                Requirement: %s
                """.formatted(language, language, userPrompt);
        return call(prompt);
    }

    public AiResponse chat(Snippet snippet, String question) {
        String systemPrompt = """
                You are an expert %s developer and coding assistant.
                The user is asking about the following code snippet titled "%s".
                Answer specifically and concisely. If the question is about the code, reference
                specific parts of it. If it's a general question, answer it clearly.
                Always format code in markdown code blocks.
                """.formatted(snippet.getLanguage(), snippet.getTitle());

        String userPrompt = """
                Here is the code snippet:

                ```%s
                %s
                ```

                My question: %s
                """.formatted(snippet.getLanguage().toLowerCase(), snippet.getContent(), question);

        return callWithSystem(systemPrompt, userPrompt);
    }


    private AiResponse call(String userPrompt) {
        return callWithSystem(null, userPrompt);
    }

    private AiResponse callWithSystem(String systemPrompt, String userPrompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 1500);

            ArrayNode messages = body.putArray("messages");

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode sysMsg = messages.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
            }

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return AiResponse.fail("Groq API returned status " + response.statusCode()
                        + ": " + response.body());
            }

            JsonNode root    = objectMapper.readTree(response.body());
            String   content = root.path("choices").get(0)
                    .path("message").path("content").asText();

            return AiResponse.ok(content);

        } catch (Exception e) {
            return AiResponse.fail("Failed to reach Groq API: " + e.getMessage());
        }
    }
}