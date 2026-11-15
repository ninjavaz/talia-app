# Backend image: Spring Boot jar on a JRE, running as a non-root user. Build context: backend/.
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/talia-*.jar /app.jar

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S talia && adduser -S -G talia talia
WORKDIR /app
COPY --from=build /app.jar app.jar
USER talia
EXPOSE 8080
# Small heap for a 1 GB server (overridable with JAVA_TOOL_OPTIONS in compose)
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -Xss512k -Xmx220m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m"
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
