package com.example.api_gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

/**
 * Gateway controller for routing requests to microservices.
 */
@RestController
@CrossOrigin(origins = "*")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    @Autowired
    private LoadBalancerClient loadBalancer;

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Forward request to demo service.
     *
     * @return response from demo service
     */
    @GetMapping("/v1/users")
    public ResponseEntity<Object> forwardToDemo() {
        try {
            String serviceUrl = loadBalancer.choose("DEMO").getUri().toString();
            String url = serviceUrl + "/v1/users";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to demo service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward request to python service.
     *
     * @return response from python service
     */
    @GetMapping("/hello")
    public ResponseEntity<Object> forwardToPythonService() {
        try {
            String serviceUrl = loadBalancer
                    .choose("PYTHON-SERVICE")
                    .getUri()
                    .toString();
            String url = serviceUrl + "/hello";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to python service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward request to go service.
     *
     * @return response from go service
     */
    @GetMapping("/v1/ping")
    public ResponseEntity<Object> forwardToGoService() {
        try {
            String serviceUrl = loadBalancer
                    .choose("GO-SERVICE")
                    .getUri()
                    .toString();
            String url = serviceUrl + "/v1/ping";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to go service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    @PostMapping("/auth/signin")
    public ResponseEntity<Object> forwardToAuthServiceSignin(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/signin";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.warn("Auth service returned client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse auth service error response: {}", parseException.getMessage());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Authentication failed"));
            }

        } catch (HttpServerErrorException e) {
            logger.error("Auth service returned server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse auth service error response: {}", parseException.getMessage());
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "Service temporarily unavailable"));
            }

        } catch (Exception e) {
            logger.error("Error connecting to auth service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<Object> forwardToAuthServiceSignup(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/signup";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.warn("Auth service returned client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse auth service error response: {}", parseException.getMessage());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Registration failed"));
            }

        } catch (HttpServerErrorException e) {
            logger.error("Auth service returned server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse auth service error response: {}", parseException.getMessage());
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "Service temporarily unavailable"));
            }

        } catch (Exception e) {
            logger.error("Error connecting to auth service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    @GetMapping("/auth/verify")
    public ResponseEntity<Object> forwardToAuthServiceVerify(@RequestParam String token) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/verify?token=" + token;

            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to auth service verify: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    @PostMapping("/auth/test-email")
    public ResponseEntity<Object> forwardToAuthServiceTestEmail(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/test-email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Test failed"));
            }
        } catch (Exception e) {
            logger.error("Error connecting to auth service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward forgot password request to auth service.
     */
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Object> forwardToAuthServiceForgotPassword(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/forgot-password";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Forgot password failed"));
            }
        } catch (Exception e) {
            logger.error("Error connecting to auth service forgot-password: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward validate OTP request to auth service.
     */
    @PostMapping("/auth/validate-otp")
    public ResponseEntity<Object> forwardToAuthServiceValidateOtp(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/validate-otp";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "OTP validation failed"));
            }
        } catch (Exception e) {
            logger.error("Error connecting to auth service validate-otp: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward reset password request to auth service.
     */
    @PostMapping("/auth/reset-password")
    public ResponseEntity<Object> forwardToAuthServiceResetPassword(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/reset-password";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Password reset failed"));
            }
        } catch (Exception e) {
            logger.error("Error connecting to auth service reset-password: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward refresh token request to auth service.
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<Object> forwardToAuthServiceRefresh(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/refresh";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Token refresh failed"));
            }
        } catch (Exception e) {
            logger.error("Error connecting to auth service refresh: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward logout request to auth service.
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Object> forwardToAuthServiceLogout(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Logout failed"));
            }
        } catch (Exception e) {
            logger.error("Error connecting to auth service logout: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    // User Service Endpoints

    /**
     * Forward GET /users/health to user-service
     */
    @GetMapping("/users/health")
    public ResponseEntity<Object> forwardUserHealth() {
        try {
            String serviceUrl = loadBalancer.choose("USER-SERVICE").getUri().toString();
            String url = serviceUrl + "/users/health";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to user service health: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward GET /users/test to user-service
     */
    @GetMapping("/users/test")
    public ResponseEntity<Object> forwardUserTest() {
        try {
            String serviceUrl = loadBalancer.choose("USER-SERVICE").getUri().toString();
            String url = serviceUrl + "/users/test";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to user service test: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward GET /users/{id} to user-service
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Object> forwardGetUserById(@PathVariable String id) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id;
            return restTemplate.getForEntity(url, Object.class);
        } catch (HttpClientErrorException e) {
            // Forward client errors (like 404) as is
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to user service get user: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward GET /users to user-service (get all users)
     */
    @GetMapping("/users")
    public ResponseEntity<Object> forwardGetAllUsers() {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to user service get all users: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward PUT /users/{id} to user-service (update all)
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<Object> forwardUpdateUser(@PathVariable String id, @RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
        } catch (HttpClientErrorException e) {
            // Forward client errors (like 404) as is
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to user service update user: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward PUT /users/{id}/first_name to user-service
     */
    @PutMapping("/users/{id}/first_name")
    public ResponseEntity<Object> forwardUpdateFirstName(@PathVariable String id, @RequestBody Object body) {
        try {
            String serviceUrl = loadBalancer.choose("USER-SERVICE").getUri().toString();
            String url = serviceUrl + "/users/" + id + "/first_name";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to user service update first name: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward PUT /users/{id}/last_name to user-service
     */
    @PutMapping("/users/{id}/last_name")
    public ResponseEntity<Object> forwardUpdateLastName(@PathVariable String id, @RequestBody Object body) {
        try {
            String serviceUrl = loadBalancer.choose("USER-SERVICE").getUri().toString();
            String url = serviceUrl + "/users/" + id + "/last_name";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to user service update last name: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward PUT /users/{id}/language to user-service
     */
    @PutMapping("/users/{id}/language")
    public ResponseEntity<Object> forwardUpdateLanguage(@PathVariable String id, @RequestBody Object body) {
        try {
            String serviceUrl = loadBalancer.choose("USER-SERVICE").getUri().toString();
            String url = serviceUrl + "/users/" + id + "/language";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding to user service update language: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward PUT /users/{id}/region to user-service
     */
    @PutMapping("/users/{id}/region")
    public ResponseEntity<Object> forwardUpdateRegion(@PathVariable String id, @RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id + "/region";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
        } catch (HttpClientErrorException e) {
            // Forward client errors (like 404) as is
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to user service update region: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward PUT /users/{id}/city to user-service
     */
    @PutMapping("/users/{id}/city")
    public ResponseEntity<Object> forwardUpdateCity(@PathVariable String id, @RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id + "/city";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
        } catch (HttpClientErrorException e) {
            // Forward client errors (like 404) as is
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to user service update city: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward PUT /users/{id}/is_premium to user-service
     */
    @PutMapping("/users/{id}/is_premium")
    public ResponseEntity<Object> forwardUpdateIsPremium(@PathVariable String id, @RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id + "/is_premium";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
        } catch (HttpClientErrorException e) {
            // Forward client errors (like 404) as is
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to user service update is_premium: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * Forward PUT /users/{id}/is_add_credit_card to user-service
     */
    @PutMapping("/users/{id}/is_add_credit_card")
    public ResponseEntity<Object> forwardUpdateIsAddCreditCard(@PathVariable String id, @RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id + "/is_add_credit_card";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
        } catch (HttpClientErrorException e) {
            // Forward client errors (like 404) as is
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to user service update is_add_credit_card: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    // ==================== CREDIT CARD ENDPOINTS ====================

    /**
     * Forward POST /users/{id}/credit-cards to user-service (add credit card)
     */
    @PostMapping("/users/{id}/credit-cards")
    public ResponseEntity<Object> addCreditCard(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            logger.info("Gateway forwarding POST /users/{}/credit-cards", id);
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id + "/credit-cards";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            logger.info("Forwarding to URL: {}", url);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
        } catch (HttpClientErrorException e) {
            logger.error("Client error forwarding add credit card: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error forwarding add credit card: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding add credit card to user service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to add payment method: " + e.getMessage()));
        }
    }

    /**
     * Forward GET /users/{id}/credit-cards to user-service (get user credit cards)
     */
    @GetMapping("/users/{id}/credit-cards")
    public ResponseEntity<Object> getUserCreditCards(@PathVariable String id) {
        try {
            logger.info("Gateway forwarding GET /users/{}/credit-cards", id);
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id + "/credit-cards";

            logger.info("Forwarding to URL: {}", url);
            return restTemplate.getForEntity(url, Object.class);
        } catch (HttpClientErrorException e) {
            logger.error("Client error forwarding get credit cards: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error forwarding get credit cards: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding get credit cards to user service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to get credit cards: " + e.getMessage()));
        }
    }

    /**
     * Forward PUT /users/{id}/credit-cards/{cardId}/default to user-service (set
     * default credit card)
     */
    @PutMapping("/users/{id}/credit-cards/{cardId}/default")
    public ResponseEntity<Object> setDefaultCreditCard(@PathVariable String id, @PathVariable String cardId) {
        try {
            logger.info("Gateway forwarding PUT /users/{}/credit-cards/{}/default", id, cardId);
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service is unavailable"));
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id + "/credit-cards/" + cardId + "/default";

            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    null,
                    Object.class);

            logger.info("Successfully forwarded PUT /users/{}/credit-cards/{}/default", id, cardId);
            return response;

        } catch (Exception e) {
            logger.error("Error forwarding PUT /users/{}/credit-cards/{}/default: {}", id, cardId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to set default credit card: " + e.getMessage()));
        }
    }

    /**
     * Forward DELETE /users/{id}/credit-cards/{cardId} to user-service (delete
     * credit card)
     */
    @DeleteMapping("/users/{id}/credit-cards/{cardId}")
    public ResponseEntity<Object> deleteCreditCard(@PathVariable String id, @PathVariable String cardId) {
        try {
            logger.info("Gateway forwarding DELETE /users/{}/credit-cards/{}", id, cardId);
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service is unavailable"));
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/" + id + "/credit-cards/" + cardId;

            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    Object.class);

            logger.info("Successfully forwarded DELETE /users/{}/credit-cards/{}", id, cardId);
            return response;

        } catch (Exception e) {
            logger.error("Error forwarding DELETE /users/{}/credit-cards/{}: {}", id, cardId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to delete credit card: " + e.getMessage()));
        }
    }
}
