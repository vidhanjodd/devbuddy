package com.vidhan.devbuddy.controller;

import com.vidhan.devbuddy.service.GitHubImportService;
import com.vidhan.devbuddy.service.GitHubImportService.ImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/github")
@RequiredArgsConstructor
public class GitHubImportController {

    private final GitHubImportService gitHubImportService;

    @GetMapping("/import")
    public String importPage() {
        return "github-import";
    }

    @PostMapping("/fetch")
    public String fetchAndPreview(
            @RequestParam String githubUrl,
            Model model) {

        ImportResult result = gitHubImportService.fetchFromUrl(githubUrl);

        if (!result.success()) {
            model.addAttribute("error", result.error());
            model.addAttribute("githubUrl", githubUrl);
            return "github-import";
        }

        model.addAttribute("preview",  true);
        model.addAttribute("title",    result.title());
        model.addAttribute("content",  result.content());
        model.addAttribute("language", result.language());
        model.addAttribute("rawUrl",   result.rawUrl());
        model.addAttribute("githubUrl", githubUrl);
        return "github-import";
    }

    @PostMapping("/save")
    public String saveImported(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam String language,
            @RequestParam(required = false, defaultValue = "") String tags,
            Principal principal,
            RedirectAttributes redirectAttrs) {

        gitHubImportService.saveImported(title, content, language, tags, principal.getName());
        redirectAttrs.addFlashAttribute("successMessage",
                "\"" + title + "\" imported from GitHub successfully!");
        return "redirect:/snippets";
    }
}