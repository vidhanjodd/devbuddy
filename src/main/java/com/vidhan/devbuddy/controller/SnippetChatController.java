package com.vidhan.devbuddy.controller;

import com.vidhan.devbuddy.dto.AiResponse;
import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.service.GrokAiService;
import com.vidhan.devbuddy.service.SnippetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SnippetChatController {

    private final SnippetService snippetService;
    private final GrokAiService  grokAiService;

    @GetMapping("/snippets/{id}/chat")
    public String chatPage(@PathVariable Long id, Model model, Principal principal) {
        Snippet snippet = getOwnedSnippet(id, principal);
        model.addAttribute("snippet", snippet);
        return "snippet-chat";
    }

    @PostMapping("/snippets/{id}/chat/ask")
    @ResponseBody
    public ResponseEntity<AiResponse> ask(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {

        String question = body.getOrDefault("question", "").trim();
        if (question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(AiResponse.fail("Question cannot be empty."));
        }

        Snippet snippet = getOwnedSnippet(id, principal);
        AiResponse response = grokAiService.chat(snippet, question);
        return ResponseEntity.ok(response);
    }

    private Snippet getOwnedSnippet(Long id, Principal principal) {
        Snippet snippet = snippetService.getSnippetById(id);
        if (!snippet.getUser().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your snippet");
        }
        return snippet;
    }
}