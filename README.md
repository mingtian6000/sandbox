# Sandbox for Java Microservices

This repository serves as a sandbox environment for developing and experimenting with Java microservices.

## Purpose

- Experiment with microservice architectures
- Test different Java frameworks (Spring Boot, Quarkus, Micronaut, etc.)
- Explore containerization, orchestration, and cloud deployment
- Practice CI/CD pipelines for Java microservices

## Getting Started

1. Clone this repository:
   ```bash
   git clone https://github.com/mingtian6000/sandbox.git
   ```

2. Each microservice should be organized in its own directory under `services/` or as separate modules.

## Structure

```
sandbox/
├── services/          # Individual microservices
│   ├── service-a/
│   ├── service-b/
│   └── ...
├── infrastructure/    # Docker, Kubernetes, Terraform configs
├── scripts/          # Helper scripts
└── docs/             # Documentation
```

## Technology Stack Considerations

- **Java 17+** (LTS versions)
- **Build Tools**: Maven, Gradle
- **Frameworks**: Spring Boot, Quarkus, Micronaut
- **Database**: PostgreSQL, MySQL, MongoDB
- **Message Queue**: RabbitMQ, Kafka
- **Container**: Docker
- **Orchestration**: Kubernetes, Docker Compose
- **Monitoring**: Prometheus, Grafana
- **Logging**: ELK stack, Loki

## Contributing

This is a personal sandbox repository. Feel free to create branches and experiment!