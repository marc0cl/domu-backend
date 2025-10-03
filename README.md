# Domu Backend

Backend API for the Domu project - Proyecto de Título

## Technology Stack

- **Java 17** - Modern Java LTS version
- **Gradle 9.1.0** - Build automation tool
- **Javalin 6.7.0** - Lightweight web framework
- **Jackson** - JSON serialization/deserialization
- **SLF4J** - Logging framework
- **JUnit 5** - Testing framework

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/domu/
│   │   │   ├── config/       # Application configuration
│   │   │   ├── controller/   # REST API controllers
│   │   │   ├── service/      # Business logic services
│   │   │   ├── model/        # Domain models
│   │   │   ├── util/         # Utility classes
│   │   │   └── App.java      # Main application class
│   │   └── resources/
│   │       ├── application.properties
│   │       └── simplelogger.properties
│   └── test/
│       └── java/com/domu/    # Unit tests
└── build.gradle.kts          # Gradle build configuration
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle 9.1.0 or higher (or use the included Gradle wrapper)

### Building the Project

```bash
# Using Gradle wrapper (recommended)
./gradlew build

# Or using system Gradle
gradle build
```

### Running the Application

```bash
# Using Gradle wrapper
./gradlew run

# Or using system Gradle
gradle run
```

The server will start on port **7000** by default.

### Running Tests

```bash
./gradlew test
```

## API Endpoints

### Health Check

- **GET** `/health` - Health check endpoint
- **GET** `/` - Welcome endpoint with API information

### User Management (Example CRUD API)

- **GET** `/api/v1/users` - Get all users
- **GET** `/api/v1/users/{id}` - Get user by ID
- **POST** `/api/v1/users` - Create a new user
- **PUT** `/api/v1/users/{id}` - Update user by ID
- **DELETE** `/api/v1/users/{id}` - Delete user by ID

### Example Requests

#### Get all users
```bash
curl http://localhost:7000/api/v1/users
```

#### Create a user
```bash
curl -X POST http://localhost:7000/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```

#### Get user by ID
```bash
curl http://localhost:7000/api/v1/users/1
```

#### Update user
```bash
curl -X PUT http://localhost:7000/api/v1/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Updated","email":"john.updated@example.com"}'
```

#### Delete user
```bash
curl -X DELETE http://localhost:7000/api/v1/users/1
```

## Development

### Project Features

- ✅ RESTful API design
- ✅ JSON request/response handling
- ✅ CORS enabled for development
- ✅ Structured logging with SLF4J
- ✅ Exception handling
- ✅ Request validation
- ✅ In-memory data storage (for demonstration)
- ✅ Unit tests with JUnit 5

### Best Practices Implemented

1. **Layered Architecture**: Separation of concerns with controllers, services, and models
2. **Dependency Management**: Using Gradle version catalog (libs.versions.toml)
3. **Modern Java Features**: Java 17 with modern syntax and patterns
4. **JSON Serialization**: Proper date/time handling with Jackson
5. **Logging**: Structured logging with configurable levels
6. **Testing**: Comprehensive unit tests using Javalin's test utilities
7. **Error Handling**: Consistent error responses and validation

## Configuration

Edit `app/src/main/resources/application.properties` to configure:

- Server port
- Application metadata

Edit `app/src/main/resources/simplelogger.properties` to configure:

- Log levels
- Log format
- Package-specific logging

## License

This project is part of a university thesis project (Proyecto de Título).
