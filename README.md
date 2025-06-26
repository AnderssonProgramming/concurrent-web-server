# 🚀 Concurrent Web Server

A high-performance, thread-safe HTTP server built with Java and Spring Boot, demonstrating advanced concurrent programming concepts and clean code practices.

## 📋 Overview

This project implements a fully concurrent web server that handles multiple client requests simultaneously using thread pools and modern Java concurrency features. The server provides comprehensive session management, real-time metrics monitoring, and robust error handling.

## ✨ Key Features

### 🧵 Concurrency & Performance
- **Thread Pool Management**: Configurable thread pools with core/max sizes and queue capacity
- **Connection Handling**: Asynchronous client connection processing
- **Load Balancing**: Intelligent request distribution across worker threads
- **Performance Monitoring**: Real-time thread pool metrics and statistics
- **Graceful Shutdown**: Proper resource cleanup and thread termination

### 🎯 Session Management
- **Thread-Safe Sessions**: Concurrent session storage and retrieval
- **Automatic Cleanup**: Scheduled cleanup of expired sessions
- **User Simulation**: Multi-user environment simulation with unique sessions
- **Session Persistence**: Cookie-based session tracking

### 📊 Monitoring & Metrics
- **Real-Time Dashboard**: Beautiful web-based metrics interface
- **Performance Analytics**: Response times, throughput, and error rates
- **Resource Monitoring**: Memory usage, CPU utilization, and thread activity
- **JSON API**: Programmatic access to server metrics

### 🌐 HTTP Features
- **Multiple Endpoints**: Various handlers for different use cases
- **Cookie Support**: Full cookie management and session tracking
- **Header Processing**: Complete HTTP header parsing and manipulation
- **Error Handling**: Comprehensive error responses and logging

## 🏗️ Architecture

### Core Components

```
src/main/java/edu/eci/arsw/concurrent_ws/
├── server/
│   ├── ConcurrentWebServer.java     # Main server with thread pool
│   └── ClientHandler.java           # Individual client request handler
├── handler/
│   └── impl/
│       ├── HelloHandler.java        # Basic greeting endpoint
│       ├── TimeHandler.java         # Current time endpoint
│       ├── HeadersHandler.java      # HTTP headers display
│       ├── CookiesHandler.java      # Cookie management
│       ├── LoadTestHandler.java     # Performance testing endpoint
│       ├── MetricsHandler.java      # Server metrics dashboard
│       └── MultiUserSimulatorHandler.java # Session simulation
├── session/
│   └── SessionManager.java          # Thread-safe session management
├── monitoring/
│   └── ThreadPoolMonitor.java       # Performance monitoring
├── model/
│   ├── HttpRequest.java             # HTTP request representation
│   └── HttpResponse.java            # HTTP response representation
└── parser/
    └── HttpParser.java               # HTTP protocol parser
```

### Thread Pool Configuration

```properties
# Application Properties
server.port=8080
server.thread-pool.core-size=10      # Core threads always alive
server.thread-pool.max-size=50       # Maximum threads under load
server.thread-pool.queue-capacity=100 # Request queue size
server.connection.timeout=30000      # Connection timeout (ms)
```

## 🚦 Getting Started

### Prerequisites
- **Java 17+**: Required for modern language features
- **Maven 3.6+**: For dependency management and building
- **Git**: For version control

### Installation & Setup

1. **Clone the repository**
```bash
git clone https://github.com/AnderssonProgramming/concurrent-web-server.git
cd concurrent-web-server
```

2. **Build the project**
```bash
mvn clean compile
```

3. **Run tests**
```bash
mvn test
```

4. **Start the server**
```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## 🌐 Available Endpoints

| Endpoint | Method | Description | Features |
|----------|--------|-------------|----------|
| `/` | GET | Home page with navigation | Static content |
| `/hello` | GET | Simple greeting message | Basic response |
| `/time` | GET | Current server time | Dynamic content |
| `/headers` | GET | Display HTTP headers | Request inspection |
| `/cookies` | GET | Cookie management demo | Session handling |
| `/users` | GET | Multi-user simulator | Session tracking |
| `/load-test` | GET | Performance testing | CPU-intensive task |
| `/metrics` | GET | Server metrics dashboard | Real-time monitoring |
| `/api/metrics` | GET | JSON metrics API | Programmatic access |

### Example Usage

```bash
# Basic request
curl http://localhost:8080/hello

# Get server metrics
curl http://localhost:8080/api/metrics

# Test with session
curl -c cookies.txt -b cookies.txt http://localhost:8080/users
```

## 🧪 Testing

### Test Suites

The project includes comprehensive test suites to validate concurrent behavior:

#### 1. **Basic Concurrent Tests** (`ConcurrentWebServerTest.java`)
- Multi-threaded request handling
- Response time analysis
- Error rate validation

#### 2. **Enhanced Concurrent Tests** (`EnhancedConcurrentWebServerTest.java`)
- High-volume request testing (50 threads × 10 requests)
- Session management validation
- Mixed endpoint load testing
- Performance metrics analysis

#### 3. **Load Test Performance** (`LoadTestPerformanceTest.java`)
- Load test endpoint validation
- Rapid request stress testing
- Thread pool saturation testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EnhancedConcurrentWebServerTest

# Run with detailed logging
mvn test -Dspring.profiles.active=test
```

