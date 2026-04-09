package com.vidhan.devbuddy.service;

import com.vidhan.devbuddy.entity.Snippet;
import com.vidhan.devbuddy.entity.User;
import com.vidhan.devbuddy.repository.SnippetRepository;
import com.vidhan.devbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SnippetService {

    private final SnippetRepository snippetRepository;
    private final UserRepository userRepository;

    public void saveSnippet(Snippet snippet, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        snippet.setUser(user);
        snippetRepository.save(snippet);
    }

    public List<Snippet> getUserSnippets(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return snippetRepository.findByUser(user);
    }

    public Snippet getSnippetById(Long id) {
        return snippetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found"));
    }

    public void updateSnippet(Long id, Snippet updatedSnippet, String username) {
        Snippet snippet = snippetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found"));

        if (!snippet.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this snippet");
        }

        snippet.setTitle(updatedSnippet.getTitle());
        snippet.setContent(updatedSnippet.getContent());
        snippet.setLanguage(updatedSnippet.getLanguage());
        snippetRepository.save(snippet);
    }

    public void deleteSnippet(Long id, String username) {
        Snippet snippet = snippetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found"));

        if (!snippet.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this snippet");
        }

        snippetRepository.delete(snippet);
    }
}