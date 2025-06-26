package edu.eci.arsw.concurrent_ws.handler.impl;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handler for the root path that displays a welcome page with server information
 */
@Component
public class HomePageHandler implements RequestHandler {

    @Override
    public boolean canHandle(HttpRequest request) {
        return "GET".equals(request.getMethod()) && "/".equals(request.getPath());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String sessionId = request.getCookie("sessionId");
        
        if (sessionId == null) {
            sessionId = generateSessionId();
            response.setCookie("sessionId", sessionId, 3600, "/");
        }

        String html = createWelcomeHtml(currentTime, sessionId);
        response.setHtmlBody(html);
        
        return response;
    }

    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 10000);
    }

    private String createWelcomeHtml(String currentTime, String sessionId) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Concurrent Web Server</title>
                <meta charset="UTF-8">
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 40px; 
                        background-color: #f5f5f5; 
                    }
                    .container { 
                        max-width: 800px; 
                        margin: 0 auto; 
                        background: white; 
                        padding: 20px; 
                        border-radius: 8px; 
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1); 
                    }
                    .header { 
                        color: #2196F3; 
                        border-bottom: 2px solid #2196F3; 
                        padding-bottom: 10px; 
                    }
                    .info { 
                        background-color: #e3f2fd; 
                        padding: 15px; 
                        border-radius: 4px; 
                        margin: 20px 0; 
                    }
                    .endpoints { 
                        margin: 20px 0; 
                    }
                    .endpoint { 
                        background-color: #f9f9f9; 
                        padding: 10px; 
                        margin: 5px 0; 
                        border-left: 4px solid #4CAF50; 
                    }
                    a { 
                        color: #2196F3; 
                        text-decoration: none; 
                    }
                    a:hover { 
                        text-decoration: underline; 
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1 class="header">🚀 Concurrent Web Server</h1>
                    
                    <div class="info">
                        <h3>Server Information</h3>
                        <p><strong>Current Time:</strong> %s</p>
                        <p><strong>Your Session ID:</strong> %s</p>
                        <p><strong>Server:</strong> ConcurrentWebServer/1.0</p>
                        <p><strong>Thread Pool:</strong> Active and Ready</p>
                    </div>
                    
                    <div class="endpoints">
                        <h3>Available Endpoints</h3>
                        <div class="endpoint">
                            <strong>GET /</strong> - This welcome page
                        </div>
                        <div class="endpoint">
                            <strong>GET /hello</strong> - <a href="/hello">Simple greeting</a>
                        </div>
                        <div class="endpoint">
                            <strong>GET /time</strong> - <a href="/time">Current server time</a>
                        </div>
                        <div class="endpoint">
                            <strong>GET /headers</strong> - <a href="/headers">View your request headers</a>
                        </div>
                        <div class="endpoint">
                            <strong>GET /cookies</strong> - <a href="/cookies">View your cookies</a>
                        </div>
                        <div class="endpoint">
                            <strong>GET /load-test</strong> - <a href="/load-test">Simulate heavy load</a>
                        </div>
                    </div>
                    
                    <p><em>This server demonstrates concurrent request handling using thread pools.</em></p>
                </div>
            </body>
            </html>
            """, currentTime, sessionId);
    }
}
