package edu.eci.arsw.concurrent_ws.handler.impl;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handler for time endpoint that displays current server time
 */
@Component
public class TimeHandler implements RequestHandler {

    @Override
    public boolean canHandle(HttpRequest request) {
        return "GET".equals(request.getMethod()) && "/time".equals(request.getPath());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        
        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String isoTime = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Server Time</title>
                <meta charset="UTF-8">
                <style>
                    body { 
                        font-family: 'Courier New', monospace; 
                        margin: 40px; 
                        background-color: #1a1a1a; 
                        color: #00ff00;
                        text-align: center;
                    }
                    .time-display {
                        font-size: 3em;
                        margin: 40px 0;
                        text-shadow: 0 0 10px #00ff00;
                        border: 2px solid #00ff00;
                        padding: 20px;
                        border-radius: 10px;
                        background: rgba(0, 255, 0, 0.1);
                    }
                    .info {
                        font-size: 1.2em;
                        margin: 20px 0;
                        opacity: 0.8;
                    }
                    a {
                        color: #00ffff;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    .refresh-btn {
                        background: #00ff00;
                        color: #000;
                        padding: 10px 20px;
                        border: none;
                        border-radius: 5px;
                        cursor: pointer;
                        font-size: 1em;
                        margin: 10px;
                    }
                    .refresh-btn:hover {
                        background: #00cc00;
                    }
                </style>
                <script>
                    function refreshTime() {
                        window.location.reload();
                    }
                    
                    // Auto-refresh every 5 seconds
                    setTimeout(function() {
                        refreshTime();
                    }, 5000);
                </script>
            </head>
            <body>
                <h1>🕐 Server Time</h1>
                <div class="time-display">%s</div>
                <div class="info">
                    <p>ISO Format: %s</p>
                    <p>Thread: %s</p>
                    <p>Auto-refreshing in 5 seconds...</p>
                </div>
                <button class="refresh-btn" onclick="refreshTime()">Refresh Now</button>
                <br><br>
                <a href="/">← Back to Home</a>
            </body>
            </html>
            """, formattedTime, isoTime, Thread.currentThread().getName());

        response.setHtmlBody(html);
        return response;
    }
}
