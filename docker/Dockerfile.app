# App image: Maven build + runtime base (Temurin 17 + Node 20 + OpenResty).
#
# Manual build example:
#   docker build -f docker/Dockerfile.app \
#     --build-arg MAVEN_BUILD_IMAGE=maven:3.9-eclipse-temurin-17 \
#     --build-arg RUNTIME_BASE_IMAGE=ecshopx-java:17-node20-openresty \
#     -t ecshopx-app:latest .

ARG MAVEN_BUILD_IMAGE=maven:3.9-eclipse-temurin-17
ARG RUNTIME_BASE_IMAGE=ecshopx-java:17-node20-openresty

FROM ${MAVEN_BUILD_IMAGE} AS builder
WORKDIR /build
COPY docker/maven-settings.xml /tmp/maven-settings.xml
COPY . .
RUN mvn -B -s /tmp/maven-settings.xml -pl ecshopx-bootstrap -am package -DskipTests

FROM ${RUNTIME_BASE_IMAGE} AS runtime
WORKDIR /app

COPY docker/app-nginx.conf /app/nginx.conf.template
COPY docker/app-entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && mkdir -p /app/logs

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom" \
    APP_ARGS="" \
    APP_JAR=/app/app.jar

EXPOSE 80 18080 19999
ENTRYPOINT ["/app/entrypoint.sh"]

# Full image: bake bootstrap jar (docker-compose.dev.yml default)
FROM runtime AS app
COPY --from=builder /build/ecshopx-bootstrap/target/ecshopx-bootstrap-*.jar /app/app.jar

FROM app
