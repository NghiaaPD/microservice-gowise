package com.example.api_gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class GatewayController {

    /**
     * Load balancer client.
     */
    @Autowired
    private LoadBalancerClient loadBalancer;

    private RestTemplate restTemplate = new RestTemplate();

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
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
