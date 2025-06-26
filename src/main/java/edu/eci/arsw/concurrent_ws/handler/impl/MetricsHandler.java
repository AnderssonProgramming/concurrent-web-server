package edu.eci.arsw.concurrent_ws.handler.impl;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import edu.eci.arsw.concurrent_ws.monitoring.ThreadPoolMonitor;
import edu.eci.arsw.concurrent_ws.server.ConcurrentWebServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler that provides server metrics and thread pool statistics
 */
@Component
public class MetricsHandler implements RequestHandler {
    
    private final ThreadPoolMonitor threadPoolMonitor;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    
    public MetricsHandler(ThreadPoolMonitor threadPoolMonitor, ApplicationContext applicationContext) {
        this.threadPoolMonitor = threadPoolMonitor;
        this.objectMapper = new ObjectMapper();
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        return "GET".equals(request.getMethod()) && 
               (request.getPath().equals("/metrics") || request.getPath().equals("/api/metrics"));
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        try {
            // Get the web server bean from application context to avoid circular dependency
            ConcurrentWebServer webServer = applicationContext.getBean(ConcurrentWebServer.class);
            ThreadPoolMonitor.ThreadPoolMetrics metrics = threadPoolMonitor.getMetrics(webServer.getThreadPool());
            
            // Create response based on Accept header
            String acceptHeader = request.getHeaders().get("accept");
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                return createJsonResponse(metrics, webServer);
            } else {
                return createHtmlResponse(metrics, webServer);
            }
            
        } catch (Exception e) {
            HttpResponse errorResponse = new HttpResponse();
            errorResponse.setStatusCode(500);
            errorResponse.setStatusMessage("Internal Server Error");
            errorResponse.setBody("Error generating metrics: " + e.getMessage());
            return errorResponse;
        }
    }
    
    private HttpResponse createJsonResponse(ThreadPoolMonitor.ThreadPoolMetrics metrics, ConcurrentWebServer webServer) throws Exception {
        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("threadPool", Map.of(
            "corePoolSize", metrics.getCorePoolSize(),
            "maximumPoolSize", metrics.getMaximumPoolSize(),
            "currentPoolSize", metrics.getCurrentPoolSize(),
            "activeThreads", metrics.getActiveThreads(),
            "queueSize", metrics.getQueueSize(),
            "queueCapacity", metrics.getQueueCapacity(),
            "completedTaskCount", metrics.getCompletedTaskCount(),
            "totalTaskCount", metrics.getTotalTaskCount(),
            "totalTasksExecuted", metrics.getTotalTasksExecuted(),
            "totalRejectedTasks", metrics.getTotalRejectedTasks()
        ));
        
        metricsMap.put("server", Map.of(
            "port", webServer.getPort(),
            "running", webServer.isRunning(),
            "uptimeMinutes", metrics.getUptimeMinutes(),
            "totalConnectionsHandled", webServer.getTotalConnectionsHandled()
        ));
        
        metricsMap.put("system", Map.of(
            "availableProcessors", Runtime.getRuntime().availableProcessors(),
            "totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024),
            "freeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024),
            "maxMemoryMB", Runtime.getRuntime().maxMemory() / (1024 * 1024)
        ));
        
        String jsonBody = objectMapper.writeValueAsString(metricsMap);
        
        HttpResponse response = new HttpResponse();
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.setHeader("Content-Type", "application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.setBody(jsonBody);
        
        return response;
    }
    
    private HttpResponse createHtmlResponse(ThreadPoolMonitor.ThreadPoolMetrics metrics, ConcurrentWebServer webServer) {
        long usedMemoryMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        int memoryUsagePercent = (int) ((usedMemoryMB * 100) / maxMemoryMB);
        
        int queueUsagePercent = metrics.getQueueCapacity() > 0 ? 
            (metrics.getQueueSize() * 100) / metrics.getQueueCapacity() : 0;
        int poolUsagePercent = metrics.getMaximumPoolSize() > 0 ? 
            (metrics.getCurrentPoolSize() * 100) / metrics.getMaximumPoolSize() : 0;
        
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Server Metrics - Concurrent Web Server</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 15px;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(135deg, #1e3c72 0%%, #2a5298 100%%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 { font-size: 2.5em; margin-bottom: 10px; }
                    .header p { font-size: 1.1em; opacity: 0.9; }
                    .metrics-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                        gap: 30px;
                        padding: 30px;
                    }
                    .metric-card {
                        background: #f8f9fa;
                        border-radius: 10px;
                        padding: 25px;
                        border-left: 5px solid #007bff;
                        transition: transform 0.3s ease, box-shadow 0.3s ease;
                    }
                    .metric-card:hover {
                        transform: translateY(-5px);
                        box-shadow: 0 10px 25px rgba(0,0,0,0.1);
                    }
                    .metric-card h3 {
                        color: #2c3e50;
                        margin-bottom: 20px;
                        font-size: 1.3em;
                        display: flex;
                        align-items: center;
                    }
                    .metric-card h3::before {
                        content: '';
                        width: 8px;
                        height: 8px;
                        background: #007bff;
                        border-radius: 50%%;
                        margin-right: 10px;
                    }
                    .metric-row {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 15px;
                        padding: 10px 0;
                        border-bottom: 1px solid #e9ecef;
                    }
                    .metric-row:last-child { border-bottom: none; margin-bottom: 0; }
                    .metric-label {
                        font-weight: 600;
                        color: #495057;
                    }
                    .metric-value {
                        font-size: 1.1em;
                        font-weight: bold;
                        color: #007bff;
                    }
                    .progress-bar {
                        width: 100%%;
                        height: 8px;
                        background: #e9ecef;
                        border-radius: 4px;
                        overflow: hidden;
                        margin-top: 5px;
                    }
                    .progress-fill {
                        height: 100%%;
                        background: linear-gradient(90deg, #28a745, #20c997);
                        transition: width 0.3s ease;
                    }
                    .status-badge {
                        display: inline-block;
                        padding: 4px 12px;
                        border-radius: 20px;
                        font-size: 0.85em;
                        font-weight: bold;
                        text-transform: uppercase;
                    }
                    .status-running {
                        background: #d4edda;
                        color: #155724;
                    }
                    .status-stopped {
                        background: #f8d7da;
                        color: #721c24;
                    }
                    .refresh-btn {
                        position: fixed;
                        bottom: 30px;
                        right: 30px;
                        background: #007bff;
                        color: white;
                        border: none;
                        border-radius: 50px;
                        padding: 15px 25px;
                        font-size: 16px;
                        cursor: pointer;
                        box-shadow: 0 5px 15px rgba(0,123,255,0.3);
                        transition: all 0.3s ease;
                    }
                    .refresh-btn:hover {
                        background: #0056b3;
                        transform: translateY(-2px);
                        box-shadow: 0 8px 25px rgba(0,123,255,0.4);
                    }
                    @media (max-width: 768px) {
                        .metrics-grid { grid-template-columns: 1fr; padding: 20px; }
                        .header { padding: 20px; }
                        .header h1 { font-size: 2em; }
                    }
                </style>
                <script>
                    function refreshMetrics() {
                        window.location.reload();
                    }
                    
                    // Auto-refresh every 30 seconds
                    setInterval(refreshMetrics, 30000);
                </script>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚀 Server Metrics Dashboard</h1>
                        <p>Real-time monitoring of concurrent web server performance</p>
                    </div>
                    
                    <div class="metrics-grid">
                        <!-- Server Status -->
                        <div class="metric-card">
                            <h3>🌐 Server Status</h3>
                            <div class="metric-row">
                                <span class="metric-label">Status:</span>
                                <span class="status-badge %s">%s</span>
                            </div>
                            <div class="metric-row">
                                <span class="metric-label">Port:</span>
                                <span class="metric-value">%d</span>
                            </div>
                            <div class="metric-row">
                                <span class="metric-label">Uptime:</span>
                                <span class="metric-value">%d minutes</span>
                            </div>
                            <div class="metric-row">
                                <span class="metric-label">Total Connections:</span>
                                <span class="metric-value">%d</span>
                            </div>
                        </div>
                        
                        <!-- Thread Pool -->
                        <div class="metric-card">
                            <h3>🧵 Thread Pool</h3>
                            <div class="metric-row">
                                <span class="metric-label">Pool Size:</span>
                                <span class="metric-value">%d / %d</span>
                            </div>
                            <div class="progress-bar">
                                <div class="progress-fill" style="width: %d%%;"></div>
                            </div>
                            <div class="metric-row">
                                <span class="metric-label">Active Threads:</span>
                                <span class="metric-value">%d</span>
                            </div>
                            <div class="metric-row">
                                <span class="metric-label">Completed Tasks:</span>
                                <span class="metric-value">%d</span>
                            </div>
                        </div>
                        
                        <!-- Queue Status -->
                        <div class="metric-card">
                            <h3>📋 Task Queue</h3>
                            <div class="metric-row">
                                <span class="metric-label">Queue Usage:</span>
                                <span class="metric-value">%d / %d</span>
                            </div>
                            <div class="progress-bar">
                                <div class="progress-fill" style="width: %d%%;"></div>
                            </div>
                            <div class="metric-row">
                                <span class="metric-label">Total Tasks:</span>
                                <span class="metric-value">%d</span>
                            </div>
                            <div class="metric-row">
                                <span class="metric-label">Rejected Tasks:</span>
                                <span class="metric-value">%d</span>
                            </div>
                        </div>
                        
                        <!-- System Resources -->
                        <div class="metric-card">
                            <h3>💻 System Resources</h3>
                            <div class="metric-row">
                                <span class="metric-label">Memory Usage:</span>
                                <span class="metric-value">%d / %d MB</span>
                            </div>
                            <div class="progress-bar">
                                <div class="progress-fill" style="width: %d%%;"></div>
                            </div>
                            <div class="metric-row">
                                <span class="metric-label">Available CPUs:</span>
                                <span class="metric-value">%d</span>
                            </div>
                        </div>
                    </div>
                </div>
                
                <button class="refresh-btn" onclick="refreshMetrics()">
                    🔄 Refresh
                </button>
            </body>
            </html>
            """, 
            webServer.isRunning() ? "status-running" : "status-stopped",
            webServer.isRunning() ? "Running" : "Stopped",
            webServer.getPort(),
            metrics.getUptimeMinutes(),
            webServer.getTotalConnectionsHandled(),
            metrics.getCurrentPoolSize(), metrics.getMaximumPoolSize(),
            poolUsagePercent,
            metrics.getActiveThreads(),
            metrics.getCompletedTaskCount(),
            metrics.getQueueSize(), metrics.getQueueCapacity(),
            queueUsagePercent,
            metrics.getTotalTaskCount(),
            metrics.getTotalRejectedTasks(),
            usedMemoryMB, maxMemoryMB,
            memoryUsagePercent,
            Runtime.getRuntime().availableProcessors()
        );
        
        HttpResponse response = new HttpResponse();
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setBody(html);
        
        return response;
    }
}
