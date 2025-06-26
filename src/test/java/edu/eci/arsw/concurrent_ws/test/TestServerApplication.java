package edu.eci.arsw.concurrent_ws.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test application for running the concurrent web server on test port
 */
@SpringBootApplication
@ComponentScan(basePackages = "edu.eci.arsw.concurrent_ws")
public class TestServerApplication {

    public static void main(String[] args) {
        System.setProperty("server.port", "8082");
        System.setProperty("spring.profiles.active", "test");
        
        SpringApplication.run(TestServerApplication.class, args);
        
        // The server will start automatically via @PostConstruct
        System.out.println("🧪 Test server started on port 8082");
        System.out.println("Press Ctrl+C to stop the test server");
    }
}
