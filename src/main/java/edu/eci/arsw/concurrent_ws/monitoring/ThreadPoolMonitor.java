package edu.eci.arsw.concurrent_ws.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors thread pool performance and provides metrics
 */
@Component
public class ThreadPoolMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolMonitor.class);
    
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicLong totalRejectedTasks = new AtomicLong(0);
    private volatile long monitoringStartTime = System.currentTimeMillis();
    
    /**
     * Gets comprehensive thread pool metrics
     */
    public ThreadPoolMetrics getMetrics(ThreadPoolExecutor threadPool) {
        if (threadPool == null) {
            return new ThreadPoolMetrics();
        }
        
        long currentTime = System.currentTimeMillis();
        long uptimeMinutes = (currentTime - monitoringStartTime) / (1000 * 60);
        
        return ThreadPoolMetrics.builder()
            .corePoolSize(threadPool.getCorePoolSize())
            .maximumPoolSize(threadPool.getMaximumPoolSize())
            .currentPoolSize(threadPool.getPoolSize())
            .activeThreads(threadPool.getActiveCount())
            .queueSize(threadPool.getQueue().size())
            .queueCapacity(threadPool.getQueue().size() + threadPool.getQueue().remainingCapacity())
            .completedTaskCount(threadPool.getCompletedTaskCount())
            .totalTaskCount(threadPool.getTaskCount())
            .totalTasksExecuted(totalTasksExecuted.get())
            .totalRejectedTasks(totalRejectedTasks.get())
            .uptimeMinutes(uptimeMinutes)
            .build();
    }
    
    /**
     * Logs thread pool performance metrics
     */
    public void logMetrics(ThreadPoolExecutor threadPool) {
        ThreadPoolMetrics metrics = getMetrics(threadPool);
        
        logger.info("📊 Thread Pool Metrics:");
        logger.info("   🧵 Pool Size: {}/{} (core/max)", metrics.getCorePoolSize(), metrics.getMaximumPoolSize());
        logger.info("   🔥 Active Threads: {}/{}", metrics.getActiveThreads(), metrics.getCurrentPoolSize());
        logger.info("   📋 Queue: {}/{} (used/capacity)", metrics.getQueueSize(), metrics.getQueueCapacity());
        logger.info("   ✅ Completed Tasks: {}", metrics.getCompletedTaskCount());
        logger.info("   ⏱️ Uptime: {} minutes", metrics.getUptimeMinutes());
        
        if (metrics.getTotalRejectedTasks() > 0) {
            logger.warn("   ❌ Rejected Tasks: {}", metrics.getTotalRejectedTasks());
        }
    }
    
    /**
     * Increments the task execution counter
     */
    public void incrementTasksExecuted() {
        totalTasksExecuted.incrementAndGet();
    }
    
    /**
     * Increments the rejected task counter
     */
    public void incrementRejectedTasks() {
        totalRejectedTasks.incrementAndGet();
    }
    
    /**
     * Resets monitoring counters
     */
    public void resetCounters() {
        totalTasksExecuted.set(0);
        totalRejectedTasks.set(0);
        monitoringStartTime = System.currentTimeMillis();
        logger.info("Thread pool monitoring counters reset");
    }
    
    /**
     * Immutable metrics data class
     */
    public static class ThreadPoolMetrics {
        private final int corePoolSize;
        private final int maximumPoolSize;
        private final int currentPoolSize;
        private final int activeThreads;
        private final int queueSize;
        private final int queueCapacity;
        private final long completedTaskCount;
        private final long totalTaskCount;
        private final long totalTasksExecuted;
        private final long totalRejectedTasks;
        private final long uptimeMinutes;
        
        private ThreadPoolMetrics() {
            this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        
        private ThreadPoolMetrics(int corePoolSize, int maximumPoolSize, int currentPoolSize,
                                 int activeThreads, int queueSize, int queueCapacity,
                                 long completedTaskCount, long totalTaskCount,
                                 long totalTasksExecuted, long totalRejectedTasks,
                                 long uptimeMinutes) {
            this.corePoolSize = corePoolSize;
            this.maximumPoolSize = maximumPoolSize;
            this.currentPoolSize = currentPoolSize;
            this.activeThreads = activeThreads;
            this.queueSize = queueSize;
            this.queueCapacity = queueCapacity;
            this.completedTaskCount = completedTaskCount;
            this.totalTaskCount = totalTaskCount;
            this.totalTasksExecuted = totalTasksExecuted;
            this.totalRejectedTasks = totalRejectedTasks;
            this.uptimeMinutes = uptimeMinutes;
        }
        
        public static ThreadPoolMetricsBuilder builder() {
            return new ThreadPoolMetricsBuilder();
        }
        
        // Getters
        public int getCorePoolSize() { return corePoolSize; }
        public int getMaximumPoolSize() { return maximumPoolSize; }
        public int getCurrentPoolSize() { return currentPoolSize; }
        public int getActiveThreads() { return activeThreads; }
        public int getQueueSize() { return queueSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public long getCompletedTaskCount() { return completedTaskCount; }
        public long getTotalTaskCount() { return totalTaskCount; }
        public long getTotalTasksExecuted() { return totalTasksExecuted; }
        public long getTotalRejectedTasks() { return totalRejectedTasks; }
        public long getUptimeMinutes() { return uptimeMinutes; }
        
        /**
         * Builder pattern for ThreadPoolMetrics
         */
        public static class ThreadPoolMetricsBuilder {
            private int corePoolSize;
            private int maximumPoolSize;
            private int currentPoolSize;
            private int activeThreads;
            private int queueSize;
            private int queueCapacity;
            private long completedTaskCount;
            private long totalTaskCount;
            private long totalTasksExecuted;
            private long totalRejectedTasks;
            private long uptimeMinutes;
            
            public ThreadPoolMetricsBuilder corePoolSize(int corePoolSize) {
                this.corePoolSize = corePoolSize;
                return this;
            }
            
            public ThreadPoolMetricsBuilder maximumPoolSize(int maximumPoolSize) {
                this.maximumPoolSize = maximumPoolSize;
                return this;
            }
            
            public ThreadPoolMetricsBuilder currentPoolSize(int currentPoolSize) {
                this.currentPoolSize = currentPoolSize;
                return this;
            }
            
            public ThreadPoolMetricsBuilder activeThreads(int activeThreads) {
                this.activeThreads = activeThreads;
                return this;
            }
            
            public ThreadPoolMetricsBuilder queueSize(int queueSize) {
                this.queueSize = queueSize;
                return this;
            }
            
            public ThreadPoolMetricsBuilder queueCapacity(int queueCapacity) {
                this.queueCapacity = queueCapacity;
                return this;
            }
            
            public ThreadPoolMetricsBuilder completedTaskCount(long completedTaskCount) {
                this.completedTaskCount = completedTaskCount;
                return this;
            }
            
            public ThreadPoolMetricsBuilder totalTaskCount(long totalTaskCount) {
                this.totalTaskCount = totalTaskCount;
                return this;
            }
            
            public ThreadPoolMetricsBuilder totalTasksExecuted(long totalTasksExecuted) {
                this.totalTasksExecuted = totalTasksExecuted;
                return this;
            }
            
            public ThreadPoolMetricsBuilder totalRejectedTasks(long totalRejectedTasks) {
                this.totalRejectedTasks = totalRejectedTasks;
                return this;
            }
            
            public ThreadPoolMetricsBuilder uptimeMinutes(long uptimeMinutes) {
                this.uptimeMinutes = uptimeMinutes;
                return this;
            }
            
            public ThreadPoolMetrics build() {
                return new ThreadPoolMetrics(corePoolSize, maximumPoolSize, currentPoolSize,
                    activeThreads, queueSize, queueCapacity, completedTaskCount,
                    totalTaskCount, totalTasksExecuted, totalRejectedTasks, uptimeMinutes);
            }
        }
    }
}
