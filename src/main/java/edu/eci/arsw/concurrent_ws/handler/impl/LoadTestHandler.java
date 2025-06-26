package edu.eci.arsw.concurrent_ws.handler.impl;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import org.springframework.stereotype.Component;

/**
 * Handler that simulates heavy load to test concurrent capabilities
 */
@Component
public class LoadTestHandler implements RequestHandler {

    @Override
    public boolean canHandle(HttpRequest request) {
        return "GET".equals(request.getMethod()) && "/load-test".equals(request.getPath());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        long startTime = System.currentTimeMillis();
        
        // Simulate some processing time
        try {
            Thread.sleep(1000 + (long)(Math.random() * 2000)); // 1-3 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate some CPU-intensive work
        long result = 0;
        for (int i = 0; i < 1_000_000; i++) {
            result += Math.sqrt(i) * Math.sin(i);
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        HttpResponse response = new HttpResponse();
        
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Load Test Results</title>
                <meta charset="UTF-8">
                <style>
                    body { 
                        font-family: 'Roboto', Arial, sans-serif; 
                        margin: 40px; 
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                        background: rgba(255, 255, 255, 0.1);
                        padding: 30px;
                        border-radius: 15px;
                        backdrop-filter: blur(10px);
                        box-shadow: 0 8px 32px rgba(0,0,0,0.3);
                    }
                    h1 { 
                        text-align: center;
                        margin-bottom: 30px;
                        text-shadow: 2px 2px 4px rgba(0,0,0,0.5);
                    }
                    .metrics {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 20px;
                        margin: 30px 0;
                    }
                    .metric-card {
                        background: rgba(255, 255, 255, 0.2);
                        padding: 20px;
                        border-radius: 10px;
                        text-align: center;
                        border: 1px solid rgba(255, 255, 255, 0.3);
                    }
                    .metric-value {
                        font-size: 2.5em;
                        font-weight: bold;
                        margin-bottom: 10px;
                        text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
                    }
                    .metric-label {
                        font-size: 0.9em;
                        opacity: 0.8;
                    }
                    .info-box {
                        background: rgba(255, 255, 255, 0.15);
                        padding: 20px;
                        border-radius: 10px;
                        margin: 20px 0;
                        border-left: 4px solid #ffd700;
                    }
                    .progress-bar {
                        width: 100%%;
                        height: 20px;
                        background: rgba(255, 255, 255, 0.3);
                        border-radius: 10px;
                        overflow: hidden;
                        margin: 10px 0;
                    }
                    .progress-fill {
                        height: 100%%;
                        background: linear-gradient(90deg, #00ff88, #00cc6a);
                        width: 100%%;
                        animation: loadAnimation 2s ease-in-out;
                    }
                    @keyframes loadAnimation {
                        0%% { width: 0%%; }
                        100%% { width: 100%%; }
                    }
                    a { 
                        color: #ffd700; 
                        text-decoration: none; 
                        font-weight: bold;
                    }
                    a:hover { 
                        text-decoration: underline; 
                    }
                    .test-again {
                        text-align: center;
                        margin: 30px 0;
                    }
                    .test-btn {
                        background: linear-gradient(135deg, #ff6b6b, #ee5a24);
                        color: white;
                        padding: 15px 30px;
                        border: none;
                        border-radius: 25px;
                        font-size: 1.1em;
                        cursor: pointer;
                        text-decoration: none;
                        display: inline-block;
                        transition: transform 0.2s;
                    }
                    .test-btn:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 8px rgba(0,0,0,0.3);
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>⚡ Load Test Completed</h1>
                    
                    <div class="progress-bar">
                        <div class="progress-fill"></div>
                    </div>
                    
                    <div class="metrics">
                        <div class="metric-card">
                            <div class="metric-value">%d ms</div>
                            <div class="metric-label">Processing Time</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-value">%s</div>
                            <div class="metric-label">Thread ID</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-value">%.2f</div>
                            <div class="metric-label">Computation Result</div>
                        </div>
                        <div class="metric-card">
                            <div class="metric-value">%d</div>
                            <div class="metric-label">Iterations</div>
                        </div>
                    </div>
                    
                    <div class="info-box">
                        <h3>🔬 Test Details</h3>
                        <p><strong>Simulated Work:</strong> Sleep delay + CPU-intensive mathematical calculations</p>
                        <p><strong>Thread Pool:</strong> This request was processed concurrently with other requests</p>
                        <p><strong>Performance:</strong> Multiple users can access this endpoint simultaneously</p>
                    </div>
                    
                    <div class="test-again">
                        <a href="/load-test" class="test-btn">🔄 Run Test Again</a>
                    </div>
                    
                    <p style="text-align: center;"><a href="/">← Back to Home</a></p>
                </div>
            </body>
            </html>
            """, processingTime, Thread.currentThread().getName(), 
            result / 1000000.0, 1_000_000);

        response.setHtmlBody(html);
        return response;
    }
}
