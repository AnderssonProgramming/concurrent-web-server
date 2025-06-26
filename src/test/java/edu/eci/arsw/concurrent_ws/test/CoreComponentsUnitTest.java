package edu.eci.arsw.concurrent_ws.test;

import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import edu.eci.arsw.concurrent_ws.parser.HttpParser;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for core components that don't require a running server
 */
class CoreComponentsUnitTest {

    private static final Logger logger = LoggerFactory.getLogger(CoreComponentsUnitTest.class);

    @Test
    void testHttpRequestParsing() throws Exception {
        logger.info("🧪 Testing HTTP request parsing...");
        
        HttpParser parser = new HttpParser();
        String rawRequest = "GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
        
        HttpRequest request = parser.parseRequest(rawRequest);
        
        assertNotNull(request, "Parsed request should not be null");
        assertEquals("GET", request.getMethod(), "HTTP method should be GET");
        assertEquals("/", request.getPath(), "Path should be /");
        assertEquals("HTTP/1.1", request.getHttpVersion(), "Version should be HTTP/1.1");
        assertTrue(request.getHeaders().containsKey("host"), "Should contain host header (lowercase)");
        assertEquals("localhost", request.getHeaders().get("host"), "Host should be localhost");
        
        logger.info("✅ HTTP request parsing test passed");
    }

    @Test
    void testHttpResponseCreation() {
        logger.info("🧪 Testing HTTP response creation...");
        
        HttpParser parser = new HttpParser();
        HttpResponse response = new HttpResponse();
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.setHeader("Content-Type", "text/html");
        response.setBody("<html><body>Hello World</body></html>");
        
        String responseString = parser.formatResponse(response);
        
        assertNotNull(responseString, "Response string should not be null");
        assertTrue(responseString.contains("200 OK"), "Should contain status line");
        assertTrue(responseString.contains("Content-Type: text/html"), "Should contain content type header");
        assertTrue(responseString.contains("Hello World"), "Should contain body content");
        
        logger.info("✅ HTTP response creation test passed");
    }

    @Test
    void testHttpRequestWithParameters() throws Exception {
        logger.info("🧪 Testing HTTP request with query parameters...");
        
        HttpParser parser = new HttpParser();
        String rawRequest = "GET /search?q=test&limit=10 HTTP/1.1\r\nHost: localhost\r\n\r\n";
        
        HttpRequest request = parser.parseRequest(rawRequest);
        
        assertNotNull(request, "Parsed request should not be null");
        assertEquals("GET", request.getMethod(), "HTTP method should be GET");
        assertEquals("/search?q=test&limit=10", request.getPath(), "Path should include query parameters");
        
        logger.info("✅ HTTP request with parameters test passed");
    }

    @Test
    void testHttpRequestWithBody() throws Exception {
        logger.info("🧪 Testing HTTP request with body...");
        
        HttpParser parser = new HttpParser();
        String rawRequest = """
                POST /api/data HTTP/1.1\r
                Host: localhost\r
                Content-Type: application/json\r
                Content-Length: 26\r
                \r
                {"name":"test","id":123}""";
        
        HttpRequest request = parser.parseRequest(rawRequest);
        
        assertNotNull(request, "Parsed request should not be null");
        assertEquals("POST", request.getMethod(), "HTTP method should be POST");
        assertEquals("/api/data", request.getPath(), "Path should be /api/data");
        assertTrue(request.getHeaders().containsKey("content-type"), "Should contain content-type header (lowercase)");
        assertEquals("application/json", request.getHeaders().get("content-type"), "Content-Type should be application/json");
        
        logger.info("✅ HTTP request with body test passed");
    }

    @Test
    void testHttpResponseWithCustomHeaders() {
        logger.info("🧪 Testing HTTP response with custom headers...");
        
        HttpParser parser = new HttpParser();
        HttpResponse response = new HttpResponse();
        response.setStatusCode(201);
        response.setStatusMessage("Created");
        response.setHeader("Content-Type", "application/json");
        response.setHeader("X-Custom-Header", "custom-value");
        response.setHeader("Cache-Control", "no-cache");
        response.setBody("{\"id\":123,\"status\":\"created\"}");
        
        String responseString = parser.formatResponse(response);
        
        assertNotNull(responseString, "Response string should not be null");
        assertTrue(responseString.contains("201 Created"), "Should contain status line");
        assertTrue(responseString.contains("Content-Type: application/json"), "Should contain content type");
        assertTrue(responseString.contains("X-Custom-Header: custom-value"), "Should contain custom header");
        assertTrue(responseString.contains("Cache-Control: no-cache"), "Should contain cache control header");
        assertTrue(responseString.contains("\"id\":123"), "Should contain JSON body");
        
        logger.info("✅ HTTP response with custom headers test passed");
    }

    @Test
    void testConcurrentHttpResponseCreation() throws InterruptedException {
        logger.info("🧪 Testing concurrent HTTP response creation...");
        
        HttpParser parser = new HttpParser();
        int numberOfThreads = 10;
        Thread[] threads = new Thread[numberOfThreads];
        HttpResponse[] responses = new HttpResponse[numberOfThreads];
        
        // Create responses concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                HttpResponse response = new HttpResponse();
                response.setStatusCode(200);
                response.setStatusMessage("OK");
                response.setHeader("Content-Type", "text/plain");
                response.setBody("Response from thread " + index);
                responses[index] = response;
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all responses were created correctly
        for (int i = 0; i < numberOfThreads; i++) {
            assertNotNull(responses[i], "Response " + i + " should not be null");
            String formattedResponse = parser.formatResponse(responses[i]);
            assertTrue(formattedResponse.contains("200 OK"), "Response " + i + " should have 200 OK status");
            assertTrue(formattedResponse.contains("thread " + i), "Response " + i + " should contain thread ID");
        }
        
        logger.info("✅ Concurrent HTTP response creation test passed");
    }
}
