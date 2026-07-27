FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl app -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system travelmind && useradd --system --gid travelmind --home-dir /app travelmind \
    && mkdir -p /app/uploads/private /app/uploads/public /app/logs \
    && chown -R travelmind:travelmind /app
COPY --from=build --chown=travelmind:travelmind /workspace/app/target/app-*.jar /app/app.jar
USER travelmind
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
