FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app

COPY backend/pom.xml pom.xml
COPY backend/src src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends bash postgresql-client python3 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/bookshelves-backend-0.0.1-SNAPSHOT.jar app.jar
COPY scripts scripts
RUN chmod +x /app/scripts/export_bookshelves.sh

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
