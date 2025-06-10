package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user endpoints.
 */
@RestController
@RequestMapping("/")
public class UserController {

    /**
     * User service.
     */
    @Autowired
    private UserService userService;

    /**
     * Get all users.
     * 
     * @return list of users
     */
    @GetMapping("/v1/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
}
