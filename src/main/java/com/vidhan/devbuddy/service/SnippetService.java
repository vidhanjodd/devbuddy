package com.vidhan.devbuddy.service;

import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.entity.User;
import com.vidhan.devbuddy.repository.SnippetRepository;
import com.vidhan.devbuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SnippetService {

    @Autowired
    private SnippetRepository snippetRepository;

    @Autowired
    private UserRepository userRepository;

    public void saveSnippet(Snippet snippet, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();

        snippet.setUser(user);
        snippet.setCreatedAt(LocalDateTime.now());

        snippetRepository.save(snippet);
    }

    public List<Snippet> getUserSnippets(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return snippetRepository.findByUser(user);
    }

    public Snippet getSnippetById(Long id) {
        return snippetRepository.findById(id).orElseThrow();
    }

    public void updateSnippet(Long id, Snippet updatedSnippet, String username) {
        Snippet snippet = snippetRepository.findById(id).orElseThrow();

        if (!snippet.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }

        snippet.setTitle(updatedSnippet.getTitle());
        snippet.setContent(updatedSnippet.getContent());
        snippet.setLanguage(updatedSnippet.getLanguage());

        snippetRepository.save(snippet);
    }

    public void deleteSnippet(Long id, String username) {
        Snippet snippet = snippetRepository.findById(id).orElseThrow();

        if (!snippet.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }

        snippetRepository.delete(snippet);
    }
}