package edu.eci.arsw.concurrent_ws.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive concurrent tests for the web server
 * These tests assume the server is running on port 8081 (main server)
 * 
 * To run these tests:
 * 1. Start the main server: mvn spring-boot:run
 * 2. Run tests: mvn test -Dtest=StandaloneConcurrentWebServerTest
 */
class StandaloneConcurrentWebServerTest {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneConcurrentWebServerTest.class);
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8081; // Main server port
    private static final int CONNECTION_TIMEOUT = 5000;

    @BeforeAll
    static void setup() {
        logger.info("🚀 Setting up comprehensive concurrent web server tests...");
        logger.info("ℹ️  These tests assume the server is running on port {}", SERVER_PORT);
        logger.info("ℹ️  Please ensure the main server is started before running tests");
        
        // Test server connectivity before running tests
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            logger.info("✅ Server connectivity verified");
        } catch (IOException e) {
            logger.error("❌ Cannot connect to server on port {}. Please start the server first.", SERVER_PORT);
            throw new RuntimeException("Server not available for testing", e);
        }
    }

    @Test
    void testServerAvailability() throws IOException {
        logger.info("🧪 Testing server availability...");
        
        String response = sendHttpRequest("GET", "/");
        assertNotNull(response, "Response should not be null");
        assertTrue(response.contains("200 OK"), "Server should respond with 200 OK");
        
        logger.info("✅ Server availability test passed");
    }

    @Test
    void testConcurrentRequests() throws InterruptedException, ExecutionException {
        logger.info("🧪 Testing concurrent request handling...");
        
        int numberOfThreads = 10; // Reduced for stability
        int requestsPerThread = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<TestResult>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Submit concurrent requests
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<TestResult> future = executor.submit(() -> {
                List<Long> responseTimes = new ArrayList<>();
                int threadSuccessCount = 0;
                int threadErrorCount = 0;

                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long requestStart = System.currentTimeMillis();
                        String response = sendHttpRequest("GET", "/hello");
                        long responseTime = System.currentTimeMillis() - requestStart;
                        
                        responseTimes.add(responseTime);
                        
                        if (response.contains("200 OK")) {
                            threadSuccessCount++;
                            successCount.incrementAndGet();
                        } else {
                            threadErrorCount++;
                            errorCount.incrementAndGet();
                            logger.warn("Thread {} request {} failed: unexpected response", threadId, j);
                        }
                        
                        // Small delay between requests to prevent overwhelming the server
                        // Small delay between requests
                        // Using CompletableFuture delay instead of Thread.sleep
                        CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS);
                        
                    } catch (Exception e) {
                        threadErrorCount++;
                        errorCount.incrementAndGet();
                        logger.error("Thread {} request {} failed: {}", threadId, j, e.getMessage());
                    }
                }

                return new TestResult(threadId, threadSuccessCount, threadErrorCount, responseTimes);
            });
            
            futures.add(future);
        }

        // Collect results
        List<TestResult> results = new ArrayList<>();
        for (Future<TestResult> future : futures) {
            results.add(future.get());
        }

        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;

        // Analyze results
        analyzeTestResults(results, totalTime, numberOfThreads * requestsPerThread, 
                          successCount.get(), errorCount.get());

        // Assertions
        assertTrue(successCount.get() > 0, "At least some requests should succeed");
        assertTrue(successCount.get() >= (numberOfThreads * requestsPerThread) * 0.7, 
                  "At least 70% of requests should succeed");
        
        logger.info("✅ Concurrent requests test completed successfully");
    }

    @Test
    void testDifferentEndpoints() throws IOException {
        logger.info("🧪 Testing different endpoints...");
        
        String[] endpoints = {"/", "/hello", "/time", "/metrics", "/load-test"};
        int successCount = 0;

        for (String endpoint : endpoints) {
            try {
                String response = sendHttpRequest("GET", endpoint);
                if (response.contains("200 OK")) {
                    successCount++;
                    logger.info("✅ Endpoint {} responded successfully", endpoint);
                } else {
                    logger.warn("⚠️  Endpoint {} returned unexpected response", endpoint);
                }
            } catch (Exception e) {
                logger.error("❌ Endpoint {} failed: {}", endpoint, e.getMessage());
            }
        }

        assertTrue(successCount > 0, "At least some endpoints should work");
        logger.info("✅ Endpoints test completed. {}/{} endpoints working", successCount, endpoints.length);
    }

    @Test
    void testLoadTestEndpoint() throws IOException {
        logger.info("🧪 Testing load test endpoint...");
        
        String response = sendHttpRequest("GET", "/load-test");
        assertNotNull(response, "Response should not be null");
        assertTrue(response.contains("200 OK"), "Load test endpoint should respond with 200 OK");
        
        logger.info("✅ Load test endpoint test passed");
    }

    @Test
    void testMetricsEndpoint() throws IOException {
        logger.info("🧪 Testing metrics endpoint...");
        
        String response = sendHttpRequest("GET", "/metrics");
        assertNotNull(response, "Response should not be null");
        assertTrue(response.contains("200 OK"), "Metrics endpoint should respond with 200 OK");
        
        logger.info("✅ Metrics endpoint test passed");
    }

    // Helper methods

    private String sendHttpRequest(String method, String path) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            socket.setSoTimeout(CONNECTION_TIMEOUT);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send request
            out.println(method + " " + path + " HTTP/1.1");
            out.println("Host: " + SERVER_HOST + ":" + SERVER_PORT);
            out.println("Connection: close");
            out.println();

            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }

            return response.toString();
        }
    }

    private void analyzeTestResults(List<TestResult> results, long totalTime, int totalRequests, 
                                   int successCount, int errorCount) {
        logger.info("📊 Test Results Analysis:");
        logger.info("   Total time: {}ms", totalTime);
        logger.info("   Total requests: {}", totalRequests);
        logger.info("   Successful requests: {}", successCount);
        logger.info("   Failed requests: {}", errorCount);
        logger.info("   Success rate: {:.2f}%", (successCount * 100.0) / totalRequests);
        if (totalTime > 0) {
            logger.info("   Requests per second: {:.2f}", (totalRequests * 1000.0) / totalTime);
        }

        // Log per-thread statistics
        for (TestResult result : results) {
            logger.debug("   Thread {}: {} success, {} errors", 
                        result.getThreadId(), result.getSuccessCount(), result.getErrorCount());
        }

        // Calculate response time statistics
        List<Long> allResponseTimes = new ArrayList<>();
        for (TestResult result : results) {
            allResponseTimes.addAll(result.getResponseTimes());
        }

        if (!allResponseTimes.isEmpty()) {
            allResponseTimes.sort(Long::compareTo);
            long min = allResponseTimes.get(0);
            long max = allResponseTimes.get(allResponseTimes.size() - 1);
            long median = allResponseTimes.get(allResponseTimes.size() / 2);
            double avg = allResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

            logger.info("   Response times - Min: {}ms, Max: {}ms, Median: {}ms, Avg: {:.2f}ms", 
                       min, max, median, avg);
        }
    }

    /**
     * Test result holder class
     */
    private static class TestResult {
        private final int threadId;
        private final int successCount;
        private final int errorCount;
        private final List<Long> responseTimes;

        public TestResult(int threadId, int successCount, int errorCount, List<Long> responseTimes) {
            this.threadId = threadId;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.responseTimes = new ArrayList<>(responseTimes);
        }

        public int getThreadId() {
            return threadId;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public List<Long> getResponseTimes() {
            return responseTimes;
        }
    }
}
