package edu.eci.arsw.concurrent_ws.handler.impl;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler that displays request headers
 */
@Component
public class HeadersHandler implements RequestHandler {

    @Override
    public boolean canHandle(HttpRequest request) {
        return "GET".equals(request.getMethod()) && "/headers".equals(request.getPath());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        
        StringBuilder headersHtml = new StringBuilder();
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            headersHtml.append(String.format(
                "<tr><td><strong>%s</strong></td><td>%s</td></tr>", 
                escapeHtml(header.getKey()), 
                escapeHtml(header.getValue())
            ));
        }

        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Request Headers</title>
                <meta charset="UTF-8">
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 40px; 
                        background-color: #f8f9fa; 
                    }
                    .container {
                        max-width: 900px;
                        margin: 0 auto;
                        background: white;
                        padding: 20px;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    h1 { 
                        color: #343a40; 
                        border-bottom: 2px solid #007bff;
                        padding-bottom: 10px;
                    }
                    table { 
                        width: 100%%; 
                        border-collapse: collapse; 
                        margin: 20px 0;
                    }
                    th, td { 
                        border: 1px solid #ddd; 
                        padding: 12px; 
                        text-align: left; 
                    }
                    th { 
                        background-color: #007bff; 
                        color: white; 
                    }
                    tr:nth-child(even) { 
                        background-color: #f8f9fa; 
                    }
                    .info {
                        background-color: #d4edda;
                        border: 1px solid #c3e6cb;
                        color: #155724;
                        padding: 15px;
                        border-radius: 4px;
                        margin: 20px 0;
                    }
                    a { 
                        color: #007bff; 
                        text-decoration: none; 
                    }
                    a:hover { 
                        text-decoration: underline; 
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>📋 Your Request Headers</h1>
                    
                    <div class="info">
                        <strong>Request Method:</strong> %s<br>
                        <strong>Request Path:</strong> %s<br>
                        <strong>Processing Thread:</strong> %s
                    </div>
                    
                    <table>
                        <thead>
                            <tr>
                                <th>Header Name</th>
                                <th>Header Value</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                    
                    <p><a href="/">← Back to Home</a></p>
                </div>
            </body>
            </html>
            """, request.getMethod(), request.getPath(), 
            Thread.currentThread().getName(), headersHtml.toString());

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
