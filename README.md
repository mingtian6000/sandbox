# Sandbox for Java Microservices

This repository serves as a sandbox environment for developing and experimenting with Java microservices using Spring Boot, PostgreSQL, and Docker.

## Project Structure

```
sandbox/
├── services/                   # Microservices source code
│   ├── pom.xml                # Parent Maven POM
│   ├── user-service/          # User authentication service
│   │   ├── src/main/java/com/example/userservice/
│   │   ├── src/main/resources/
│   │   ├── pom.xml
│   │   └── Dockerfile
│   └── verification-service/  # Verification/OTP service
│       ├── src/main/java/com/example/verificationservice/
│       ├── src/main/resources/
│       ├── pom.xml
│       └── Dockerfile
├── infrastructure/            # Docker Compose and database configs
│   ├── docker-compose.yml
│   └── .env.example
├── scripts/                   # Utility scripts
│   ├── start-services.sh
│   └── stop-services.sh
├── docs/                     # Documentation
└── .github/workflows/        # CI/CD pipelines
```

## Technology Stack

- **Java 21** (Eclipse Temurin)
- **Spring Boot 3.2.0** with Spring Security, Spring Data JPA
- **PostgreSQL 16** (separate databases per service)
- **Docker & Docker Compose** for containerization
- **JWT** for authentication
- **Maven** as build tool

## Services

### 1. User Service (Port: 8081)
- User registration and authentication
- JWT-based secure login
- User profile management
- **Endpoints:**
  - `POST /api/users/auth/register` - Register new user
  - `POST /api/users/auth/login` - Login and get JWT token
  - `GET /api/users/{id}` - Get user by ID (authenticated)
  - `GET /api/users/username/{username}` - Get user by username

### 2. Verification Service (Port: 8082)
- OTP generation and verification
- Email notification (configurable SMTP)
- Support for multiple verification types (registration, password reset, login)
- **Endpoints:**
  - `POST /api/verification/generate` - Generate and send OTP
  - `POST /api/verification/verify` - Verify OTP code

## Prerequisites

- Docker and Docker Compose installed
- Java 21 (optional, for local development)
- Maven (optional, for local development)

## Quick Start

### Option 1: Using Docker Compose (Recommended)

1. **Clone the repository:**
   ```bash
   git clone https://github.com/mingtian6000/sandbox.git
   cd sandbox
   ```

2. **Start all services:**
   ```bash
   ./scripts/start-services.sh
   ```

   This will:
   - Build Docker images for both services
   - Start PostgreSQL databases
   - Start the microservices
   - Configure network between services

3. **Verify services are running:**
   ```bash
   curl http://localhost:8081/api/users/health
   curl http://localhost:8082/api/verification/health
   ```

4. **Stop services:**
   ```bash
   ./scripts/stop-services.sh
   ```

### Option 2: Local Development

1. **Set up databases:**
   ```bash
   docker run --name user-db -e POSTGRES_DB=userdb -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16-alpine
   docker run --name verification-db -e POSTGRES_DB=verificationdb -e POSTGRES_PASSWORD=postgres -p 5433:5432 -d postgres:16-alpine
   ```

2. **Build and run services:**
   ```bash
   cd services
   ./mvnw spring-boot:run -pl user-service
   # In another terminal
   ./mvnw spring-boot:run -pl verification-service
   ```

## API Usage Examples

### Register a New User
```bash
curl -X POST http://localhost:8081/api/users/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Login
```bash
curl -X POST http://localhost:8081/api/users/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "password123"
  }'
```

### Generate Verification Code
```bash
curl -X POST http://localhost:8082/api/verification/generate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "verificationType": "REGISTRATION"
  }'
```

### Verify Code
```bash
curl -X POST "http://localhost:8082/api/verification/verify?email=john@example.com&code=123456&verificationType=REGISTRATION"
```

## Configuration

### Environment Variables
Copy `.env.example` to `.env` in the infrastructure directory and update values:
```bash
cp infrastructure/.env.example infrastructure/.env
```

Key variables:
- `DB_USERNAME`, `DB_PASSWORD`: PostgreSQL credentials
- `JWT_SECRET`: Secret key for JWT tokens (change in production!)
- `SMTP_USERNAME`, `SMTP_PASSWORD`: Email credentials for verification service

### Database Migrations
Flyway is configured for database migrations. Migration scripts should be placed in:
- `user-service/src/main/resources/db/migration/`
- `verification-service/src/main/resources/db/migration/`

## Development

### Adding New Services
1. Create a new module in `services/` directory
2. Add module to parent `pom.xml`
3. Create `Dockerfile` for the service
4. Add service to `docker-compose.yml`
5. Update CI/CD workflow if needed

### Testing
```bash
cd services
./mvnw test
```

### Building Docker Images
```bash
cd services
docker build -f user-service/Dockerfile -t user-service:latest .
```

## CI/CD
GitHub Actions workflow (`/.github/workflows/ci.yml`) runs on every push:
- Java 17 setup
- Maven build
- Unit tests

## Contributing
This is a personal sandbox repository. Feel free to create branches and experiment!

## License
MIT