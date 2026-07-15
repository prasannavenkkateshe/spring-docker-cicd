# spring-docker-cicd

A production-grade Spring Boot REST API with a fully automated CI/CD pipeline built using GitHub Actions — covering automated testing, Docker image builds, and Docker Hub publishing on every code push.

> Built to demonstrate end-to-end DevOps practices for a Java backend application.

---

## What this project does

Every `git push` to `main` automatically:

1. Spins up a fresh Linux environment on GitHub's servers
2. Runs all tests against a real MySQL 8 database (service container)
3. On test pass → builds a Docker image and pushes to Docker Hub with two tags:
   - `:latest` — always the newest image
   - `:commit-sha` — frozen to that exact commit, enabling rollback

No manual steps. No local Docker builds. Fully automated.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Build Tool | Maven |
| Database | MySQL 8 |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Image Registry | Docker Hub |

---

## CI/CD Pipeline Architecture

```
git push → main
        │
        ▼
┌─────────────────────────────────────────┐
│  JOB 1 — test (ubuntu-latest)          │
│                                         │
│  [MySQL 8 service container]            │
│                                         │
│  checkout → setup-java → cache ~/.m2   │
│  → wait for MySQL → mvn clean test      │
│                                         │
│  ✅ pass → Job 2    ❌ fail → stops     │
└─────────────────────────────────────────┘
        │
        │  needs: test
        ▼
┌─────────────────────────────────────────┐
│  JOB 2 — docker (ubuntu-latest)        │
│                                         │
│  checkout → docker login                │
│  → build image → push to Docker Hub    │
│                                         │
│  Tags:                                  │
│  :latest                                │
│  :${{ github.sha }}                     │
└─────────────────────────────────────────┘
```

---

## Pipeline Features

**Multi-job with dependency**
The docker job only runs if the test job passes. Broken code never gets packaged into an image.

**Real database in CI**
Tests run against an actual MySQL 8 instance spun up as a service container — not an in-memory mock. Spring Boot datasource URL, username, and password are injected as environment variables at runtime.

**Maven dependency caching**
The `~/.m2` directory is cached using `actions/cache` keyed on `pom.xml` hash. Dependencies are only re-downloaded when `pom.xml` changes.

**Secrets management**
All credentials (DB password, Docker Hub token) are stored in GitHub Secrets — never hardcoded in YAML.

**Commit SHA image tagging**
Every push produces a uniquely tagged image. Any version can be pulled and run independently, enabling instant rollback:
```bash
docker pull prasannavenkkateshe/spring-docker-cicd:<commit-sha>
```

---

## Running Locally

**Prerequisites:** Java 17, Maven, Docker Desktop

```bash
# Clone the repo
git clone https://github.com/prasannavenkkateshe/spring-docker-cicd.git
cd spring-docker-cicd

# Start app + MySQL with Docker Compose
docker-compose up --build
```

App runs at `http://localhost:8080`

---

## Project Structure

```
spring-docker-cicd/
├── .github/
│   └── workflows/
│       └── ci.yml          # Full CI/CD pipeline definition
├── src/
│   ├── main/java/          # Spring Boot application code
│   └── test/java/          # Test classes
├── Dockerfile              # Multi-stage build (JDK build → JRE runtime)
├── docker-compose.yml      # Local dev setup with MySQL
└── pom.xml
```

---

## Dockerfile Highlights

Uses a **multi-stage build** to keep the final image small:
- **Stage 1** — builds the JAR using `eclipse-temurin:17-jdk-alpine`
- **Stage 2** — runs the JAR using `eclipse-temurin:17-jre-alpine` (no JDK in prod)
- Runs as a **non-root user** for security

---

## GitHub Actions Workflow — ci.yml

```yaml
name: CI Pipeline

on:
  push:
    branches: [ "main" ]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_DATABASE: ${{ secrets.MYSQL_DB }}
          MYSQL_ROOT_PASSWORD: ${{ secrets.MYSQL_ROOT_PASSWORD }}
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -h localhost -u root -proot"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-maven-
      - name: Wait for MySQL
        run: |
          sudo apt-get install -y mysql-client
          until mysqladmin ping -h 127.0.0.1 -u root -proot --silent; do
            sleep 5
          done
      - name: Build and Test
        run: mvn clean test
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/${{ secrets.MYSQL_DB }}
          SPRING_DATASOURCE_USERNAME: root
          SPRING_DATASOURCE_PASSWORD: ${{ secrets.MYSQL_ROOT_PASSWORD }}

  docker:
    runs-on: ubuntu-latest
    needs: test
    steps:
      - uses: actions/checkout@v4
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      - name: Build and Push Docker Image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKERHUB_USERNAME }}/spring-docker-cicd:latest
            ${{ secrets.DOCKERHUB_USERNAME }}/spring-docker-cicd:${{ github.sha }}
```

---

## Docker Hub

Image available at:
```
docker pull prasannavenkkateshe/spring-docker-cicd:latest
```

---

## Author

**Prasanna** — Java / Spring Boot Developer · 2.5 YOE  
[GitHub](https://github.com/prasannavenkkateshe)
