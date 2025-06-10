package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for demo service.
 */
@SpringBootApplication
public final class DemoApplication {

    private DemoApplication() {
        // Prevent instantiation
    }

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
