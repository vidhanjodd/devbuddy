package com.vidhan.devbuddy.controller;

import com.vidhan.devbuddy.entity.User;
import com.vidhan.devbuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(user);
            return "redirect:/login?registered";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Username or email is already taken. Please choose another.");
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}