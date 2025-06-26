package edu.eci.arsw.concurrent_ws.test.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;

/**
 * Base class for concurrent web server tests
 * Provides common functionality and ensures proper test isolation
 */
public abstract class BaseServerTest {

    protected static final Logger logger = LoggerFactory.getLogger(BaseServerTest.class);
    protected static final String SERVER_HOST = "localhost";
    protected static final int TEST_SERVER_PORT = 8082; // Different from main server port
    protected static final int CONNECTION_TIMEOUT = 5000;

    /**
     * Check if the test server is running on the expected port
     */
    protected static boolean isServerRunning() {
        try (Socket socket = new Socket(SERVER_HOST, TEST_SERVER_PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Wait for server to be available
     */
    protected static void waitForServer(int maxAttempts) throws InterruptedException {
        for (int i = 0; i < maxAttempts; i++) {
            if (isServerRunning()) {
                logger.info("✅ Test server is available on port {}", TEST_SERVER_PORT);
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            logger.info("⏳ Waiting for test server... attempt {}/{}", i + 1, maxAttempts);
        }
        throw new RuntimeException("Test server is not available after " + maxAttempts + " attempts");
    }

    /**
     * Find an available port for testing
     */
    protected static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find available port", e);
        }
    }

    /**
     * Send HTTP request to the test server
     */
    protected String sendHttpRequest(String method, String path) throws IOException {
        return sendHttpRequest(method, path, null);
    }

    /**
     * Send HTTP request with body to the test server
     */
    protected String sendHttpRequest(String method, String path, String body) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, TEST_SERVER_PORT)) {
            socket.setSoTimeout(CONNECTION_TIMEOUT);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send request
            out.println(method + " " + path + " HTTP/1.1");
            out.println("Host: " + SERVER_HOST + ":" + TEST_SERVER_PORT);
            out.println("Connection: close");
            
            if (body != null && !body.isEmpty()) {
                out.println("Content-Length: " + body.length());
                out.println("Content-Type: text/plain");
                out.println();
                out.print(body);
            } else {
                out.println();
            }

            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }

            return response.toString();
        }
    }

    /**
     * Parse response time from response headers
     */
    protected long extractResponseTime(String response) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.startsWith("X-Response-Time:")) {
                try {
                    return Long.parseLong(line.substring("X-Response-Time:".length()).trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * Check if response indicates success
     */
    protected boolean isSuccessResponse(String response) {
        return response != null && response.contains("200 OK");
    }

    /**
     * Check if response indicates error
     */
    protected boolean isErrorResponse(String response) {
        return response != null && (response.contains("404") || response.contains("500") || response.contains("400"));
    }
}
