package edu.eci.arsw.concurrent_ws.server;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.parser.HttpParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent web server that handles HTTP requests using thread pools
 */
@Component
public class ConcurrentWebServer {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentWebServer.class);
    
    @Value("${server.port:8080}")
    private int port;
    
    @Value("${server.thread-pool.core-size:10}")
    private int corePoolSize;
    
    @Value("${server.thread-pool.max-size:50}")
    private int maxPoolSize;
    
    @Value("${server.thread-pool.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${server.connection.timeout:30000}")
    private int connectionTimeout;
    
    private final HttpParser httpParser;
    private final List<RequestHandler> requestHandlers;
    
    private ServerSocket serverSocket;
    private ThreadPoolExecutor threadPool;
    private volatile boolean running = false;
    private Thread acceptorThread;
    private final AtomicInteger connectionCounter = new AtomicInteger(0);

    public ConcurrentWebServer(HttpParser httpParser, List<RequestHandler> requestHandlers) {
        this.httpParser = httpParser;
        this.requestHandlers = requestHandlers;
    }

    @PostConstruct
    public void start() {
        try {
            // Initialize thread pool with custom naming
            threadPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ServerThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy() // Fallback policy
            );
            
            // Create server socket
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000); // 1 second timeout for accept()
            
            running = true;
            
            // Start acceptor thread
            acceptorThread = new Thread(this::acceptConnections, "ServerAcceptor");
            acceptorThread.start();
            
            logger.info("🚀 Concurrent Web Server started successfully!");
            logger.info("📡 Listening on port: {}", port);
            logger.info("🧵 Thread pool configuration: core={}, max={}, queue={}", 
                       corePoolSize, maxPoolSize, queueCapacity);
            logger.info("🌐 Server URL: http://localhost:{}", port);
            
        } catch (IOException e) {
            logger.error("Failed to start server on port {}: {}", port, e.getMessage(), e);
            throw new RuntimeException("Failed to start server", e);
        }
    }
    
    private void acceptConnections() {
        logger.info("Server acceptor thread started, waiting for connections...");
        
        while (running) {
            try {
                // Accept client connection with timeout
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(connectionTimeout);
                
                int connectionId = connectionCounter.incrementAndGet();
                logger.debug("Accepted connection #{} from {}", 
                           connectionId, clientSocket.getRemoteSocketAddress());
                
                // Submit client handling to thread pool
                ClientHandler clientHandler = new ClientHandler(clientSocket, httpParser, requestHandlers);
                
                try {
                    threadPool.submit(clientHandler);
                    logger.debug("Connection #{} submitted to thread pool (active threads: {}, queue size: {})",
                               connectionId, threadPool.getActiveCount(), threadPool.getQueue().size());
                } catch (RejectedExecutionException e) {
                    logger.error("Thread pool rejected connection #{}: {}", connectionId, e.getMessage());
                    clientSocket.close();
                }
                
            } catch (SocketTimeoutException e) {
                // Timeout is expected, continue loop to check running status
                continue;
            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting connection: {}", e.getMessage(), e);
                }
                // Continue to next iteration
            }
        }
        
        logger.info("Server acceptor thread stopped");
    }
    
    @PreDestroy
    public void stop() {
        logger.info("Shutting down Concurrent Web Server...");
        running = false;
        
        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("Server socket closed");
            } catch (IOException e) {
                logger.error("Error closing server socket: {}", e.getMessage());
            }
        }
        
        // Wait for acceptor thread
        if (acceptorThread != null) {
            try {
                acceptorThread.join(5000);
                logger.info("Acceptor thread stopped");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for acceptor thread to stop");
            }
        }
        
        // Shutdown thread pool gracefully
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warn("Thread pool did not terminate gracefully, forcing shutdown...");
                    threadPool.shutdownNow();
                    if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.error("Thread pool did not terminate after forced shutdown");
                    }
                }
                logger.info("Thread pool shutdown completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                threadPool.shutdownNow();
                logger.warn("Interrupted while waiting for thread pool to shutdown");
            }
        }
        
        logger.info("✅ Concurrent Web Server shutdown completed");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
    
    public ThreadPoolExecutor getThreadPool() {
        return threadPool;
    }
    
    public int getActiveConnections() {
        return threadPool != null ? threadPool.getActiveCount() : 0;
    }
    
    public int getTotalConnectionsHandled() {
        return connectionCounter.get();
    }
    
    /**
     * Custom thread factory for naming server threads
     */
    private static class ServerThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private static final String NAME_PREFIX = "WebServer-Worker-";

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, NAME_PREFIX + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
