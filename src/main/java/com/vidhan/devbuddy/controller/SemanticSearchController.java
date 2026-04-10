package com.vidhan.devbuddy.controller;

import com.vidhan.devbuddy.service.SemanticSearchService;
import com.vidhan.devbuddy.service.SemanticSearchService.SemanticResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @GetMapping("/search")
    public String searchPage(
            @RequestParam(required = false, defaultValue = "") String query,
            Model model,
            Principal principal) {

        model.addAttribute("query", query);

        if (!query.isBlank()) {
            List<SemanticResult> results =
                    semanticSearchService.search(query, principal.getName());
            model.addAttribute("results", results);
            model.addAttribute("resultCount", results.size());
        }

        return "semantic-search";
    }


    @PostMapping("/search/api")
    @ResponseBody
    public ResponseEntity<?> searchApi(
            @RequestBody Map<String, String> body,
            Principal principal) {

        String query = body.getOrDefault("query", "").trim();
        if (query.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query cannot be empty"));
        }

        List<SemanticResult> results =
                semanticSearchService.search(query, principal.getName());

        List<Map<String, Object>> payload = results.stream().map(r -> Map.<String, Object>of(
                "id",           r.snippet().getId(),
                "title",        r.snippet().getTitle(),
                "language",     r.snippet().getLanguage(),
                "score",        r.scorePercent(),
                "preview",      previewContent(r.snippet().getContent()),
                "editUrl",      "/snippets/edit/" + r.snippet().getId(),
                "tags",         r.snippet().getTags().stream()
                        .map(t -> t.getName())
                        .sorted()
                        .toList()
        )).toList();

        return ResponseEntity.ok(Map.of("results", payload, "count", payload.size()));
    }

    private String previewContent(String content) {
        if (content == null) return "";
        String trimmed = content.strip();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed;
    }
}