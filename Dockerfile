FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app \
    && mkdir -p /app/uploaded-images && chown app:app /app/uploaded-images
USER app

COPY --from=build /workspace/target/my-market-app.jar app.jar

ENV MARKET_IMAGES_DIR=/app/uploaded-images
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
