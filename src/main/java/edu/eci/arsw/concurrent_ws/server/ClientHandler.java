package edu.eci.arsw.concurrent_ws.server;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import edu.eci.arsw.concurrent_ws.parser.HttpParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Handles individual client connections in a separate thread
 */
public class ClientHandler implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    private final Socket clientSocket;
    private final HttpParser httpParser;
    private final List<RequestHandler> requestHandlers;

    public ClientHandler(Socket clientSocket, HttpParser httpParser, List<RequestHandler> requestHandlers) {
        this.clientSocket = clientSocket;
        this.httpParser = httpParser;
        this.requestHandlers = requestHandlers;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        logger.info("Handling client connection from {} on thread {}", 
                   clientAddress, Thread.currentThread().getName());
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(
                clientSocket.getOutputStream(), true)) {
            
            // Read the request
            String rawRequest = readRequest(reader);
            if (rawRequest.trim().isEmpty()) {
                logger.warn("Received empty request from {}", clientAddress);
                return;
            }
            
            logger.debug("Raw request from {}: {}", clientAddress, 
                        rawRequest.substring(0, Math.min(100, rawRequest.length())));
            
            // Parse the request
            HttpRequest request = httpParser.parseRequest(rawRequest);
            logger.info("Parsed request: {} {} from {}", 
                       request.getMethod(), request.getPath(), clientAddress);
            
            // Find appropriate handler and process request
            HttpResponse response = processRequest(request);
            
            // Send response
            String rawResponse = httpParser.formatResponse(response);
            writer.print(rawResponse);
            writer.flush();
            
            logger.info("Response sent to {} with status {} (processed by thread {})", 
                       clientAddress, response.getStatusCode(), Thread.currentThread().getName());
            
        } catch (Exception e) {
            logger.error("Error handling client {}: {}", clientAddress, e.getMessage(), e);
            sendErrorResponse(e.getMessage());
        } finally {
            closeClientSocket();
        }
    }
    
    private String readRequest(BufferedReader reader) throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        String line;
        int contentLength = 0;
        boolean isHeaderSection = true;
        
        // Read headers
        while ((line = reader.readLine()) != null) {
            requestBuilder.append(line).append("\n");
            
            if (isHeaderSection) {
                if (line.trim().isEmpty()) {
                    isHeaderSection = false;
                    break;
                }
                
                // Check for Content-Length header
                if (line.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid Content-Length header: {}", line);
                    }
                }
            }
        }
        
        // Read body if Content-Length is specified
        if (contentLength > 0) {
            char[] bodyBuffer = new char[contentLength];
            int bytesRead = reader.read(bodyBuffer, 0, contentLength);
            if (bytesRead > 0) {
                requestBuilder.append(new String(bodyBuffer, 0, bytesRead));
            }
        }
        
        return requestBuilder.toString();
    }
    
    private HttpResponse processRequest(HttpRequest request) {
        try {
            // Find the appropriate handler
            for (RequestHandler handler : requestHandlers) {
                if (handler.canHandle(request)) {
                    logger.debug("Using handler {} for request {}", 
                               handler.getClass().getSimpleName(), request.getPath());
                    return handler.handle(request);
                }
            }
            
            // No handler found - return 404
            logger.warn("No handler found for request: {} {}", request.getMethod(), request.getPath());
            return httpParser.createErrorResponse(404, 
                "The requested resource '" + request.getPath() + "' was not found on this server.");
                
        } catch (Exception e) {
            logger.error("Error processing request {}: {}", request.getPath(), e.getMessage(), e);
            return httpParser.createErrorResponse(500, 
                "Internal server error while processing request: " + e.getMessage());
        }
    }
    
    private void sendErrorResponse(String errorMessage) {
        try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            HttpResponse errorResponse = httpParser.createErrorResponse(500, errorMessage);
            String rawResponse = httpParser.formatResponse(errorResponse);
            writer.print(rawResponse);
            writer.flush();
        } catch (IOException e) {
            logger.error("Failed to send error response: {}", e.getMessage());
        }
    }
    
    private void closeClientSocket() {
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
                logger.debug("Closed connection for thread {}", Thread.currentThread().getName());
            }
        } catch (IOException e) {
            logger.error("Error closing client socket: {}", e.getMessage());
        }
    }
}
