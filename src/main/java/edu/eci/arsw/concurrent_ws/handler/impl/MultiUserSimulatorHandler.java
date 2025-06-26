package edu.eci.arsw.concurrent_ws.handler.impl;

import edu.eci.arsw.concurrent_ws.handler.RequestHandler;
import edu.eci.arsw.concurrent_ws.model.HttpRequest;
import edu.eci.arsw.concurrent_ws.model.HttpResponse;
import edu.eci.arsw.concurrent_ws.session.SessionManager;
import edu.eci.arsw.concurrent_ws.session.SessionManager.UserSession;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handler that simulates multiple concurrent users with session management
 */
@Component
public class MultiUserSimulatorHandler implements RequestHandler {
    
    private final SessionManager sessionManager;
    
    public MultiUserSimulatorHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean canHandle(HttpRequest request) {
        return "GET".equals(request.getMethod()) && "/users".equals(request.getPath());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        // Get or create session
        String sessionId = request.getCookie("JSESSIONID");
        UserSession session = sessionManager.getOrCreateSession(sessionId);
        
        // Update session attributes
        session.setAttribute("userAgent", request.getHeaders().get("user-agent"));
        session.setAttribute("lastPath", request.getPath());
        session.setAttribute("thread", Thread.currentThread().getName());
        
        // Simulate user activity
        String userName = session.getAttribute("userName");
        if (userName == null) {
            userName = "User_" + session.getSessionId().substring(0, 8);
            session.setAttribute("userName", userName);
        }
        
        // Get all active sessions for display
        List<UserSession> activeSessions = getAllActiveSessions();
        
        String html = generateMultiUserHtml(session, activeSessions);
        
        HttpResponse response = new HttpResponse();
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        
        // Set session cookie if new session
        if (!session.getSessionId().equals(sessionId)) {
            response.setCookie("JSESSIONID", session.getSessionId(), 1800, "/"); // 30 minutes
        }
        
        response.setBody(html);
        return response;
    }
    
    private List<UserSession> getAllActiveSessions() {
        // This is a simplified approach. In a real application, you'd need 
        // a more sophisticated way to get all sessions
        return sessionManager.getAllActiveSessions(); // We'll need to add this method
    }
    
    private String generateMultiUserHtml(UserSession currentSession, List<UserSession> allSessions) {
        String currentUserName = currentSession.getAttribute("userName");
        String lastThread = currentSession.getAttribute("thread");
        
        // Generate active users table
        StringBuilder usersTable = new StringBuilder();
        for (UserSession session : allSessions) {
            String userName = session.getAttribute("userName");
            String thread = session.getAttribute("thread");
            
            LocalDateTime createdTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(session.getCreatedAt()), ZoneId.systemDefault());
            LocalDateTime lastAccessTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(session.getLastAccessTime()), ZoneId.systemDefault());
            
            boolean isCurrentUser = session.getSessionId().equals(currentSession.getSessionId());
            String rowClass = isCurrentUser ? "current-user" : "";
            
