package com.example.api_gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class GatewayController {

    @Autowired
    private LoadBalancerClient loadBalancer;

    private RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/v1/users")
    public ResponseEntity<Object> forwardToDemo() {
        try {
            // Sử dụng trực tiếp tên service "DEMO"
            String serviceUrl = loadBalancer.choose("DEMO").getUri().toString();
            String url = serviceUrl + "/v1/users";

            return restTemplate.getForEntity(url, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}