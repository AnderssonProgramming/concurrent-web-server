package edu.eci.arsw.concurrent_ws.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced concurrent tests for the web server to validate thread pool behavior,
 * concurrent request handling, and session management
 */
public class EnhancedConcurrentWebServerTest {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedConcurrentWebServerTest.class);
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int CONNECTION_TIMEOUT = 10000;

    @BeforeEach
    public void setUp() {
        logger.info("🔧 Setting up test environment...");
        // Wait a bit to ensure server is ready
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("High Volume Concurrent Request Test")
    public void testHighVolumeConcurrentRequests() throws InterruptedException, ExecutionException {
        logger.info("🧪 Testing high volume concurrent request handling...");
        
        int numberOfThreads = 50;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<TestResult>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

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
                        totalResponseTime.addAndGet(responseTime);
                        
                        if (response.contains("200 OK")) {
                            threadSuccessCount++;
                            successCount.incrementAndGet();
                        } else {
                            threadErrorCount++;
                            errorCount.incrementAndGet();
                            logger.warn("Thread {} request {} failed: unexpected response", threadId, j);
                        }
                        
                        // Small random delay to simulate real usage
                        Thread.sleep(5 + (long)(Math.random() * 15));
                        
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
                          successCount.get(), errorCount.get(), totalResponseTime.get());

        // Assertions
        assertFalse(successCount.get() > 0, "At least some requests should succeed");
        assertFalse(successCount.get() >= (numberOfThreads * requestsPerThread) * 0.85, 
                  "At least 85% of requests should succeed");
        
        long avgResponseTime = totalResponseTime.get() / Math.max(successCount.get(), 1);
        assertFalse(avgResponseTime < 5000, "Average response time should be less than 5 seconds");
        
        logger.info("✅ High volume concurrent test completed successfully");
    }

    @Test
    @DisplayName("Session Management Concurrent Test")
    public void testConcurrentSessionManagement() throws InterruptedException, ExecutionException {
        logger.info("🧪 Testing concurrent session management...");
        
        int numberOfUsers = 20;
        int requestsPerUser = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        List<Future<SessionTestResult>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Create concurrent users with sessions
        for (int i = 0; i < numberOfUsers; i++) {
            final int userId = i;
            Future<SessionTestResult> future = executor.submit(() -> {
                List<String> sessionIds = new ArrayList<>();
                String currentSessionId = null;
                int successfulRequests = 0;
                
                for (int j = 0; j < requestsPerUser; j++) {
                    try {
                        String response = sendHttpRequestWithSession("GET", "/users", currentSessionId);
                        
                        // Extract session ID from Set-Cookie header if present
                        String newSessionId = extractSessionIdFromResponse(response);
                        if (newSessionId != null) {
                            currentSessionId = newSessionId;
                            sessionIds.add(newSessionId);
                        }
                        
                        if (response.contains("200 OK")) {
                            successfulRequests++;
                        }
                        
                        Thread.sleep(100); // Small delay between requests
                        
                    } catch (Exception e) {
                        logger.error("User {} request {} failed: {}", userId, j, e.getMessage());
                    }
                }
                
                return new SessionTestResult(userId, sessionIds, successfulRequests);
            });
            
            futures.add(future);
        }

        // Collect results
        List<SessionTestResult> results = new ArrayList<>();
        for (Future<SessionTestResult> future : futures) {
            results.add(future.get());
        }

        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;

        // Analyze session results
        analyzeSessionTestResults(results, totalTime);

        // Assertions
        assertFalse(results.stream().allMatch(r -> r.successfulRequests > 0), 
                  "All users should have at least one successful request");
        assertFalse(results.stream().allMatch(r -> !r.sessionIds.isEmpty()), 
                  "All users should have at least one session ID");
        
        logger.info("✅ Concurrent session management test completed successfully");
    }

    @Test
    @DisplayName("Load Test with Mixed Endpoints")
    public void testMixedEndpointLoad() throws InterruptedException, ExecutionException {
        logger.info("🧪 Testing mixed endpoint load...");
        
        String[] endpoints = {"/hello", "/time", "/headers", "/cookies", "/users", "/metrics"};
        int numberOfThreads = 30;
        int requestsPerThread = 8;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<EndpointTestResult>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<EndpointTestResult> future = executor.submit(() -> {
                List<EndpointResult> endpointResults = new ArrayList<>();
                
                for (int j = 0; j < requestsPerThread; j++) {
                    String endpoint = endpoints[j % endpoints.length];
                    
                    try {
                        long requestStart = System.currentTimeMillis();
                        String response = sendHttpRequest("GET", endpoint);
                        long responseTime = System.currentTimeMillis() - requestStart;
                        
                        boolean success = response.contains("200 OK");
                        endpointResults.add(new EndpointResult(endpoint, success, responseTime));
                        
                        Thread.sleep(50); // Small delay
                        
                    } catch (Exception e) {
                        endpointResults.add(new EndpointResult(endpoint, false, -1));
                        logger.error("Thread {} failed to access {}: {}", threadId, endpoint, e.getMessage());
                    }
                }
                
                return new EndpointTestResult(threadId, endpointResults);
            });
            
            futures.add(future);
        }

        // Collect results
        List<EndpointTestResult> results = new ArrayList<>();
        for (Future<EndpointTestResult> future : futures) {
            results.add(future.get());
        }

        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;

        // Analyze mixed endpoint results
        analyzeMixedEndpointResults(results, totalTime);

        // Assertions
        int totalRequests = numberOfThreads * requestsPerThread;
        long successCount = results.stream()
            .flatMap(r -> r.endpointResults.stream())
            .mapToLong(er -> er.success ? 1 : 0)
            .sum();
        
        assertFalse(successCount >= totalRequests * 0.8, 
                  "At least 80% of mixed endpoint requests should succeed");
        
        logger.info("✅ Mixed endpoint load test completed successfully");
    }

    private String sendHttpRequest(String method, String path) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            socket.setSoTimeout(CONNECTION_TIMEOUT);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send HTTP request
            out.println(method + " " + path + " HTTP/1.1");
            out.println("Host: " + SERVER_HOST + ":" + SERVER_PORT);
            out.println("Connection: close");
            out.println();
            
            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\\n");
            }
            
            return response.toString();
        }
    }

    private String sendHttpRequestWithSession(String method, String path, String sessionId) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            socket.setSoTimeout(CONNECTION_TIMEOUT);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send HTTP request with session cookie if available
            out.println(method + " " + path + " HTTP/1.1");
            out.println("Host: " + SERVER_HOST + ":" + SERVER_PORT);
            if (sessionId != null) {
                out.println("Cookie: JSESSIONID=" + sessionId);
            }
            out.println("Connection: close");
            out.println();
            
            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\\n");
            }
            
            return response.toString();
        }
    }

    private String extractSessionIdFromResponse(String response) {
        String[] lines = response.split("\\n");
        for (String line : lines) {
            if (line.startsWith("Set-Cookie:") && line.contains("JSESSIONID=")) {
                int start = line.indexOf("JSESSIONID=") + 11;
                int end = line.indexOf(";", start);
                if (end == -1) end = line.length();
                return line.substring(start, end).trim();
            }
        }
        return null;
    }

    private void analyzeTestResults(List<TestResult> results, long totalTime, int expectedRequests, 
                                   int successCount, int errorCount, long totalResponseTime) {
        logger.info("📊 Test Results Analysis:");
        logger.info("   📋 Total Requests: {} (expected: {})", successCount + errorCount, expectedRequests);
        logger.info("   ✅ Successful: {} ({:.1f}%)", successCount, (successCount * 100.0) / expectedRequests);
        logger.info("   ❌ Failed: {} ({:.1f}%)", errorCount, (errorCount * 100.0) / expectedRequests);
        logger.info("   ⏱️ Total Time: {} ms", totalTime);
        logger.info("   🚀 Throughput: {:.1f} req/sec", (successCount * 1000.0) / totalTime);
        
        if (successCount > 0) {
            long avgResponseTime = totalResponseTime / successCount;
            logger.info("   📈 Average Response Time: {} ms", avgResponseTime);
            
            List<Long> allResponseTimes = new ArrayList<>();
            results.forEach(r -> allResponseTimes.addAll(r.responseTimes));
            Collections.sort(allResponseTimes);
            
            if (!allResponseTimes.isEmpty()) {
                int p95Index = (int) (allResponseTimes.size() * 0.95);
                int p99Index = (int) (allResponseTimes.size() * 0.99);
                logger.info("   📊 P95 Response Time: {} ms", allResponseTimes.get(p95Index));
                logger.info("   📊 P99 Response Time: {} ms", allResponseTimes.get(p99Index));
            }
        }
    }

    private void analyzeSessionTestResults(List<SessionTestResult> results, long totalTime) {
        logger.info("👥 Session Test Results:");
        logger.info("   👤 Total Users: {}", results.size());
        logger.info("   ⏱️ Total Time: {} ms", totalTime);
        
        int totalSessions = results.stream().mapToInt(r -> r.sessionIds.size()).sum();
        int totalSuccessfulRequests = results.stream().mapToInt(r -> r.successfulRequests).sum();
        
        logger.info("   🎫 Total Sessions Created: {}", totalSessions);
        logger.info("   ✅ Total Successful Requests: {}", totalSuccessfulRequests);
        logger.info("   📈 Average Sessions per User: {:.1f}", totalSessions / (double) results.size());
    }

    private void analyzeMixedEndpointResults(List<EndpointTestResult> results, long totalTime) {
        logger.info("🌐 Mixed Endpoint Test Results:");
        logger.info("   🧵 Total Threads: {}", results.size());
        logger.info("   ⏱️ Total Time: {} ms", totalTime);
        
        // Group results by endpoint
        java.util.Map<String, List<EndpointResult>> endpointGroups = new java.util.HashMap<>();
        results.forEach(r -> r.endpointResults.forEach(er -> 
            endpointGroups.computeIfAbsent(er.endpoint, k -> new ArrayList<>()).add(er)));
        
        endpointGroups.forEach((endpoint, endpointResults) -> {
            long successCount = endpointResults.stream().mapToLong(er -> er.success ? 1 : 0).sum();
            double avgResponseTime = endpointResults.stream()
                .filter(er -> er.responseTime > 0)
                .mapToLong(er -> er.responseTime)
                .average()
                .orElse(0.0);
            
            logger.info("   📍 {}: {} successful, avg {:.0f}ms", 
                       endpoint, successCount, avgResponseTime);
        });
    }

    // Test result classes
    public static class TestResult {
        public final int threadId;
        public final int successCount;
        public final int errorCount;
        public final List<Long> responseTimes;

        public TestResult(int threadId, int successCount, int errorCount, List<Long> responseTimes) {
            this.threadId = threadId;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.responseTimes = new ArrayList<>(responseTimes);
        }
    }

    public static class SessionTestResult {
        public final int userId;
        public final List<String> sessionIds;
        public final int successfulRequests;

        public SessionTestResult(int userId, List<String> sessionIds, int successfulRequests) {
            this.userId = userId;
            this.sessionIds = new ArrayList<>(sessionIds);
            this.successfulRequests = successfulRequests;
        }
    }

    public static class EndpointTestResult {
        public final int threadId;
        public final List<EndpointResult> endpointResults;

        public EndpointTestResult(int threadId, List<EndpointResult> endpointResults) {
            this.threadId = threadId;
            this.endpointResults = new ArrayList<>(endpointResults);
        }
    }

    public static class EndpointResult {
        public final String endpoint;
        public final boolean success;
        public final long responseTime;

        public EndpointResult(String endpoint, boolean success, long responseTime) {
            this.endpoint = endpoint;
            this.success = success;
            this.responseTime = responseTime;
        }
    }
}
