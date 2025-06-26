// package edu.eci.arsw.concurrent_ws.test;

// import edu.eci.arsw.concurrent_ws.test.base.BaseServerTest;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.*;
// import java.util.concurrent.atomic.AtomicInteger;

// import static org.junit.jupiter.api.Assertions.*;

// /**
//  * Concurrent tests for the web server to validate thread pool behavior and concurrent request handling
//  */
// class ConcurrentWebServerTest extends BaseServerTest {

//     private static final Logger logger = LoggerFactory.getLogger(ConcurrentWebServerTest.class);

//     @BeforeAll
//     static void setup() throws InterruptedException {
//         logger.info("🚀 Setting up concurrent web server tests...");
//         // Wait for test server to be available (it should be running on port 8082)
//         waitForServer(10);
//     }

//     @Test
//     void testConcurrentRequests() throws InterruptedException, ExecutionException {
//         logger.info("🧪 Testing concurrent request handling...");
        
//         int numberOfThreads = 20;
//         int requestsPerThread = 5;
//         ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
//         List<Future<TestResult>> futures = new ArrayList<>();
//         AtomicInteger successCount = new AtomicInteger(0);
//         AtomicInteger errorCount = new AtomicInteger(0);

//         long startTime = System.currentTimeMillis();

//         // Submit concurrent requests
//         for (int i = 0; i < numberOfThreads; i++) {
//             final int threadId = i;
//             Future<TestResult> future = executor.submit(() -> {
//                 List<Long> responseTimes = new ArrayList<>();
//                 int threadSuccessCount = 0;
//                 int threadErrorCount = 0;

//                 for (int j = 0; j < requestsPerThread; j++) {
//                     try {
//                         long requestStart = System.currentTimeMillis();
//                         String response = sendHttpRequest("GET", "/hello");
//                         long responseTime = System.currentTimeMillis() - requestStart;
                        
//                         responseTimes.add(responseTime);
                        
//                         if (isSuccessResponse(response)) {
//                             threadSuccessCount++;
//                             successCount.incrementAndGet();
//                         } else {
//                             threadErrorCount++;
//                             errorCount.incrementAndGet();
//                             logger.warn("Thread {} request {} failed: unexpected response", threadId, j);
//                         }
                        
//                         // Small delay between requests
//                         try {
//                             Thread.sleep(10);
//                         } catch (InterruptedException e) {
//                             Thread.currentThread().interrupt();
//                             break;
//                         }
                        
//                     } catch (Exception e) {
//                         threadErrorCount++;
//                         errorCount.incrementAndGet();
//                         logger.error("Thread {} request {} failed: {}", threadId, j, e.getMessage());
//                     }
//                 }

//                 return new TestResult(threadId, threadSuccessCount, threadErrorCount, responseTimes);
//             });
            
//             futures.add(future);
//         }

//         // Collect results
//         List<TestResult> results = new ArrayList<>();
//         for (Future<TestResult> future : futures) {
//             results.add(future.get());
//         }

//         executor.shutdown();
//         long totalTime = System.currentTimeMillis() - startTime;

//         // Analyze results
//         analyzeTestResults(results, totalTime, numberOfThreads * requestsPerThread, 
//                           successCount.get(), errorCount.get());

//         // Assertions
//         assertTrue(successCount.get() > 0, "At least some requests should succeed");
//         assertTrue(successCount.get() >= (numberOfThreads * requestsPerThread) * 0.8, 
//                   "At least 80% of requests should succeed");
        
//         logger.info("✅ Concurrent requests test completed successfully");
//     }

//     @Test
//     void testLoadTestEndpoint() throws InterruptedException, ExecutionException {
//         logger.info("🧪 Testing load test endpoint...");
        
//         int numberOfThreads = 10;
//         ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
//         List<Future<String>> futures = new ArrayList<>();
//         AtomicInteger successCount = new AtomicInteger(0);

//         // Submit load test requests
//         for (int i = 0; i < numberOfThreads; i++) {
//             Future<String> future = executor.submit(() -> {
//                 try {
//                     String response = sendHttpRequest("GET", "/load-test");
//                     if (isSuccessResponse(response)) {
//                         successCount.incrementAndGet();
//                     }
//                     return response;
//                 } catch (IOException e) {
//                     logger.error("Load test request failed: {}", e.getMessage());
//                     return null;
//                 }
//             });
//             futures.add(future);
//         }

