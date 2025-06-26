package edu.eci.arsw.concurrent_ws.session;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe session manager for handling user sessions
 */
@Component
public class SessionManager {
    
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
        r -> {
            Thread t = new Thread(r, "SessionCleanup");
            t.setDaemon(true);
            return t;
        }
    );
    
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    
    public SessionManager() {
        // Schedule cleanup task every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Creates a new session or returns existing one
     */
    public UserSession getOrCreateSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return createNewSession();
        }
        
        UserSession session = sessions.get(sessionId);
        if (session == null || session.isExpired()) {
            if (session != null) {
                sessions.remove(sessionId);
            }
            return createNewSession();
        }
        
        session.updateLastAccess();
        return session;
    }
    
    /**
     * Creates a new session with unique ID
     */
    public UserSession createNewSession() {
        String sessionId = generateSessionId();
        UserSession session = new UserSession(sessionId);
        sessions.put(sessionId, session);
        return session;
    }
    
    /**
     * Gets a session by ID without creating a new one
     */
    public UserSession getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        UserSession session = sessions.get(sessionId);
        if (session != null && !session.isExpired()) {
            session.updateLastAccess();
            return session;
        }
        
        return null;
    }
    
    /**
     * Removes a session
     */
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }
    
    /**
     * Gets current active session count
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Gets all active sessions (for monitoring purposes)
     */
    public List<UserSession> getAllActiveSessions() {
        return sessions.values().stream()
            .filter(session -> !session.isExpired())
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Cleans up expired sessions
     */
    private void cleanupExpiredSessions() {
        int initialSize = sessions.size();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removedCount = initialSize - sessions.size();
        if (removedCount > 0) {
            System.out.println("Cleaned up " + removedCount + " expired sessions. Active sessions: " + sessions.size());
        }
    }
    
    /**
     * Generates a unique session ID
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Shutdown cleanup executor
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupExecutor.shutdownNow();
        }
    }
    
    /**
     * Inner class representing a user session
     */
    public static class UserSession {
        private final String sessionId;
        private final long createdAt;
        private volatile long lastAccessTime;
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private final AtomicInteger visitCount = new AtomicInteger(0);
        
        public UserSession(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessTime = this.createdAt;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public void updateLastAccess() {
            this.lastAccessTime = System.currentTimeMillis();
            this.visitCount.incrementAndGet();
        }
        
        public int getVisitCount() {
            return visitCount.get();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > SESSION_TIMEOUT_MINUTES * 60 * 1000;
        }
        
        public long getSessionAgeMinutes() {
            return (System.currentTimeMillis() - createdAt) / (1000 * 60);
        }
        
        public void setAttribute(String key, Object value) {
            if (key != null) {
                if (value != null) {
                    attributes.put(key, value);
                } else {
                    attributes.remove(key);
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String key) {
            return (T) attributes.get(key);
        }
        
        public Map<String, Object> getAttributes() {
            return new ConcurrentHashMap<>(attributes);
        }
        
        public void removeAttribute(String key) {
            attributes.remove(key);
        }
        
        public void clearAttributes() {
            attributes.clear();
        }
        
        @Override
        public String toString() {
            return String.format("UserSession{id='%s', age=%d min, visits=%d}", 
                               sessionId.substring(0, 8) + "...", getSessionAgeMinutes(), visitCount.get());
        }
    }
}
