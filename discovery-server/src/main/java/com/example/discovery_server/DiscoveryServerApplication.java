package com.example.discovery_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Main application class for discovery-server.
 */
@SpringBootApplication
@EnableEurekaServer
public final class DiscoveryServerApplication {

    private DiscoveryServerApplication() {
        // Prevent instantiation
    }

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }

}
