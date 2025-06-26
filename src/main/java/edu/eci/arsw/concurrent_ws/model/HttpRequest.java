package edu.eci.arsw.concurrent_ws.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an HTTP request with method, path, headers, and cookies
 */
public class HttpRequest {
    private final String method;
    private final String path;
    private final String httpVersion;
    private final Map<String, String> headers;
    private final Map<String, String> cookies;
    private final String body;

    public HttpRequest(String method, String path, String httpVersion, 
                      Map<String, String> headers, Map<String, String> cookies, String body) {
        this.method = method;
        this.path = path;
        this.httpVersion = httpVersion;
        this.headers = headers != null ? headers : new HashMap<>();
        this.cookies = cookies != null ? cookies : new HashMap<>();
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getBody() {
        return body;
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    public String getCookie(String name) {
        return cookies.get(name);
    }

    @Override
    public String toString() {
        return String.format("HttpRequest{method='%s', path='%s', httpVersion='%s'}", 
                           method, path, httpVersion);
    }
}
