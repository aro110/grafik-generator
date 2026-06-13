FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml ./
COPY backend ./backend

RUN mvn -B package

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/grafik-generator-0.0.1-SNAPSHOT.jar app.jar

ENV SERVER_PORT=8081

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
