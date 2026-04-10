package com.vidhan.devbuddy.controller;

import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.service.SnippetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequestMapping("/snippets")
@RequiredArgsConstructor
public class SnippetController {

    private final SnippetService snippetService;

    @InitBinder("snippet")
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("tags");
    }


    @GetMapping
    public String getSnippets(
            @RequestParam(defaultValue = "")  String query,
            @RequestParam(defaultValue = "0") int    page,
            Model model,
            Principal principal) {

        Page<Snippet> snippetPage = snippetService.getUserSnippets(principal.getName(), query, page);

        model.addAttribute("snippets",    snippetPage.getContent());
        model.addAttribute("currentPage", snippetPage.getNumber());
        model.addAttribute("totalPages",  snippetPage.getTotalPages());
        model.addAttribute("totalItems",  snippetPage.getTotalElements());
        model.addAttribute("query",       query);
        model.addAttribute("hasPrev",     snippetPage.hasPrevious());
        model.addAttribute("hasNext",     snippetPage.hasNext());
        return "snippets";
    }


    @GetMapping("/new")
    public String newSnippetForm(
            @RequestParam(required = false, defaultValue = "") String title,
            @RequestParam(required = false, defaultValue = "") String language,
            @RequestParam(required = false, defaultValue = "") String content,
            Model model) {

        Snippet snippet = new Snippet();
        snippet.setTitle(title);
        snippet.setLanguage(language);
        snippet.setContent(content);
        model.addAttribute("snippet", snippet);
        return "create-snippet";
    }

    @PostMapping
    public String saveSnippet(
            @Valid @ModelAttribute("snippet") Snippet snippet,
            BindingResult bindingResult,
            @RequestParam(required = false, defaultValue = "") String tags,
            Principal principal,
            Model model) {

        if (bindingResult.hasErrors()) return "create-snippet";
        snippetService.saveSnippet(snippet, principal.getName(), tags);
        return "redirect:/snippets";
    }


    @GetMapping("/edit/{id}")
    public String editSnippetForm(@PathVariable Long id, Model model, Principal principal) {
        Snippet snippet = snippetService.getSnippetById(id);
        if (!snippet.getUser().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this snippet");
        }
        String tagString = snippet.getTags().stream()
                .map(t -> t.getName())
                .sorted()
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);

        model.addAttribute("snippet",   snippet);
        model.addAttribute("tagString", tagString);
        return "edit-snippet";
    }

    @PostMapping("/update/{id}")
    public String updateSnippet(
            @PathVariable Long id,
            @Valid @ModelAttribute("snippet") Snippet snippet,
            BindingResult bindingResult,
            @RequestParam(required = false, defaultValue = "") String tags,
            Principal principal) {

        if (bindingResult.hasErrors()) return "edit-snippet";
        snippetService.updateSnippet(id, snippet, principal.getName(), tags);
        return "redirect:/snippets";
    }



    @PostMapping("/delete/{id}")
    public String deleteSnippet(@PathVariable Long id, Principal principal) {
        snippetService.deleteSnippet(id, principal.getName());
        return "redirect:/snippets";
    }
}