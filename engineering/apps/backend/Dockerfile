# ================================================================
# Anadolu Atlası Backend - Multi-stage Docker Build
# ================================================================
# Java 21 + Spring Boot 3.5 + Maven Wrapper
# ================================================================

# ── Stage 1: Build ──────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Maven wrapper ve pom.xml — bağımlılık cache için önce kopyala
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Bağımlılıkları indir (layer cache)
RUN ./mvnw dependency:resolve -B

# Kaynak kodu kopyala ve paketle
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Güvenlik: root olmayan kullanıcı
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

# JVM tuning
ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=local

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
