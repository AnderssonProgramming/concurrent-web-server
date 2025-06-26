package edu.eci.arsw.concurrent_ws.handler;

import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;

/**
 * Interface for handling HTTP requests
 */
public interface RequestHandler {
    
    /**
     * Handles an HTTP request and returns an HTTP response
     * 
     * @param request the HTTP request to handle
     * @return the HTTP response
     */
    HttpResponse handle(HttpRequest request);
    
    /**
     * Checks if this handler can handle the given request
     * 
     * @param request the HTTP request
     * @return true if this handler can handle the request, false otherwise
     */
    boolean canHandle(HttpRequest request);
}
