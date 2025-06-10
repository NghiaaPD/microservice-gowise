package com.example.api_gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Gateway controller for routing requests to microservices.
 */
@RestController
public final class GatewayController {

    /**
     * Load balancer client.
     */
    @Autowired
    private LoadBalancerClient loadBalancer;

    /**
     * RestTemplate for HTTP requests.
     */
    private final RestTemplate restTemplate = new RestTemplate();

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
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
            String serviceUrl = loadBalancer.choose("PYTHON-SERVICE").getUri().toString();
            String url = serviceUrl + "/hello";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
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
            String serviceUrl = loadBalancer.choose("GO-SERVICE").getUri().toString();
            String url = serviceUrl + "/v1/ping";
            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
