FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY openapi/ openapi/
COPY market-app/pom.xml market-app/
COPY payment-service/pom.xml payment-service/
COPY auth-server/pom.xml auth-server/
RUN chmod +x mvnw && ./mvnw -B -q -pl auth-server -am dependency:go-offline

COPY auth-server/src/ auth-server/src/
RUN ./mvnw -B -q -pl auth-server -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /workspace/auth-server/target/auth-server.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
