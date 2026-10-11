package com.aimmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single entry point that starts the whole backend: embedded web server,
 * database connection, WebSocket broker, security filters - all wired up by
 * Spring Boot's auto-configuration based on what's on the classpath (see pom.xml).
 */
@SpringBootApplication
public class AimMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AimMasterApplication.class, args);
    }
}