//         // Wait for all requests to complete
//         for (Future<String> future : futures) {
//             future.get();
//         }

//         executor.shutdown();

//         // Assertions
//         assertTrue(successCount.get() > 0, "At least some load test requests should succeed");
//         logger.info("✅ Load test endpoint test completed with {} successful requests", successCount.get());
//     }

//     @Test
//     void testDifferentEndpointsConcurrently() throws InterruptedException, ExecutionException {
//         logger.info("🧪 Testing different endpoints concurrently...");
        
//         String[] endpoints = {"/", "/hello", "/time", "/cookies", "/load-test"};
//         int requestsPerEndpoint = 5;
//         ExecutorService executor = Executors.newFixedThreadPool(endpoints.length * requestsPerEndpoint);
//         List<Future<Boolean>> futures = new ArrayList<>();
//         AtomicInteger totalSuccess = new AtomicInteger(0);

//         // Submit requests for different endpoints
//         for (String endpoint : endpoints) {
//             for (int i = 0; i < requestsPerEndpoint; i++) {
//                 Future<Boolean> future = executor.submit(() -> {
//                     try {
//                         String response = sendHttpRequest("GET", endpoint);
//                         boolean success = isSuccessResponse(response);
//                         if (success) {
//                             totalSuccess.incrementAndGet();
//                         }
//                         return success;
//                     } catch (IOException e) {
//                         logger.error("Request to {} failed: {}", endpoint, e.getMessage());
//                         return false;
//                     }
//                 });
//                 futures.add(future);
//             }
//         }

//         // Wait for all requests to complete
//         for (Future<Boolean> future : futures) {
//             future.get();
//         }

//         executor.shutdown();

//         // Assertions
//         assertTrue(totalSuccess.get() > 0, "At least some endpoint requests should succeed");
//         logger.info("✅ Different endpoints test completed with {} successful requests out of {}", 
//                    totalSuccess.get(), endpoints.length * requestsPerEndpoint);
//     }

//     @Test
//     void testServerResponseTime() throws IOException {
//         logger.info("🧪 Testing server response time...");
        
//         long startTime = System.currentTimeMillis();
//         String response = sendHttpRequest("GET", "/time");
//         long endTime = System.currentTimeMillis();
//         long responseTime = endTime - startTime;

//         assertNotNull(response, "Response should not be null");
//         assertTrue(isSuccessResponse(response), "Response should be successful");
//         assertTrue(responseTime < 5000, "Response time should be less than 5 seconds");
        
//         logger.info("✅ Response time test completed. Response time: {}ms", responseTime);
//     }

//     private void analyzeTestResults(List<TestResult> results, long totalTime, int totalRequests, 
//                                    int successCount, int errorCount) {
//         logger.info("📊 Test Results Analysis:");
//         logger.info("   Total time: {}ms", totalTime);
//         logger.info("   Total requests: {}", totalRequests);
//         logger.info("   Successful requests: {}", successCount);
//         logger.info("   Failed requests: {}", errorCount);
//         logger.info("   Success rate: {:.2f}%", (successCount * 100.0) / totalRequests);
//         logger.info("   Requests per second: {:.2f}", (totalRequests * 1000.0) / totalTime);

//         // Calculate response time statistics
//         List<Long> allResponseTimes = new ArrayList<>();
//         for (TestResult result : results) {
//             allResponseTimes.addAll(result.getResponseTimes());
//         }

//         if (!allResponseTimes.isEmpty()) {
//             allResponseTimes.sort(Long::compareTo);
//             long min = allResponseTimes.get(0);
//             long max = allResponseTimes.get(allResponseTimes.size() - 1);
//             long median = allResponseTimes.get(allResponseTimes.size() / 2);
//             double avg = allResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

//             logger.info("   Response times - Min: {}ms, Max: {}ms, Median: {}ms, Avg: {:.2f}ms", 
//                        min, max, median, avg);
//         }
//     }

//     /**
//      * Test result holder class
//      */
//     private static class TestResult {
//         private final int threadId;
//         private final int successCount;
//         private final int errorCount;
//         private final List<Long> responseTimes;

//         public TestResult(int threadId, int successCount, int errorCount, List<Long> responseTimes) {
//             this.threadId = threadId;
//             this.successCount = successCount;
//             this.errorCount = errorCount;
//             this.responseTimes = new ArrayList<>(responseTimes);
//         }

//         public List<Long> getResponseTimes() {
//             return responseTimes;
//         }
//     }
// }
