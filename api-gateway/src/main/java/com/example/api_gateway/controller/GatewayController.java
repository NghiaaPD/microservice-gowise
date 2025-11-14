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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
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
     * Forward change password request to auth service.
     */
    @PostMapping("/auth/change-password")
    public ResponseEntity<Object> forwardToAuthServiceChangePassword(@RequestBody Map<String, String> request) {
        try {
            String serviceUrl = loadBalancer.choose("auth-service").getUri().toString();
            String url = serviceUrl + "/auth/change-password";

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
                        "message", "Password change failed"));
            }
        } catch (Exception e) {
            logger.error("Error connecting to auth service change-password: {}", e.getMessage());
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
     * Forward GET /users/search?name={keyword} to user-service (search users by
     * name)
     * Real-time search for add friend feature
     */
    @GetMapping("/users/search")
    public ResponseEntity<Object> forwardSearchUsers(@RequestParam String name) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/search?name=" + name;
            logger.info("Forwarding search request to user-service: {}", url);
            return restTemplate.getForEntity(url, Object.class);
        } catch (HttpClientErrorException e) {
            logger.warn("User service returned client error: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to user service search: {}", e.getMessage());
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

    // ==================== PLAN SERVICE ENDPOINTS ====================

    /**
     * Forward POST /flights/search to plan-service
     */
    @PostMapping("/flights/search")
    public ResponseEntity<Object> forwardFlightSearch(@RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/flights/search";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> entity = new HttpEntity<>(body, headers);
                return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/flights/search";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in flight search: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in flight search: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service flight search: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Flight search service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward POST /hotels/search to plan-service
     */
    @PostMapping("/hotels/search")
    public ResponseEntity<Object> forwardHotelSearch(@RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/hotels/search";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> entity = new HttpEntity<>(body, headers);
                return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/hotels/search";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in hotel search: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in hotel search: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service hotel search: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Hotel search service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    // ==================== TRAVEL AGENT ENDPOINTS ====================

    /**
     * Forward GET /agent/status to plan-service
     */
    @GetMapping("/agent/status")
    public ResponseEntity<Object> forwardAgentStatus() {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/agent/status";
                return restTemplate.getForEntity(url, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/agent/status";
            return restTemplate.getForEntity(url, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in agent status: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in agent status: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service agent status: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Travel agent service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward POST /agent/chat to plan-service
     */
    @PostMapping("/agent/chat")
    public ResponseEntity<Object> forwardAgentChat(@RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/agent/chat";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> entity = new HttpEntity<>(body, headers);
                return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/agent/chat";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in agent chat: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in agent chat: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service agent chat: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Travel agent chat service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward POST /agent/places to plan-service
     */
    @PostMapping("/agent/places")
    public ResponseEntity<Object> forwardAgentPlaces(@RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/agent/places";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> entity = new HttpEntity<>(body, headers);
                return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/agent/places";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in agent places: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in agent places: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service agent places: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Travel agent places service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward POST /agent/itinerary to plan-service
     */
    @PostMapping("/agent/itinerary")
    public ResponseEntity<Object> forwardAgentItinerary(@RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/agent/itinerary";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> entity = new HttpEntity<>(body, headers);
                return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/agent/itinerary";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in agent itinerary: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in agent itinerary: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service agent itinerary: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Travel agent itinerary service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward GET /cities/suggest to plan-service
     */
    @GetMapping("/cities/suggest")
    public ResponseEntity<Object> forwardCitiesSuggest(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/cities/suggest?q=" + (q != null ? q : "") + "&limit=" + limit;
                return restTemplate.getForEntity(url, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/cities/suggest?q=" + (q != null ? q : "") + "&limit=" + limit;
            return restTemplate.getForEntity(url, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in cities suggest: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in cities suggest: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service cities suggest: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Cities suggest service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward GET /cities/airports to plan-service
     */
    @GetMapping("/cities/airports")
    public ResponseEntity<Object> forwardCitiesAirports(@RequestParam String city) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/cities/airports?city=" + city;
                return restTemplate.getForEntity(url, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/cities/airports?city=" + city;
            return restTemplate.getForEntity(url, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in cities airports: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in cities airports: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service cities airports: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Cities airports service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward POST /plans/save to plan-service
     */
    @PostMapping("/plans/save")
    public ResponseEntity<Object> forwardPlansSave(@RequestBody Map<String, Object> requestBody) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/plans/save";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/plans/save";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in plans save: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in plans save: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service plans save: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Plans save service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward GET /plans/{user_id}/{plan_id} to plan-service
     */
    @GetMapping("/plans/{user_id}/{plan_id}")
    public ResponseEntity<Object> forwardGetPlan(@PathVariable String user_id, @PathVariable String plan_id) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/plans/" + user_id + "/" + plan_id;
                return restTemplate.getForEntity(url, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/plans/" + user_id + "/" + plan_id;
            return restTemplate.getForEntity(url, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in get plan: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in get plan: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service get plan: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Get plan service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward GET /plans/{user_id} to plan-service
     */
    @GetMapping("/plans/{user_id}")
    public ResponseEntity<Object> forwardGetUserPlans(@PathVariable String user_id,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/plans/" + user_id + "?limit=" + limit;
                return restTemplate.getForEntity(url, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/plans/" + user_id + "?limit=" + limit;
            return restTemplate.getForEntity(url, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in get user plans: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in get user plans: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service get user plans: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Get user plans service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward DELETE /plans/{user_id}/{plan_id} to plan-service
     */
    @DeleteMapping("/plans/{user_id}/{plan_id}")
    public ResponseEntity<Object> forwardDeletePlan(@PathVariable String user_id, @PathVariable String plan_id) {
        try {
            var serviceInstance = loadBalancer.choose("PLAN-SERVICE");
            if (serviceInstance == null) {
                logger.warn("PLAN-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8001/plans/" + user_id + "/" + plan_id;
                return restTemplate.exchange(url, HttpMethod.DELETE, null, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/plans/" + user_id + "/" + plan_id;
            return restTemplate.exchange(url, HttpMethod.DELETE, null, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in delete plan: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in delete plan: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to plan service delete plan: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Delete plan service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    // ==================== CHATBOT SERVICE ENDPOINTS ====================

    /**
     * Forward GET /chatbot/hello to chatbot-service
     */
    @GetMapping("/chatbot/hello")
    public ResponseEntity<Object> forwardChatbotHello() {
        try {
            var serviceInstance = loadBalancer.choose("CHATBOT-SERVICE");
            if (serviceInstance == null) {
                logger.warn("CHATBOT-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8002/hello";
                return restTemplate.getForEntity(url, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/hello";
            return restTemplate.getForEntity(url, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in chatbot hello: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in chatbot hello: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to chatbot service hello: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Chatbot service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    /**
     * Forward POST /chatbot/chat to chatbot-service
     */
    @PostMapping("/chatbot/chat")
    public ResponseEntity<Object> forwardChatbotChat(@RequestBody Object body) {
        try {
            var serviceInstance = loadBalancer.choose("CHATBOT-SERVICE");
            if (serviceInstance == null) {
                logger.warn("CHATBOT-SERVICE not available in load balancer, trying direct connection");
                // Fallback to direct connection
                String url = "http://localhost:8002/chat";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Object> entity = new HttpEntity<>(body, headers);
                return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            }

            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/chat";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.error("Client error in chatbot chat: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            logger.error("Server error in chatbot chat: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding to chatbot service chat: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Chatbot chat service temporarily unavailable",
                    "error", e.getMessage()));
        }
    }

    // ==========================================
    // GALLERY SERVICE ROUTING
    // ==========================================

    /**
     * Forward upload photo request to gallery service.
     */
    @PostMapping(value = "/api/gallery/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> forwardGalleryUpload(
            @RequestParam("userId") String userId,
            @RequestParam("galleryId") String galleryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "takenAt", required = false) String takenAt) {
        try {
            String serviceUrl = loadBalancer.choose("gallery-service").getUri().toString();
            String url = serviceUrl + "/api/gallery/upload";

            // Create multipart body
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("userId", userId);
            body.add("galleryId", galleryId);
            body.add("file", file.getResource());
            if (caption != null)
                body.add("caption", caption);
            if (location != null)
                body.add("location", location);
            if (takenAt != null)
                body.add("takenAt", takenAt);

            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body,
                    headers);

            // Forward to gallery-service
            ResponseEntity<Object> response = restTemplate.postForEntity(url, requestEntity, Object.class);
            return response;
        } catch (Exception e) {
            logger.error("Error forwarding gallery upload request: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery upload service temporarily unavailable",
                            "error", e.getMessage()));
        }
    }

    /**
     * Forward get galleries by user request to gallery service.
     */
    @GetMapping("/api/gallery/user/{userId}/galleries")
    public ResponseEntity<Object> forwardGetGalleriesByUser(@PathVariable String userId) {
        try {
            String serviceUrl = loadBalancer.choose("gallery-service").getUri().toString();
            String url = serviceUrl + "/api/gallery/user/" + userId + "/galleries";

            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error forwarding get galleries by user request: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error forwarding get galleries by user request: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        }
    }

    /**
     * Forward get photos by user and trip request to gallery service.
     */
    @GetMapping("/api/gallery/user/{userId}/trip/{tripId}")
    public ResponseEntity<Object> forwardGetPhotosByUserAndTrip(@PathVariable String userId,
            @PathVariable String tripId) {
        try {
            String serviceUrl = loadBalancer.choose("gallery-service").getUri().toString();
            String url = serviceUrl + "/api/gallery/user/" + userId + "/trip/" + tripId;

            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error forwarding get photos by user and trip request: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error forwarding get photos by user and trip request: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        }
    }

    /**
     * Forward get photos by gallery request to gallery service.
     */
    @GetMapping("/api/gallery/gallery/{galleryId}")
    public ResponseEntity<Object> forwardGetPhotosByGallery(@PathVariable String galleryId) {
        try {
            String serviceUrl = loadBalancer.choose("gallery-service").getUri().toString();
            String url = serviceUrl + "/api/gallery/gallery/" + galleryId;

            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error forwarding get photos by gallery request: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error forwarding get photos by gallery request: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        }
    }

    /**
     * Forward get photo by id request to gallery service.
     */
    @GetMapping("/api/gallery/{photoId}")
    public ResponseEntity<Object> forwardGetPhotoById(@PathVariable String photoId) {
        try {
            String serviceUrl = loadBalancer.choose("gallery-service").getUri().toString();
            String url = serviceUrl + "/api/gallery/" + photoId;

            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error forwarding get photo by id request: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error forwarding get photo by id request: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        }
    }

    /**
     * Forward delete photo request to gallery service.
     */
    @DeleteMapping("/api/gallery/{photoId}")
    public ResponseEntity<Object> forwardDeletePhoto(@PathVariable String photoId) {
        try {
            String serviceUrl = loadBalancer.choose("gallery-service").getUri().toString();
            String url = serviceUrl + "/api/gallery/" + photoId;

            restTemplate.delete(url);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Photo deleted successfully"));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error forwarding delete photo request: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error forwarding delete photo request: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        }
    }

    /**
     * Forward delete gallery by trip ID request to gallery service.
     */
    @DeleteMapping("/api/gallery/trip/{tripId}")
    public ResponseEntity<Object> forwardDeleteGalleryByTripId(@PathVariable String tripId) {
        try {
            String serviceUrl = loadBalancer.choose("gallery-service").getUri().toString();
            String url = serviceUrl + "/api/gallery/trip/" + tripId;

            restTemplate.delete(url);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Gallery deleted successfully"));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error forwarding delete gallery request: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error forwarding delete gallery request: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "Gallery service temporarily unavailable",
                            "error", e.getMessage()));
        }
    }

    // ========================== BLOG SERVICE ROUTES ==========================

    /**
     * Create a new blog post
     */
    @PostMapping("/api/posts")
    public ResponseEntity<Object> createPost(
            @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers,
            @RequestBody Map<String, Object> body) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            String url = serviceUrl + "/api/posts";

            HttpHeaders forwardHeaders = new HttpHeaders();
            forwardHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers.containsKey("Authorization")) {
                forwardHeaders.set("Authorization", headers.getFirst("Authorization"));
            }
            if (headers.containsKey("X-User-Id")) {
                forwardHeaders.set("X-User-Id", headers.getFirst("X-User-Id"));
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, forwardHeaders);
            return restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error creating post: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error creating post: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Get all posts with pagination
     */
    @GetMapping("/api/posts")
    public ResponseEntity<Object> getAllPosts(@RequestParam Map<String, String> params) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            StringBuilder url = new StringBuilder(serviceUrl + "/api/posts");

            if (!params.isEmpty()) {
                url.append("?");
                params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
            }

            return restTemplate.getForEntity(url.toString(), Object.class);
        } catch (Exception e) {
            logger.error("Error getting posts: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Get a specific post by ID
     */
    @GetMapping("/api/posts/{postId}")
    public ResponseEntity<Object> getPostById(@PathVariable String postId) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            String url = serviceUrl + "/api/posts/" + postId;
            return restTemplate.getForEntity(url, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error getting post: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error getting post: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Update a post
     */
    @PutMapping("/api/posts/{postId}")
    public ResponseEntity<Object> updatePost(
            @PathVariable String postId,
            @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers,
            @RequestBody Map<String, Object> body) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            String url = serviceUrl + "/api/posts/" + postId;

            HttpHeaders forwardHeaders = new HttpHeaders();
            forwardHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers.containsKey("Authorization")) {
                forwardHeaders.set("Authorization", headers.getFirst("Authorization"));
            }
            if (headers.containsKey("X-User-Id")) {
                forwardHeaders.set("X-User-Id", headers.getFirst("X-User-Id"));
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, forwardHeaders);
            return restTemplate.exchange(url, HttpMethod.PUT, request, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error updating post: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error updating post: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Delete a post
     */
    @DeleteMapping("/api/posts/{postId}")
    public ResponseEntity<Object> deletePost(
            @PathVariable String postId,
            @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            String url = serviceUrl + "/api/posts/" + postId;

            HttpHeaders forwardHeaders = new HttpHeaders();
            if (headers.containsKey("Authorization")) {
                forwardHeaders.set("Authorization", headers.getFirst("Authorization"));
            }
            if (headers.containsKey("X-User-Id")) {
                forwardHeaders.set("X-User-Id", headers.getFirst("X-User-Id"));
            }

            HttpEntity<Void> request = new HttpEntity<>(forwardHeaders);
            return restTemplate.exchange(url, HttpMethod.DELETE, request, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error deleting post: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error deleting post: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Like a post
     */
    @PostMapping("/api/posts/{postId}/like")
    public ResponseEntity<Object> likePost(
            @PathVariable String postId,
            @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            String url = serviceUrl + "/api/posts/" + postId + "/like";

            HttpHeaders forwardHeaders = new HttpHeaders();
            if (headers.containsKey("Authorization")) {
                forwardHeaders.set("Authorization", headers.getFirst("Authorization"));
            }
            if (headers.containsKey("X-User-Id")) {
                forwardHeaders.set("X-User-Id", headers.getFirst("X-User-Id"));
            }

            HttpEntity<Void> request = new HttpEntity<>(forwardHeaders);
            return restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error liking post: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error liking post: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Unlike a post
     */
    @DeleteMapping("/api/posts/{postId}/like")
    public ResponseEntity<Object> unlikePost(
            @PathVariable String postId,
            @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            String url = serviceUrl + "/api/posts/" + postId + "/like";

            HttpHeaders forwardHeaders = new HttpHeaders();
            if (headers.containsKey("Authorization")) {
                forwardHeaders.set("Authorization", headers.getFirst("Authorization"));
            }
            if (headers.containsKey("X-User-Id")) {
                forwardHeaders.set("X-User-Id", headers.getFirst("X-User-Id"));
            }

            HttpEntity<Void> request = new HttpEntity<>(forwardHeaders);
            return restTemplate.exchange(url, HttpMethod.DELETE, request, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error unliking post: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error unliking post: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Get my posts (current user's posts)
     */
    @GetMapping("/api/posts/me")
    public ResponseEntity<Object> getMyPosts(
            @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers,
            @RequestParam Map<String, String> params) {
        try {
            var serviceInstance = loadBalancer.choose("BLOG-SERVICE");
            if (serviceInstance == null) {
                logger.error("No instances of BLOG-SERVICE available");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "Blog service temporarily unavailable"));
            }

            String serviceUrl = serviceInstance.getUri().toString();
            StringBuilder url = new StringBuilder(serviceUrl + "/api/posts/me");

            if (!params.isEmpty()) {
                url.append("?");
                params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
            }

            HttpHeaders forwardHeaders = new HttpHeaders();
            if (headers.containsKey("Authorization")) {
                forwardHeaders.set("Authorization", headers.getFirst("Authorization"));
            }
            if (headers.containsKey("X-User-Id")) {
                forwardHeaders.set("X-User-Id", headers.getFirst("X-User-Id"));
            }
            if (headers.containsKey("X-User-Roles")) {
                forwardHeaders.set("X-User-Roles", headers.getFirst("X-User-Roles"));
            }

            HttpEntity<Void> request = new HttpEntity<>(forwardHeaders);
            return restTemplate.exchange(url.toString(), HttpMethod.GET, request, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error getting my posts: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error getting my posts: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Get posts by user
     */
    @GetMapping("/api/posts/by-user/{userId}")
    public ResponseEntity<Object> getPostsByUser(
            @PathVariable String userId,
            @RequestParam Map<String, String> params) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            StringBuilder url = new StringBuilder(serviceUrl + "/api/posts/by-user/" + userId);

            if (!params.isEmpty()) {
                url.append("?");
                params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
            }

            return restTemplate.getForEntity(url.toString(), Object.class);
        } catch (Exception e) {
            logger.error("Error getting user posts: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Get user post statistics
     */
    @GetMapping("/api/posts/stats/{userId}")
    public ResponseEntity<Object> getUserPostStats(@PathVariable String userId) {
        try {
            String serviceUrl = loadBalancer.choose("BLOG-SERVICE").getUri().toString();
            String url = serviceUrl + "/api/posts/stats/" + userId;
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error getting user post stats: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    // ========================== ADMIN BLOG ROUTES ==========================

    /**
     * Get pending posts for moderation
     */
    @GetMapping("/api/admin/posts/pending")
    public ResponseEntity<Object> getPendingPosts(
            @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers,
            @RequestParam Map<String, String> params) {
        try {
            var serviceInstance = loadBalancer.choose("BLOG-SERVICE");
            if (serviceInstance == null) {
                logger.error("BLOG-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "Blog service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            StringBuilder url = new StringBuilder(serviceUrl + "/api/admin/posts/pending");

            if (!params.isEmpty()) {
                url.append("?");
                params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
            }

            HttpHeaders forwardHeaders = new HttpHeaders();
            if (headers.containsKey("Authorization")) {
                forwardHeaders.set("Authorization", headers.getFirst("Authorization"));
            }
            if (headers.containsKey("X-User-Id")) {
                forwardHeaders.set("X-User-Id", headers.getFirst("X-User-Id"));
            }
            if (headers.containsKey("X-User-Roles")) {
                forwardHeaders.set("X-User-Roles", headers.getFirst("X-User-Roles"));
            }

            HttpEntity<Void> request = new HttpEntity<>(forwardHeaders);
            return restTemplate.exchange(url.toString(), HttpMethod.GET, request, Object.class);
        } catch (Exception e) {
            logger.error("Error getting pending posts: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    /**
     * Moderate a post (approve/reject)
     */
    @PostMapping("/api/admin/posts/{postId}/moderate")
    public ResponseEntity<Object> moderatePost(
            @PathVariable String postId,
            @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers,
            @RequestBody Map<String, Object> body) {
        try {
            var serviceInstance = loadBalancer.choose("BLOG-SERVICE");
            if (serviceInstance == null) {
                logger.error("BLOG-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "Blog service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/api/admin/posts/" + postId + "/moderate";

            HttpHeaders forwardHeaders = new HttpHeaders();
            forwardHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers.containsKey("Authorization")) {
                forwardHeaders.set("Authorization", headers.getFirst("Authorization"));
            }
            if (headers.containsKey("X-User-Id")) {
                forwardHeaders.set("X-User-Id", headers.getFirst("X-User-Id"));
            }
            if (headers.containsKey("X-User-Roles")) {
                forwardHeaders.set("X-User-Roles", headers.getFirst("X-User-Roles"));
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, forwardHeaders);
            return restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error moderating post: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error moderating post: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Blog service temporarily unavailable"));
        }
    }

    // ==================== FRIEND ENDPOINTS ====================

    /**
     * POST /users/friends - Add friend (send friend request)
     * Body: { "user_id": "uuid", "friend_id": "uuid" }
     */
    @PostMapping("/users/friends")
    public ResponseEntity<Object> addFriend(@RequestBody Map<String, Object> body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/friends";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            logger.info("Forwarding add friend request to user-service: body={}", body);

            return restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
        } catch (HttpClientErrorException e) {
            logger.warn("User service returned client error: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding add friend request: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * POST /users/friends/list - Get all friends for a user
     * Body: { "user_id": "uuid" }
     */
    @PostMapping("/users/friends/list")
    public ResponseEntity<Object> getAllFriends(@RequestBody Map<String, Object> body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/friends/list";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            logger.info("Forwarding get all friends request to user-service");

            return restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding get all friends request: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * POST /users/friends/pending - Get pending friend requests
     * Body: { "user_id": "uuid" }
     */
    @PostMapping("/users/friends/pending")
    public ResponseEntity<Object> getPendingFriendRequests(@RequestBody Map<String, Object> body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/friends/pending";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            logger.info("Forwarding get pending requests to user-service");

            return restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding get pending requests: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * POST /users/friends/accepted - Get accepted friends
     * Body: { "user_id": "uuid" }
     */
    @PostMapping("/users/friends/accepted")
    public ResponseEntity<Object> getAcceptedFriends(@RequestBody Map<String, Object> body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/friends/accepted";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            logger.info("Forwarding get accepted friends to user-service");

            return restTemplate.exchange(url, HttpMethod.POST, request, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding get accepted friends: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * PUT /users/friends/accept - Accept friend request
     * Body: { "user_id": "uuid", "friend_id": "uuid" }
     */
    @PutMapping("/users/friends/accept")
    public ResponseEntity<Object> acceptFriendRequest(@RequestBody Map<String, Object> body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/friends/accept";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            logger.info("Forwarding accept friend request to user-service");

            return restTemplate.exchange(url, HttpMethod.PUT, request, Object.class);
        } catch (HttpClientErrorException e) {
            logger.warn("User service returned client error: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding accept friend request: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * DELETE /users/friends - Remove friend
     * Body: { "user_id": "uuid", "friend_id": "uuid" }
     */
    @DeleteMapping("/users/friends")
    public ResponseEntity<Object> removeFriend(@RequestBody Map<String, Object> body) {
        try {
            var serviceInstance = loadBalancer.choose("USER-SERVICE");
            if (serviceInstance == null) {
                logger.error("USER-SERVICE not available in load balancer");
                return ResponseEntity.status(503).body(Map.of(
                        "success", false,
                        "message", "User service temporarily unavailable"));
            }
            String serviceUrl = serviceInstance.getUri().toString();
            String url = serviceUrl + "/users/friends";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            logger.info("Forwarding remove friend request to user-service");

            return restTemplate.exchange(url, HttpMethod.DELETE, request, Object.class);
        } catch (HttpClientErrorException e) {
            logger.warn("User service returned client error: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error forwarding remove friend request: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * GET /users/statistics - Get total users count
     */
    @GetMapping("/users/statistics")
    public ResponseEntity<Object> getUserStatistics() {
        try {
            String serviceUrl = loadBalancer.choose("user-service").getUri().toString();
            String url = serviceUrl + "/users/statistics";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding get user statistics request: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * GET /plan/statistics - Get total plans count
     */
    @GetMapping("/plan/statistics")
    public ResponseEntity<Object> getPlanStatistics() {
        try {
            String serviceUrl = loadBalancer.choose("python-service").getUri().toString();
            String url = serviceUrl + "/plan-statistics";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            logger.error("Error forwarding get plan statistics request: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * GET /statistics - Get all system statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Object> getAllStatistics() {
        try {
            // Get user statistics
            String userServiceUrl = loadBalancer.choose("user-service").getUri().toString();
            String userUrl = userServiceUrl + "/users/statistics";
            ResponseEntity<Object> userResponse = restTemplate.getForEntity(userUrl, Object.class);

            // Get plan statistics
            String planServiceUrl = loadBalancer.choose("python-service").getUri().toString();
            String planUrl = planServiceUrl + "/plan-statistics";
            ResponseEntity<Object> planResponse = restTemplate.getForEntity(planUrl, Object.class);

            // Combine results
            Map<String, Object> combinedStats = new HashMap<>();
            combinedStats.put("success", true);

            if (userResponse.getBody() instanceof Map) {
                Map<String, Object> userStats = (Map<String, Object>) userResponse.getBody();
                combinedStats.put("total_users", userStats.get("total_users"));
            }

            if (planResponse.getBody() instanceof Map) {
                Map<String, Object> planStats = (Map<String, Object>) planResponse.getBody();
                combinedStats.put("total_plans", planStats.get("total_plans"));
            }

            return ResponseEntity.ok(combinedStats);
        } catch (Exception e) {
            logger.error("Error getting all statistics: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * POST /api/payos/payment-link - Create payment link
     */
    @PostMapping("/api/payos/payment-link")
    public ResponseEntity<Object> createPaymentLink(@RequestBody Map<String, Object> request) {
        try {
            String serviceUrl = loadBalancer.choose("payment-service").getUri().toString();
            String url = serviceUrl + "/api/payos/payment-link";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.warn("Payment service returned client error: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse payment service error response: {}", parseException.getMessage());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Payment link creation failed"));
            }

        } catch (HttpServerErrorException e) {
            logger.error("Payment service returned server error: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse payment service error response: {}", parseException.getMessage());
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "Service temporarily unavailable"));
            }

        } catch (Exception e) {
            logger.error("Error connecting to payment service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * POST /api/payos/payment-link/premium - Create premium payment link (314,380 VND)
     */
    @PostMapping("/api/payos/payment-link/premium")
    public ResponseEntity<Object> createPremiumPaymentLink(@RequestBody Map<String, Object> request) {
        try {
            String serviceUrl = loadBalancer.choose("payment-service").getUri().toString();
            String url = serviceUrl + "/api/payos/payment-link/premium";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.warn("Payment service returned client error: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse payment service error response: {}", parseException.getMessage());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Premium payment link creation failed"));
            }

        } catch (HttpServerErrorException e) {
            logger.error("Payment service returned server error: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse payment service error response: {}", parseException.getMessage());
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "Service temporarily unavailable"));
            }

        } catch (Exception e) {
            logger.error("Error connecting to payment service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }

    /**
     * POST /api/payos/payment-link/enterprise - Create enterprise payment link (628,760 VND)
     */
    @PostMapping("/api/payos/payment-link/enterprise")
    public ResponseEntity<Object> createEnterprisePaymentLink(@RequestBody Map<String, Object> request) {
        try {
            String serviceUrl = loadBalancer.choose("payment-service").getUri().toString();
            String url = serviceUrl + "/api/payos/payment-link/enterprise";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);

        } catch (HttpClientErrorException e) {
            logger.warn("Payment service returned client error: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse payment service error response: {}", parseException.getMessage());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                        "success", false,
                        "message", "Enterprise payment link creation failed"));
            }

        } catch (HttpServerErrorException e) {
            logger.error("Payment service returned server error: {} - {}", e.getStatusCode(),
                    e.getResponseBodyAsString());

            try {
                Object responseBody = objectMapper.readValue(e.getResponseBodyAsString(), Object.class);
                return ResponseEntity.status(e.getStatusCode()).body(responseBody);
            } catch (Exception parseException) {
                logger.error("Failed to parse payment service error response: {}", parseException.getMessage());
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "Service temporarily unavailable"));
            }

        } catch (Exception e) {
            logger.error("Error connecting to payment service: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Service temporarily unavailable"));
        }
    }
}
