package com.example.user_service.controller;

import com.example.user_service.entity.User;
import com.example.user_service.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/health")
    public String health() {
        return "User Service is running!";
    }

    @GetMapping("/test")
    public String test() {
        return "Test endpoint working!";
    }

    /**
     * GET /users/{id} - Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable UUID id) {
        logger.info("GET /users/{} - Fetching user", id);

        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            response.put("success", true);
            response.put("message", "User found");
            response.put("data", user);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * GET /users - Get all users
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        logger.info("GET /users - Fetching all users");

        Map<String, Object> response = new HashMap<>();

        List<User> users = userService.getAllUsers();
        response.put("success", true);
        response.put("message", "Users retrieved successfully");
        response.put("data", users);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /users/{id}/first_name - Update user's first name
     */
    @PutMapping("/{id}/first_name")
    public ResponseEntity<Map<String, Object>> updateFirstName(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateFirstNameRequest request) {

        logger.info("PUT /users/{}/first_name - Updating first name to: {}", id, request.getFirstName());

        Map<String, Object> response = new HashMap<>();

        boolean updated = userService.updateFirstName(id, request.getFirstName());
        if (updated) {
            response.put("success", true);
            response.put("message", "First name updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found or update failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * PUT /users/{id}/last_name - Update user's last name
     */
    @PutMapping("/{id}/last_name")
    public ResponseEntity<Map<String, Object>> updateLastName(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateLastNameRequest request) {

        logger.info("PUT /users/{}/last_name - Updating last name to: {}", id, request.getLastName());

        Map<String, Object> response = new HashMap<>();

        boolean updated = userService.updateLastName(id, request.getLastName());
        if (updated) {
            response.put("success", true);
            response.put("message", "Last name updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found or update failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * PUT /users/{id}/language - Update user's language
     */
    @PutMapping("/{id}/language")
    public ResponseEntity<Map<String, Object>> updateLanguage(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateLanguageRequest request) {

        logger.info("PUT /users/{}/language - Updating language to: {}", id, request.getLanguage());

        Map<String, Object> response = new HashMap<>();

        boolean updated = userService.updateLanguage(id, request.getLanguage());
        if (updated) {
            response.put("success", true);
            response.put("message", "Language updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found or update failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * PUT /users/{id}/region - Update user's region
     */
    @PutMapping("/{id}/region")
    public ResponseEntity<Map<String, Object>> updateRegion(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateRegionRequest request) {

        logger.info("PUT /users/{}/region - Updating region to: {}", id, request.getRegion());

        Map<String, Object> response = new HashMap<>();

        boolean updated = userService.updateRegion(id, request.getRegion());
        if (updated) {
            response.put("success", true);
            response.put("message", "Region updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found or update failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * PUT /users/{id}/city - Update user's city
     */
    @PutMapping("/{id}/city")
    public ResponseEntity<Map<String, Object>> updateCity(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateCityRequest request) {

        logger.info("PUT /users/{}/city - Updating city to: {}", id, request.getCity());

        Map<String, Object> response = new HashMap<>();

        boolean updated = userService.updateCity(id, request.getCity());
        if (updated) {
            response.put("success", true);
            response.put("message", "City updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found or update failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * PUT /users/{id}/is_premium - Update user's premium status
     */
    @PutMapping("/{id}/is_premium")
    public ResponseEntity<Map<String, Object>> updateIsPremium(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateIsPremiumRequest request) {

        logger.info("PUT /users/{}/is_premium - Updating is_premium to: {}", id, request.getIsPremium());

        Map<String, Object> response = new HashMap<>();

        boolean updated = userService.updateIsPremium(id, request.getIsPremium());
        if (updated) {
            response.put("success", true);
            response.put("message", "Premium status updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found or update failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * PUT /users/{id}/is_add_credit_card - Update user's credit card addition
     * status
     */
    @PutMapping("/{id}/is_add_credit_card")
    public ResponseEntity<Map<String, Object>> updateIsAddCreditCard(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateIsAddCreditCardRequest request) {

        logger.info("PUT /users/{}/is_add_credit_card - Updating is_add_credit_card to: {}", id,
                request.getIsAddCreditCard());

        Map<String, Object> response = new HashMap<>();

        boolean updated = userService.updateIsAddCreditCard(id, request.getIsAddCreditCard());
        if (updated) {
            response.put("success", true);
            response.put("message", "Credit card addition status updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found or update failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * PUT /users/{id} - Update all user fields
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateUserRequest request) {

        logger.info("PUT /users/{} - Updating all fields", id);

        Map<String, Object> response = new HashMap<>();

        boolean updated = userService.updateAll(id, request.getFirstName(), request.getLastName(),
                request.getLanguage(), request.getRegion(), request.getIsPremium(), request.getIsAddCreditCard(),
                request.getCity());
        if (updated) {
            response.put("success", true);
            response.put("message", "User updated successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "User not found or update failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // Request DTOs for validation
    public static class UpdateUserRequest {
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        private String lastName;

        @Size(max = 10, message = "Language must not exceed 10 characters")
        private String language;

        @Size(max = 50, message = "Region must not exceed 50 characters")
        private String region;

        private Boolean isPremium;

        private Boolean isAddCreditCard;

        private String city;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public Boolean getIsPremium() {
            return isPremium;
        }

        public void setIsPremium(Boolean isPremium) {
            this.isPremium = isPremium;
        }

        public Boolean getIsAddCreditCard() {
            return isAddCreditCard;
        }

        public void setIsAddCreditCard(Boolean isAddCreditCard) {
            this.isAddCreditCard = isAddCreditCard;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    public static class UpdateFirstNameRequest {
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }

    public static class UpdateLastNameRequest {
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        private String lastName;

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    public static class UpdateLanguageRequest {
        @Size(max = 10, message = "Language must not exceed 10 characters")
        private String language;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    public static class UpdateRegionRequest {
        @Size(max = 50, message = "Region must not exceed 50 characters")
        private String region;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    public static class UpdateCityRequest {
        @Size(max = 100, message = "City must not exceed 100 characters")
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    public static class UpdateIsPremiumRequest {
        private Boolean isPremium;

        public Boolean getIsPremium() {
            return isPremium;
        }

        public void setIsPremium(Boolean isPremium) {
            this.isPremium = isPremium;
        }
    }

    public static class UpdateIsAddCreditCardRequest {
        private Boolean isAddCreditCard;

        public Boolean getIsAddCreditCard() {
            return isAddCreditCard;
        }

        public void setIsAddCreditCard(Boolean isAddCreditCard) {
            this.isAddCreditCard = isAddCreditCard;
        }
    }
}