            usersTable.append(String.format("""
                <tr class="%s">
                    <td>%s %s</td>
                    <td>%s</td>
                    <td>%d</td>
                    <td>%d min</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """,
                rowClass,
                escapeHtml(userName != null ? userName : "Anonymous"),
                isCurrentUser ? "<span class=\"you-badge\">YOU</span>" : "",
                session.getSessionId().substring(0, 8) + "...",
                session.getVisitCount(),
                session.getSessionAgeMinutes(),
                createdTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                lastAccessTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                escapeHtml(thread != null ? thread : "N/A")
            ));
        }
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Multi-User Simulator - Concurrent Web Server</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 1400px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 15px;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(135deg, #1e3c72 0%%, #2a5298 100%%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 { font-size: 2.5em; margin-bottom: 10px; }
                    .header p { font-size: 1.1em; opacity: 0.9; }
                    .content {
                        padding: 30px;
                    }
                    .user-info {
                        background: #f8f9fa;
                        border-radius: 10px;
                        padding: 20px;
                        margin-bottom: 30px;
                        border-left: 5px solid #28a745;
                    }
                    .user-info h3 {
                        color: #2c3e50;
                        margin-bottom: 15px;
                        display: flex;
                        align-items: center;
                    }
                    .user-info h3::before {
                        content: '👤';
                        margin-right: 10px;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 15px;
                    }
                    .info-item {
                        display: flex;
                        flex-direction: column;
                    }
                    .info-label {
                        font-size: 0.9em;
                        color: #6c757d;
                        font-weight: 600;
                        margin-bottom: 5px;
                    }
                    .info-value {
                        font-size: 1.1em;
                        color: #495057;
                        font-weight: bold;
                    }
                    .users-section h3 {
                        color: #2c3e50;
                        margin-bottom: 20px;
                        font-size: 1.5em;
                        display: flex;
                        align-items: center;
                    }
                    .users-section h3::before {
                        content: '👥';
                        margin-right: 10px;
                    }
                    .users-table {
                        width: 100%%;
                        border-collapse: collapse;
                        background: white;
                        border-radius: 10px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                    }
                    .users-table th {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 15px 12px;
                        text-align: left;
                        font-weight: 600;
                        font-size: 0.9em;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    .users-table td {
                        padding: 12px;
                        border-bottom: 1px solid #e9ecef;
                    }
                    .users-table tr:hover {
                        background: #f8f9fa;
                    }
                    .current-user {
                        background: #e8f5e8 !important;
                        font-weight: 600;
                    }
                    .current-user:hover {
                        background: #d4edda !important;
                    }
                    .you-badge {
                        background: #28a745;
                        color: white;
                        padding: 2px 8px;
                        border-radius: 12px;
                        font-size: 0.7em;
                        font-weight: bold;
                        margin-left: 8px;
                    }
                    .stats-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                        gap: 20px;
                        margin-bottom: 30px;
                    }
                    .stat-card {
                        background: #f8f9fa;
                        border-radius: 10px;
                        padding: 20px;
                        text-align: center;
                        border-top: 4px solid #007bff;
                    }
                    .stat-number {
                        font-size: 2em;
                        font-weight: bold;
                        color: #007bff;
                        margin-bottom: 5px;
                    }
                    .stat-label {
                        color: #6c757d;
                        font-size: 0.9em;
                        font-weight: 600;
                    }
                    .refresh-btn {
                        position: fixed;
                        bottom: 30px;
                        right: 30px;
                        background: #007bff;
                        color: white;
                        border: none;
                        border-radius: 50px;
                        padding: 15px 25px;
                        font-size: 16px;
                        cursor: pointer;
                        box-shadow: 0 5px 15px rgba(0,123,255,0.3);
                        transition: all 0.3s ease;
                    }
                    .refresh-btn:hover {
                        background: #0056b3;
                        transform: translateY(-2px);
                        box-shadow: 0 8px 25px rgba(0,123,255,0.4);
                    }
                    @media (max-width: 768px) {
                        .container { margin: 10px; }
                        .header { padding: 20px; }
                        .header h1 { font-size: 2em; }
                        .content { padding: 20px; }
                        .users-table { font-size: 0.9em; }
                        .users-table th, .users-table td { padding: 8px; }
                    }
                </style>
                <script>
                    function refreshPage() {
                        window.location.reload();
                    }
                    
                    // Auto-refresh every 10 seconds
                    setInterval(refreshPage, 10000);
                    
                    // Update time display
                    function updateTime() {
                        document.getElementById('current-time').textContent = new Date().toLocaleTimeString();
                    }
                    setInterval(updateTime, 1000);
                    updateTime();
                </script>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>👥 Multi-User Simulator</h1>
                        <p>Demonstrating concurrent session management</p>
                    </div>
                    
                    <div class="content">
                        <!-- Current User Info -->
                        <div class="user-info">
                            <h3>Your Session Information</h3>
                            <div class="info-grid">
                                <div class="info-item">
                                    <span class="info-label">User Name</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">Session ID</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">Visit Count</span>
                                    <span class="info-value">%d</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">Session Age</span>
                                    <span class="info-value">%d minutes</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">Handler Thread</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-item">
                                    <span class="info-label">Current Time</span>
                                    <span class="info-value" id="current-time">--:--:--</span>
                                </div>
                            </div>
                        </div>
                        
                        <!-- Statistics -->
                        <div class="stats-grid">
                            <div class="stat-card">
                                <div class="stat-number">%d</div>
                                <div class="stat-label">Active Users</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-number">%d</div>
                                <div class="stat-label">Total Visits</div>
                            </div>
                        </div>
                        
                        <!-- Active Users Table -->
                        <div class="users-section">
                            <h3>Active Users</h3>
                            <table class="users-table">
                                <thead>
                                    <tr>
                                        <th>User Name</th>
                                        <th>Session ID</th>
                                        <th>Visits</th>
                                        <th>Age (min)</th>
                                        <th>Created</th>
                                        <th>Last Access</th>
                                        <th>Thread</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    %s
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
                
                <button class="refresh-btn" onclick="refreshPage()">
                    🔄 Refresh
                </button>
            </body>
            </html>
            """,
            escapeHtml(currentUserName),
            currentSession.getSessionId().substring(0, 8) + "...",
            currentSession.getVisitCount(),
            currentSession.getSessionAgeMinutes(),
            escapeHtml(lastThread != null ? lastThread : "N/A"),
            allSessions.size(),
            allSessions.stream().mapToInt(UserSession::getVisitCount).sum(),
            usersTable.toString()
        );
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
