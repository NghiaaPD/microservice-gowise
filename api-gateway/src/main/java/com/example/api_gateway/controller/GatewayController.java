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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
}
