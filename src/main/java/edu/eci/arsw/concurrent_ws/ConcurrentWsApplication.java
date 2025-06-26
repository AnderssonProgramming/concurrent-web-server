package edu.eci.arsw.concurrent_ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main application class for the Concurrent Web Server
 * 
 * This application demonstrates:
 * - Concurrent request handling using thread pools
 * - HTTP request/response parsing
 * - Cookie and session management
 * - Clean architecture with separation of concerns
 */
@SpringBootApplication
public class ConcurrentWsApplication {

	private static final Logger logger = LoggerFactory.getLogger(ConcurrentWsApplication.class);

	public static void main(String[] args) {
		logger.info("🚀 Starting Concurrent Web Server Application...");
		
		try {
			ConfigurableApplicationContext context = SpringApplication.run(ConcurrentWsApplication.class, args);
			
			// Log startup information
			String[] profiles = context.getEnvironment().getActiveProfiles();
			if (profiles.length == 0) {
				logger.info("✅ Application started successfully with default profile");
			} else {
				logger.info("✅ Application started successfully with profiles: {}", String.join(", ", profiles));
			}
			
			logger.info("📖 Visit http://localhost:8080 to see the welcome page");
			logger.info("🔧 Use Ctrl+C to gracefully shutdown the server");
			
		} catch (Exception e) {
			logger.error("❌ Failed to start application: {}", e.getMessage(), e);
			System.exit(1);
		}
	}
}
