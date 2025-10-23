package com.example.blogs.controller;

import com.example.blogs.Utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TestController {
    private final JwtUtils jwtUtils;

    @GetMapping("/token")
    public Map<String, String> gen(@RequestParam UUID userId,
                                   @RequestParam(defaultValue = "USER") List<String> roles) {
        return Map.of("token", jwtUtils.generateToken(userId, roles, null));
    }
}

