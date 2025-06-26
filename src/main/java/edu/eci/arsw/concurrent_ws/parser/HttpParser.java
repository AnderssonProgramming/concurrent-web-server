package edu.eci.arsw.concurrent_ws.parser;

import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for parsing HTTP requests and formatting HTTP responses
 */
@Component
public class HttpParser {

    /**
     * Parses a raw HTTP request string into an HttpRequest object
     */
    public HttpRequest parseRequest(String rawRequest) throws IOException {
        if (rawRequest == null || rawRequest.trim().isEmpty()) {
            throw new IllegalArgumentException("Request cannot be null or empty");
        }

        BufferedReader reader = new BufferedReader(new StringReader(rawRequest));
        
        // Parse request line
        String requestLine = reader.readLine();
        if (requestLine == null) {
            throw new IllegalArgumentException("Invalid HTTP request: missing request line");
        }

        String[] requestParts = requestLine.split(" ");
        if (requestParts.length != 3) {
            throw new IllegalArgumentException("Invalid HTTP request line: " + requestLine);
        }

        String method = requestParts[0];
        String path = requestParts[1];
        String httpVersion = requestParts[2];

        // Parse headers
        Map<String, String> headers = new HashMap<>();
        Map<String, String> cookies = new HashMap<>();
        String line;
        
        while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String headerName = line.substring(0, colonIndex).trim().toLowerCase();
                String headerValue = line.substring(colonIndex + 1).trim();
                
                headers.put(headerName, headerValue);
                
                // Parse cookies from Cookie header
                if ("cookie".equals(headerName)) {
                    parseCookies(headerValue, cookies);
                }
            }
        }

        // Parse body (if any)
        StringBuilder bodyBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            bodyBuilder.append(line).append("\n");
        }
        String body = bodyBuilder.toString().trim();

        return new HttpRequest(method, path, httpVersion, headers, cookies, body);
    }

    /**
     * Formats an HttpResponse into a raw HTTP response string
     */
    public String formatResponse(HttpResponse response) {
        StringBuilder responseBuilder = new StringBuilder();
        
        // Status line
        responseBuilder.append("HTTP/1.1 ")
                      .append(response.getStatusCode())
                      .append(" ")
                      .append(response.getStatusMessage())
                      .append("\r\n");

        // Headers
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            responseBuilder.append(header.getKey())
                          .append(": ")
                          .append(header.getValue())
                          .append("\r\n");
        }

        // Cookies
        for (Map.Entry<String, String> cookie : response.getCookies().entrySet()) {
            responseBuilder.append("Set-Cookie: ")
                          .append(cookie.getKey())
                          .append("=")
                          .append(cookie.getValue())
                          .append("\r\n");
        }

        // Empty line before body
        responseBuilder.append("\r\n");

        // Body
        if (response.getBody() != null && !response.getBody().isEmpty()) {
            responseBuilder.append(response.getBody());
        }

        return responseBuilder.toString();
    }

    /**
     * Parses cookies from a Cookie header value
     */
    private void parseCookies(String cookieHeader, Map<String, String> cookies) {
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) {
            return;
        }

        String[] cookiePairs = cookieHeader.split(";");
        for (String pair : cookiePairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length == 2) {
                cookies.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
    }

    /**
     * Creates a simple error response
     */
    public HttpResponse createErrorResponse(int statusCode, String message) {
        HttpResponse response = new HttpResponse(statusCode, getStatusMessage(statusCode));
        response.setHtmlBody(createErrorHtml(statusCode, message));
        return response;
    }

    /**
     * Gets the standard HTTP status message for a status code
     */
    private String getStatusMessage(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Unknown Status";
        };
    }

    /**
     * Creates a simple HTML error page
     */
    private String createErrorHtml(int statusCode, String message) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Error %d</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .error { color: #d32f2f; }
                </style>
            </head>
            <body>
                <h1 class="error">Error %d</h1>
                <p>%s</p>
                <hr>
                <small>ConcurrentWebServer/1.0</small>
            </body>
            </html>
            """, statusCode, statusCode, message);
    }
}
