package com.vidhan.devbuddy.controller;

import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.service.SnippetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.security.Principal;

import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/snippets")
public class SnippetController {

    @Autowired
    private SnippetService snippetService;

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
    public String saveSnippet(@ModelAttribute Snippet snippet,
                              Principal principal) {
        snippetService.saveSnippet(snippet, principal.getName());
        return "redirect:/snippets";
    }
}