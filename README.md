# Travel Mind

Travel Mind is an AI travel planning backend built with Java 21, Spring Boot 4, Maven multi-modules, and Spring AI Alibaba.

## Overview

The service helps generate travel plans from a destination, trip length, user preferences, and notes. It includes modules for AI planning, map context, content extraction, and trip task orchestration.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring AI Alibaba / Spring AI
- Maven multi-module project
- WebSocket progress updates
- MyBatis Plus
- Sa-Token

## Modules

- `app`: application entry point and HTTP/WebSocket APIs
- `common`: shared configuration, models, and utilities
- `modules/ai`: AI prompt, agent, and structured output services
- `modules/content`: content extraction services
- `modules/map`: map and POI context services
- `modules/trip`: trip planning workflow

## Quick Start

1. Copy `.env.example` and configure required keys.
2. Build the project:

```bash
mvn clean package
```

3. Run the application module:

```bash
mvn -pl app spring-boot:run
```

## License

This project keeps the original GPL-2.0 license file.

## Author

Wang-ke-li
