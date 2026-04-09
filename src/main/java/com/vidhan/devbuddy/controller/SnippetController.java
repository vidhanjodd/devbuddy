package com.vidhan.devbuddy.controller;

import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.service.SnippetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequestMapping("/snippets")
@RequiredArgsConstructor
public class SnippetController {

    private final SnippetService snippetService;

    @GetMapping
    public String getSnippets(Model model, Principal principal) {
        model.addAttribute("snippets",
                snippetService.getUserSnippets(principal.getName()));
        return "snippets";
    }

    @GetMapping("/new")
    public String newSnippetForm(Model model) {
        model.addAttribute("snippet", new Snippet());
        return "create-snippet";
    }

    @PostMapping
    public String saveSnippet(
            @Valid @ModelAttribute("snippet") Snippet snippet,
            BindingResult bindingResult,
            Principal principal,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "create-snippet";
        }
        snippetService.saveSnippet(snippet, principal.getName());
        return "redirect:/snippets";
    }

    @GetMapping("/edit/{id}")
    public String editSnippetForm(@PathVariable Long id, Model model, Principal principal) {
        // FIX #4: Ownership check before serving the edit form
        Snippet snippet = snippetService.getSnippetById(id);
        if (!snippet.getUser().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this snippet");
        }
        model.addAttribute("snippet", snippet);
        return "edit-snippet";
    }

    @PostMapping("/update/{id}")
    public String updateSnippet(@PathVariable Long id,
                                @Valid @ModelAttribute("snippet") Snippet snippet,
                                BindingResult bindingResult,
                                Principal principal) {
        if (bindingResult.hasErrors()) {
            return "edit-snippet";
        }
        snippetService.updateSnippet(id, snippet, principal.getName());
        return "redirect:/snippets";
    }

    @PostMapping("/delete/{id}")
    public String deleteSnippet(@PathVariable Long id, Principal principal) {
        snippetService.deleteSnippet(id, principal.getName());
        return "redirect:/snippets";
    }
}