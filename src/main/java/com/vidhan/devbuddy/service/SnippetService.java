package com.vidhan.devbuddy.service;

import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.entity.Tag;
import com.vidhan.devbuddy.entity.User;
import com.vidhan.devbuddy.repository.SnippetRepository;
import com.vidhan.devbuddy.repository.TagRepository;
import com.vidhan.devbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SnippetService {

    private final SnippetRepository snippetRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    private static final int PAGE_SIZE = 10;

    public void saveSnippet(Snippet snippet, String username,String rawTags) {
        User user = userRepository.findByUsername(username).orElseThrow();
        snippet.setUser(user);
        snippet.setTags(parseTags(rawTags));
        snippetRepository.save(snippet);
    }

    public void updateSnippet(Long id, Snippet updated, String username, String rawTags) {
        Snippet snippet = getOwnedSnippet(id, username);
        snippet.setTitle(updated.getTitle());
        snippet.setContent(updated.getContent());
        snippet.setLanguage(updated.getLanguage());
        snippet.setTags(parseTags(rawTags));
        snippetRepository.save(snippet);
    }


    public void deleteSnippet(Long id, String username) {
        Snippet snippet = getOwnedSnippet(id, username);
        snippetRepository.delete(snippet);
    }


    public Page<Snippet> getUserSnippets(String username, String query, int page) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());

        if (query != null && !query.isBlank()) {
            return snippetRepository.searchByUser(user, query.trim(), pageable);
        }
        return snippetRepository.findByUser(user, pageable);
    }

    public Snippet getSnippetById(Long id) {
        return snippetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found"));
    }


    private Snippet getOwnedSnippet(Long id, String username) {
        Snippet snippet = getSnippetById(id);
        if (!snippet.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this snippet");
        }
        return snippet;
    }

    private Set<Tag> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) return new HashSet<>();
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name -> tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(new Tag(name))))
                .collect(Collectors.toSet());
    }
}