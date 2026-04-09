package com.vidhan.devbuddy.controller;

import com.vidhan.devbuddy.dto.AiRequest;
import com.vidhan.devbuddy.dto.AiResponse;
import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.service.GrokAiService;
import com.vidhan.devbuddy.service.SnippetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final GrokAiService  grokAiService;
    private final SnippetService snippetService;

    @GetMapping("/generate-page")
    public String generatePage() {
        return "generate-code";
    }

    @PostMapping("/explain")
    @ResponseBody
    public ResponseEntity<AiResponse> explain(@RequestBody AiRequest req, Principal principal) {
        Snippet snippet = getOwnedSnippet(req.getSnippetId(), principal);
        return ResponseEntity.ok(grokAiService.explain(snippet));
    }

    @PostMapping("/optimize")
    @ResponseBody
    public ResponseEntity<AiResponse> optimize(@RequestBody AiRequest req, Principal principal) {
        Snippet snippet = getOwnedSnippet(req.getSnippetId(), principal);
        return ResponseEntity.ok(grokAiService.optimize(snippet));
    }

    @PostMapping("/bugs")
    @ResponseBody
    public ResponseEntity<AiResponse> detectBugs(@RequestBody AiRequest req, Principal principal) {
        Snippet snippet = getOwnedSnippet(req.getSnippetId(), principal);
        return ResponseEntity.ok(grokAiService.detectBugs(snippet));
    }

    @PostMapping("/generate")
    @ResponseBody
    public ResponseEntity<AiResponse> generate(@RequestBody AiRequest req) {
        if (req.getPrompt() == null || req.getPrompt().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(AiResponse.fail("Prompt cannot be empty."));
        }
        String lang = (req.getLanguage() != null && !req.getLanguage().isBlank())
                ? req.getLanguage() : "Code";
        return ResponseEntity.ok(grokAiService.generate(req.getPrompt(), lang));
    }

    private Snippet getOwnedSnippet(Long id, Principal principal) {
        Snippet snippet = snippetService.getSnippetById(id);
        if (!snippet.getUser().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your snippet");
        }
        return snippet;
    }
}