FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY openapi/ openapi/
COPY payment-service/pom.xml payment-service/
RUN chmod +x mvnw && ./mvnw -B -q -pl payment-service -am dependency:go-offline

COPY payment-service/src/ payment-service/src/
RUN ./mvnw -B -q -pl payment-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /workspace/payment-service/target/payment-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
