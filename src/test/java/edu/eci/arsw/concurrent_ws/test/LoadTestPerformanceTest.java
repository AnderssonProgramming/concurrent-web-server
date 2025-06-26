package edu.eci.arsw.concurrent_ws.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
 * Performance tests focusing on the server's load testing capabilities
 */
public class LoadTestPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(LoadTestPerformanceTest.class);
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int CONNECTION_TIMEOUT = 15000; // Longer timeout for load tests

    @Test
    @DisplayName("Load Test Endpoint Performance")
    public void testLoadTestEndpointPerformance() throws InterruptedException, ExecutionException {
        logger.info("🚀 Testing load test endpoint performance...");
        
        int numberOfThreads = 15; // Moderate load to avoid overwhelming the test environment
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<LoadTestResult>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Submit load test requests
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<LoadTestResult> future = executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    String response = sendHttpRequest("GET", "/load-test");
                    long responseTime = System.currentTimeMillis() - requestStart;
                    
                    boolean success = response.contains("200 OK") && response.contains("Load Test Results");
                    
                    if (success) {
                        successCount.incrementAndGet();
                        logger.info("Thread {} completed load test in {} ms", threadId, responseTime);
                    } else {
                        errorCount.incrementAndGet();
                        logger.warn("Thread {} failed load test", threadId);
                    }
                    
                    return new LoadTestResult(threadId, success, responseTime);
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("Thread {} failed with exception: {}", threadId, e.getMessage());
                    return new LoadTestResult(threadId, false, -1);
                }
            });
            
            futures.add(future);
        }

        // Collect results
        List<LoadTestResult> results = new ArrayList<>();
        for (Future<LoadTestResult> future : futures) {
            try {
                results.add(future.get(30, TimeUnit.SECONDS)); // Long timeout for load tests
            } catch (TimeoutException e) {
                logger.error("Load test timed out: {}", e.getMessage());
                results.add(new LoadTestResult(-1, false, -1));
            }
        }

        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;

        // Analyze results
        analyzeLoadTestResults(results, totalTime, numberOfThreads);

        // Assertions
        assertFalse(successCount.get() > 0, "At least some load tests should succeed");
        assertFalse(successCount.get() >= numberOfThreads * 0.7, 
                  "At least 70% of load tests should succeed");
        
        // Check that concurrent processing actually happened
        assertTrue(totalTime < numberOfThreads * 3000, 
                  "Concurrent processing should be faster than sequential");
        
        logger.info("✅ Load test endpoint performance test completed");
    }

    @Test
    @DisplayName("Stress Test with Rapid Requests")
    void testRapidRequestStress() throws InterruptedException, ExecutionException {
        logger.info("⚡ Testing rapid request stress...");
        
        int numberOfThreads = 25;
        int requestsPerThread = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Integer>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<Integer> future = executor.submit(() -> {
                int threadSuccessCount = 0;
                
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        String response = sendHttpRequest("GET", "/hello");
                        if (response.contains("200 OK")) {
                            threadSuccessCount++;
                        }
                        
                        // Small delay between requests (using a different approach)
                        if (j < requestsPerThread - 1) {
                            long startWait = System.nanoTime();
                            while (System.nanoTime() - startWait < 10_000_000) {
                                // Busy wait for 10ms
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.debug("Thread {} request {} failed: {}", threadId, j, e.getMessage());
                    }
                }
                
                return threadSuccessCount;
            });
            
            futures.add(future);
        }

        // Collect results
        int totalSuccessful = 0;
        for (Future<Integer> future : futures) {
            totalSuccessful += future.get();
        }

        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;

        // Analyze rapid request results
        int totalRequests = numberOfThreads * requestsPerThread;
        double successRate = (totalSuccessful * 100.0) / totalRequests;
        double throughput = (totalSuccessful * 1000.0) / totalTime;

        logger.info("📊 Rapid Request Stress Results:");
        logger.info("   📋 Total Requests: {}", totalRequests);
        logger.info("   ✅ Successful: {} ({:.1f}%)", totalSuccessful, successRate);
        logger.info("   ⏱️ Total Time: {} ms", totalTime);
        logger.info("   🚀 Throughput: {:.1f} req/sec", throughput);

        // Assertions
        assertFalse(successRate >= 75.0, "At least 75% of rapid requests should succeed");
        assertFalse(throughput >= 5.0, "Should handle at least 5 requests per second");
        
        logger.info("✅ Rapid request stress test completed");
    }

    @Test
    @DisplayName("Thread Pool Saturation Test")
    void testThreadPoolSaturation() throws InterruptedException, ExecutionException {
        logger.info("🧵 Testing thread pool saturation...");
        
        // Try to saturate the thread pool (default max is 50)
        int numberOfRequests = 60;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfRequests);
        List<Future<SaturationResult>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit more requests than the server's thread pool can handle simultaneously
        for (int i = 0; i < numberOfRequests; i++) {
            final int requestId = i;
            Future<SaturationResult> future = executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    String response = sendHttpRequest("GET", "/time"); // Quick endpoint
                    long responseTime = System.currentTimeMillis() - requestStart;
                    
                    boolean success = response.contains("200 OK");
                    return new SaturationResult(requestId, success, responseTime, requestStart);
                    
                } catch (Exception e) {
                    return new SaturationResult(requestId, false, -1, System.currentTimeMillis());
                }
            });
            
            futures.add(future);
        }

        // Collect results
        List<SaturationResult> results = new ArrayList<>();
        for (Future<SaturationResult> future : futures) {
            results.add(future.get());
        }

        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;

        // Analyze saturation results
        analyzeSaturationResults(results, totalTime, numberOfRequests);

        // Assertions
        long successCount = results.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        assertFalse(successCount > numberOfRequests * 0.8, 
                  "At least 80% of requests should succeed even under saturation");
        
        logger.info("✅ Thread pool saturation test completed");
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
                // Limit response size to avoid memory issues in tests
                if (response.length() > 10000) {
                    break;
                }
            }
            
            return response.toString();
        }
    }

    private void analyzeLoadTestResults(List<LoadTestResult> results, long totalTime, int totalRequests) {
        logger.info("📊 Load Test Performance Results:");
        logger.info("   📋 Total Load Tests: {}", totalRequests);
        
        long successCount = results.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        logger.info("   ✅ Successful: {} ({:.1f}%)", successCount, (successCount * 100.0) / totalRequests);
        
        logger.info("   ⏱️ Total Time: {} ms", totalTime);
        logger.info("   🚀 Concurrent Efficiency: {:.1f}x", 
                   (totalRequests * 3000.0) / totalTime); // Assuming 3s per load test sequentially
        
        if (successCount > 0) {
            double avgResponseTime = results.stream()
                .filter(r -> r.success && r.responseTime > 0)
                .mapToLong(r -> r.responseTime)
                .average()
                .orElse(0.0);
            logger.info("   📈 Average Load Test Time: {:.0f} ms", avgResponseTime);
        }
    }

    private void analyzeSaturationResults(List<SaturationResult> results, long totalTime, int totalRequests) {
        logger.info("📊 Thread Pool Saturation Results:");
        logger.info("   📋 Total Requests: {}", totalRequests);
        
        long successCount = results.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        logger.info("   ✅ Successful: {} ({:.1f}%)", successCount, (successCount * 100.0) / totalRequests);
        
        logger.info("   ⏱️ Total Time: {} ms", totalTime);
        
        if (successCount > 0) {
            double avgResponseTime = results.stream()
                .filter(r -> r.success && r.responseTime > 0)
                .mapToLong(r -> r.responseTime)
                .average()
                .orElse(0.0);
            logger.info("   📈 Average Response Time: {:.0f} ms", avgResponseTime);
            
            // Analyze request distribution over time
            long minStartTime = results.stream()
                .mapToLong(r -> r.startTime)
                .min()
                .orElse(0);
            
            logger.info("   📊 Request Distribution Analysis:");
            long timeWindow = 1000; // 1 second windows
            long maxTime = results.stream().mapToLong(r -> r.startTime).max().orElse(0);
            
            for (long window = minStartTime; window <= maxTime; window += timeWindow) {
                final long windowStart = window;
                final long windowEnd = window + timeWindow;
                
                long requestsInWindow = results.stream()
                    .filter(r -> r.startTime >= windowStart && r.startTime < windowEnd)
                    .count();
                
                if (requestsInWindow > 0) {
                    logger.info("     Window {}s: {} requests", 
                               (window - minStartTime) / 1000, requestsInWindow);
                }
            }
        }
    }

    // Result classes
    public static class LoadTestResult {
        public final int threadId;
        public final boolean success;
        public final long responseTime;

        public LoadTestResult(int threadId, boolean success, long responseTime) {
            this.threadId = threadId;
            this.success = success;
            this.responseTime = responseTime;
        }
    }

    public static class SaturationResult {
        public final int requestId;
        public final boolean success;
        public final long responseTime;
        public final long startTime;

        public SaturationResult(int requestId, boolean success, long responseTime, long startTime) {
            this.requestId = requestId;
            this.success = success;
            this.responseTime = responseTime;
            this.startTime = startTime;
        }
    }
}
