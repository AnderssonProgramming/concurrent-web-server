package edu.eci.arsw.concurrent_ws.test.config;

import edu.eci.arsw.concurrent_ws.server.ConcurrentWebServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * Test configuration for concurrent web server tests
 */
@TestConfiguration
@TestPropertySource(locations = "classpath:application-test.properties")
public class TestServerConfig {

    @Autowired
    private ConcurrentWebServer webServer;

    /**
     * Ensures the test server is properly configured for testing
     */
    @Bean
    @Primary
    public TestServerManager testServerManager() {
        return new TestServerManager(webServer);
    }

    /**
     * Helper class to manage server lifecycle during tests
     */
    public static class TestServerManager {
        private final ConcurrentWebServer server;
        private boolean started = false;

        public TestServerManager(ConcurrentWebServer server) {
            this.server = server;
        }

        public synchronized void startServer() {
            if (!started) {
                // Server is started automatically by Spring
                started = true;
            }
        }

        public synchronized void stopServer() {
            if (started) {
                // Server will be stopped automatically by Spring
                started = false;
            }
        }

        public boolean isStarted() {
            return started;
        }
    }
}
