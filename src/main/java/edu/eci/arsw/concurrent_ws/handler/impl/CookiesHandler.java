package edu.eci.arsw.concurrent_ws.handler.impl;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler that displays cookies and manages session state
 */
@Component
public class CookiesHandler implements RequestHandler {

    @Override
    public boolean canHandle(HttpRequest request) {
        return "GET".equals(request.getMethod()) && "/cookies".equals(request.getPath());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        
        // Set a test cookie if it doesn't exist
        String visitCount = request.getCookie("visitCount");
        int visits = 1;
        if (visitCount != null) {
            try {
                visits = Integer.parseInt(visitCount) + 1;
            } catch (NumberFormatException e) {
                visits = 1;
            }
        }
        response.setCookie("visitCount", String.valueOf(visits), 3600, "/");
        
        // Set a timestamp cookie
        response.setCookie("lastVisit", String.valueOf(System.currentTimeMillis()), 3600, "/");

        StringBuilder cookiesHtml = new StringBuilder();
        if (request.getCookies().isEmpty()) {
            cookiesHtml.append("<tr><td colspan=\"2\"><em>No cookies found</em></td></tr>");
        } else {
            for (Map.Entry<String, String> cookie : request.getCookies().entrySet()) {
                cookiesHtml.append(String.format(
                    "<tr><td><strong>%s</strong></td><td>%s</td></tr>", 
                    escapeHtml(cookie.getKey()), 
                    escapeHtml(cookie.getValue())
                ));
            }
        }

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Cookies Management</title>
                <meta charset="UTF-8">
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 40px; 
                        background: linear-gradient(135deg, #ff9a9e 0%%, #fecfef 100%%);
                        min-height: 100vh;
                    }
                    .container {
                        max-width: 900px;
                        margin: 0 auto;
                        background: rgba(255, 255, 255, 0.95);
                        padding: 20px;
                        border-radius: 15px;
                        box-shadow: 0 8px 32px rgba(0,0,0,0.1);
                        backdrop-filter: blur(10px);
                    }
                    h1 { 
                        color: #d63384; 
                        text-align: center;
                        border-bottom: 2px solid #d63384;
                        padding-bottom: 10px;
                    }
                    table { 
                        width: 100%%; 
                        border-collapse: collapse; 
                        margin: 20px 0;
                        border-radius: 8px;
                        overflow: hidden;
                    }
                    th, td { 
                        border: 1px solid #ddd; 
                        padding: 12px; 
                        text-align: left; 
                    }
                    th { 
                        background: linear-gradient(135deg, #d63384, #f8d7da);
                        color: white; 
                    }
                    tr:nth-child(even) { 
                        background-color: #fdf2f8; 
                    }
                    .info {
                        background: linear-gradient(135deg, #d1ecf1, #bee5eb);
                        border: 1px solid #b8daff;
                        color: #0c5460;
                        padding: 15px;
                        border-radius: 8px;
                        margin: 20px 0;
                    }
                    .stats {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 15px;
                        margin: 20px 0;
                    }
                    .stat-card {
                        background: linear-gradient(135deg, #667eea, #764ba2);
                        color: white;
                        padding: 20px;
                        border-radius: 10px;
                        text-align: center;
                    }
                    .stat-value {
                        font-size: 2em;
                        font-weight: bold;
                    }
                    a { 
                        color: #d63384; 
                        text-decoration: none; 
                        font-weight: bold;
                    }
                    a:hover { 
                        text-decoration: underline; 
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🍪 Cookies & Session Management</h1>
                    
                    <div class="stats">
                        <div class="stat-card">
                            <div class="stat-value">%d</div>
                            <div>Visit Count</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">%d</div>
                            <div>Active Cookies</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-value">%s</div>
                            <div>Current Thread</div>
                        </div>
                    </div>
                    
                    <div class="info">
                        <strong>Note:</strong> This page demonstrates cookie handling and session management. 
                        Each visit increments your visit counter, and new cookies are set with each request.
                    </div>
                    
                    <h3>Current Cookies</h3>
                    <table>
                        <thead>
                            <tr>
                                <th>Cookie Name</th>
                                <th>Cookie Value</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                    
                    <p><a href="/cookies">🔄 Refresh Page</a> | <a href="/">← Back to Home</a></p>
                </div>
            </body>
            </html>
            """, visits, request.getCookies().size(), 
            Thread.currentThread().getName(), cookiesHtml.toString());

        response.setHtmlBody(html);
        return response;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
}
