# 🏦 Fraud Detection System

Real-time fraud scoring pipeline built with **Quarkus + GraalVM**, **ONNX Runtime**, **Janino Rules Engine**, **Redis**, and **Cassandra**.

## Architecture

```
Transaction → DataLoader → [Scanner | Rules | Model] (parallel)
  → FeatureMerger (Janino) → ScoreAggregator → Decision → Result
```

| Layer | Technology | Purpose |
|:---|:---|:---|
| **Framework** | Quarkus + GraalVM | Native-compiled Java microservice |
| **Model** | ONNX Runtime | LightGBM-style model (converted to ONNX) |
| **Rules** | Janino | Dynamic rule expressions, compiled at runtime |
| **L1 Cache** | Redis | Raw transaction data (24h TTL) |
| **L2 Cache** | Cassandra | Pre-computed aggregated metrics (30-90d) |
| **Stream** | Kafka | Transaction event ingestion |
| **Config** | YAML | Rules, pipeline DAG, thresholds |

## Quick Start

### Prerequisites
- JDK 21+
- Apache Maven 3.9+
- GraalVM (for native build)
- Docker (for Redis + Cassandra)

### Build & Run

```bash
# Development mode (JVM)
./mvnw quarkus:dev

# Native build (GraalVM)
./mvnw package -Pnative

# Run native binary
./target/fraud-detection-1.0.0-SNAPSHOT-runner
```

### Docker Compose

```bash
docker-compose up -d redis cassandra
```

### API

```bash
# Score a transaction
curl -X POST http://localhost:8080/api/v1/fraud/score \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "txn-001",
    "accountId": "acc_normal_001",
    "amount": 150.00,
    "currency": "USD",
    "channel": "ONLINE",
    "deviceId": "device-mbp-001",
    "ipAddress": "8.8.8.8",
    "geoLocation": "37.7749,-122.4194",
    "timestamp": 1700000000000
  }'

# Batch scoring
curl -X POST http://localhost:8080/api/v1/fraud/score/batch \
  -H "Content-Type: application/json" \
  -d '[{...}, {...}]'

# Health check
curl http://localhost:8080/api/v1/fraud/health
```

### Response

```json
{
  "transactionId": "txn-001",
  "fraudScore": 12.5,
  "decision": "APPROVE",
  "decisionReason": "Transaction approved — low risk",
  "modelScore": 10.2,
  "rulesScore": 5.0,
  "scannersScore": 3.5,
  "scannerDetails": [...],
  "ruleDetails": [...],
  "processingTimeMs": 45,
  "pipelineVersion": "1.0.0"
}
```

## Project Structure

```
src/main/java/com/fraud/detection/
├── FraudDetectionApplication.java   # Quarkus entry point
├── model/                           # Domain models
│   ├── Transaction.java
│   ├── FeatureVector.java
│   ├── ScoringResult.java
│   ├── ScannerResult.java
│   ├── RuleResult.java
│   └── OnnxModelService.java        # ONNX Runtime inference
├── pipeline/
│   ├── PipelineOrchestrator.java    # DAG orchestrator
│   ├── PipelineContext.java         # Shared context
├── scanner/
│   ├── Scanner.java                 # Interface
│   ├── DeviceFingerprintScanner.java
│   ├── GeoLocationScanner.java
│   └── BehaviorPatternScanner.java
├── rule/
│   ├── JaninoRuleEngine.java        # Dynamic rules engine
│   ├── FraudRule.java
│   └── RuleConfigLoader.java
├── feature/
│   ├── FeatureExtractor.java        # Redis + Cassandra → features
│   └── FeatureMerger.java           # Janino score fusion
├── decision/
│   └── DecisionMaker.java           # Threshold-based decision
├── cache/
│   ├── RedisService.java            # L1: raw transactions (24h)
│   └── CassandraService.java        # L2: aggregated metrics (30-90d)
└── resource/
    └── FraudDetectionResource.java  # REST API
```

## Performance

| Metric | Target |
|:---|:---|
| P99 Latency | < 200ms per transaction |
| Throughput | 1000+ TPS (with GPU ONNX) |
| Model Inference | < 1ms (GPU) / < 5ms (CPU) |
| Pipeline Parallelism | 3 tracks concurrent |
