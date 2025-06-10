package com.example.admin_service;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for admin-service.
 */
@SpringBootApplication
@EnableAdminServer
public final class AdminServiceApplication {

    private AdminServiceApplication() {
        // Prevent instantiation
    }

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