### Performance Benchmarks

Expected performance characteristics:
- **Throughput**: 100+ requests/second
- **Response Time**: < 100ms for simple endpoints
- **Concurrent Users**: 50+ simultaneous connections
- **Success Rate**: > 95% under normal load

## 📊 Monitoring & Metrics

### Web Dashboard

Access the metrics dashboard at `http://localhost:8080/metrics` for:
- Real-time thread pool status
- Request processing statistics
- Memory and CPU utilization
- Active session count
- Performance trends

### JSON API

Programmatic access via `http://localhost:8080/api/metrics`:

```json
{
  "threadPool": {
    "corePoolSize": 10,
    "maximumPoolSize": 50,
    "currentPoolSize": 15,
    "activeThreads": 5,
    "queueSize": 2,
    "completedTaskCount": 1247
  },
  "server": {
    "port": 8080,
    "running": true,
    "uptimeMinutes": 45,
    "totalConnectionsHandled": 1250
  },
  "system": {
    "availableProcessors": 8,
    "totalMemoryMB": 512,
    "freeMemoryMB": 128,
    "maxMemoryMB": 1024
  }
}
```

## 👥 Session Management

### Features
- **Thread-Safe Storage**: Concurrent session access without conflicts
- **Automatic Expiration**: 30-minute session timeout with cleanup
- **User Tracking**: Unique session IDs and visit counting
- **Attributes**: Custom session data storage

### Session Lifecycle
1. **Creation**: New session on first request
2. **Tracking**: Cookie-based session identification
3. **Updates**: Visit count and last access time
4. **Cleanup**: Automatic removal of expired sessions

## ⚙️ Configuration

### Server Settings

```properties
# Server Configuration
server.port=8080
server.thread-pool.core-size=10
server.thread-pool.max-size=50
server.thread-pool.queue-capacity=100
server.connection.timeout=30000

# Logging Configuration
logging.level.edu.eci.arsw.concurrent_ws=INFO
logging.level.root=WARN
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# Management endpoints for monitoring
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

### Environment Variables

Override configuration with environment variables:
```bash
export SERVER_PORT=9090
export THREAD_POOL_CORE_SIZE=20
export THREAD_POOL_MAX_SIZE=100
```

## 🛠️ Development

### Code Quality Standards

- **Clean Code**: Well-documented, readable, and maintainable
- **SOLID Principles**: Single responsibility, dependency injection
- **Thread Safety**: Proper synchronization and concurrent collections
- **Error Handling**: Comprehensive exception management
- **Logging**: Structured logging with appropriate levels

### Adding New Endpoints

1. **Create Handler**
```java
@Component
public class CustomHandler implements RequestHandler {
    @Override
    public boolean canHandle(HttpRequest request) {
        return "GET".equals(request.getMethod()) && "/custom".equals(request.getPath());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        response.setBody("Custom content");
        return response;
    }
}
```

2. **Register Automatically**: Spring Boot auto-detects `@Component` handlers

### Best Practices

- Use dependency injection for loose coupling
- Implement proper error boundaries
- Add comprehensive logging
- Write unit and integration tests
- Follow thread-safe programming patterns

## 📈 Performance Tuning

### Thread Pool Optimization

```properties
# For CPU-intensive workloads
server.thread-pool.core-size=8    # Number of CPU cores
server.thread-pool.max-size=16    # 2x CPU cores

# For I/O-intensive workloads
server.thread-pool.core-size=20   # Higher than CPU cores
server.thread-pool.max-size=100   # Much higher for blocking operations
```

### JVM Tuning

```bash
# Example JVM flags for production
java -Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
     -jar concurrent-web-server.jar
```

## 🔧 Troubleshooting

### Common Issues

1. **Port Already in Use**
   ```bash
   # Check what's using the port
   netstat -tulpn | grep :8080
   # Change port in application.properties
   ```

2. **Thread Pool Saturation**
   - Monitor `/metrics` for queue sizes
   - Increase max pool size or queue capacity
   - Check for blocking operations in handlers

3. **Memory Issues**
   - Monitor session count for leaks
   - Adjust JVM heap settings
   - Check for unclosed resources

### Debug Logging

Enable detailed logging:
```properties
logging.level.edu.eci.arsw.concurrent_ws=DEBUG
logging.level.org.springframework=DEBUG
```

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Spring Boot** for the excellent framework
- **Maven** for dependency management
- **JUnit 5** for testing framework
- **SLF4J** for logging abstraction

## 📞 Support

- **Documentation**: This README and code comments
- **Examples**: See test files for usage examples

---

⭐ **Star this repository if you found it helpful!**

*Built with ❤️ and lots of ☕ by Andersson Sanchez*