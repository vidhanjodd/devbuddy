package com.vidhan.devbuddy.service;

import com.vidhan.devbuddy.entity.Snippet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GitHubImportService {

    private final SnippetService snippetService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Map<String, String> EXT_TO_LANG = Map.ofEntries(
            Map.entry("java",   "Java"),
            Map.entry("js",     "JavaScript"),
            Map.entry("ts",     "TypeScript"),
            Map.entry("tsx",    "TypeScript"),
            Map.entry("jsx",    "JavaScript"),
            Map.entry("py",     "Python"),
            Map.entry("go",     "Go"),
            Map.entry("rs",     "Rust"),
            Map.entry("sql",    "SQL"),
            Map.entry("sh",     "Bash"),
            Map.entry("bash",   "Bash"),
            Map.entry("zsh",    "Bash"),
            Map.entry("rb",     "Ruby"),
            Map.entry("kt",     "Kotlin"),
            Map.entry("cs",     "C#"),
            Map.entry("cpp",    "C++"),
            Map.entry("c",      "C"),
            Map.entry("php",    "PHP"),
            Map.entry("swift",  "Swift"),
            Map.entry("yaml",   "YAML"),
            Map.entry("yml",    "YAML"),
            Map.entry("json",   "JSON"),
            Map.entry("xml",    "XML"),
            Map.entry("html",   "HTML"),
            Map.entry("css",    "CSS"),
            Map.entry("md",     "Markdown")
    );

    public record ImportResult(boolean success, String error,
                               String title, String content,
                               String language, String rawUrl) {}


    public ImportResult fetchFromUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.isBlank()) {
            return ImportResult(false, "URL cannot be empty.", null, null, null, null);
        }

        String url = inputUrl.trim();

        if (!url.contains("github.com") && !url.contains("raw.githubusercontent.com")) {
            return fail("Only GitHub URLs are supported (github.com or raw.githubusercontent.com).");
        }

        String rawUrl = toRawUrl(url);
        if (rawUrl == null) {
            return fail("Could not parse GitHub URL. Make sure it points to a specific file.");
        }

        String content;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(rawUrl))
                    .header("User-Agent", "DevBuddy/1.0")
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return fail("File not found. Make sure the repo is public and the URL is correct.");
            }
            if (response.statusCode() != 200) {
                return fail("GitHub returned status " + response.statusCode() + ". Try again later.");
            }

            content = response.body();
            if (content == null || content.isBlank()) {
                return fail("The file appears to be empty.");
            }

        } catch (Exception e) {
            return fail("Network error fetching file: " + e.getMessage());
        }

        String fileName = extractFileName(rawUrl);
        String language = detectLanguage(fileName);
        String title = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;

        return new ImportResult(true, null, title, content, language, rawUrl);
    }

    public void saveImported(String title, String content, String language,
                             String rawTags, String username) {
        Snippet snippet = new Snippet();
        snippet.setTitle(title);
        snippet.setContent(content);
        snippet.setLanguage(language);
        snippetService.saveSnippet(snippet, username, rawTags);
    }

    private String toRawUrl(String url) {
        url = url.split("\\?")[0];

        if (url.contains("raw.githubusercontent.com")) {
            return url;
        }

        Pattern blobPattern = Pattern.compile(
                "https?://github\\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.+)"
        );
        Matcher m = blobPattern.matcher(url);
        if (m.matches()) {
            String owner  = m.group(1);
            String repo   = m.group(2);
            String branch = m.group(3);
            String path   = m.group(4);
            return "https://raw.githubusercontent.com/" + owner + "/" + repo
                    + "/" + branch + "/" + path;
        }

        return null;
    }

    private String extractFileName(String rawUrl) {
        String[] parts = rawUrl.split("/");
        return parts[parts.length - 1];
    }

    private String detectLanguage(String fileName) {
        if (!fileName.contains(".")) return "Other";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return EXT_TO_LANG.getOrDefault(ext, "Other");
    }

    private ImportResult fail(String msg) {
        return new ImportResult(false, msg, null, null, null, null);
    }

    private ImportResult ImportResult(boolean success, String error,
                                      String title, String content,
                                      String language, String rawUrl) {
        return new ImportResult(success, error, title, content, language, rawUrl);
    }
}