package edu.eci.arsw.concurrent_ws.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP response with status code, headers, cookies, and body
 */
public class HttpResponse {
    private int statusCode;
    private String statusMessage;
    private final Map<String, String> headers;
    private final Map<String, String> cookies;
    private String body;

    public HttpResponse() {
        this.statusCode = 200;
        this.statusMessage = "OK";
        this.headers = new HashMap<>();
        this.cookies = new HashMap<>();
        this.body = "";
        
        // Set default headers
        setHeader("Server", "ConcurrentWebServer/1.0");
        setHeader("Connection", "close");
    }

    public HttpResponse(int statusCode, String statusMessage) {
        this();
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookie(String name, String value) {
        cookies.put(name, value);
    }

    public void setCookie(String name, String value, int maxAge, String path) {
        String cookieValue = value;
        if (maxAge >= 0) {
            cookieValue += "; Max-Age=" + maxAge;
        }
        if (path != null) {
            cookieValue += "; Path=" + path;
        }
        cookies.put(name, cookieValue);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body != null ? body : "";
        setHeader("Content-Length", String.valueOf(this.body.length()));
    }

    public void setJsonBody(String json) {
        setHeader("Content-Type", "application/json");
        setBody(json);
    }

    public void setHtmlBody(String html) {
        setHeader("Content-Type", "text/html; charset=UTF-8");
        setBody(html);
    }

    @Override
    public String toString() {
        return String.format("HttpResponse{statusCode=%d, statusMessage='%s'}", 
                           statusCode, statusMessage);
    }
}
