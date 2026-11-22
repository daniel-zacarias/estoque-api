# syntax=docker/dockerfile:1
# Imagens fixadas por digest (tag@sha256): o build é reproduzível e uma tag republicada
# não troca a base sem revisão. O Dependabot abre PR quando sai digest novo.

# ---- build ----
FROM maven:3.9-eclipse-temurin-26@sha256:b2c1ad85954592f9928e84327c65201f308ad9b5d8ed7d5b823717c97bf23fbb AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/app.jar extract --layers --launcher --destination extracted

# ---- runtime ----
FROM eclipse-temurin:25-jre@sha256:945247f9d092703e0c6be92243ef3c51640229e23ab34d353c8dcdc73e6cd4e6
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
# Camadas da menos para a mais volátil: rebuilds só trocam a última
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./
USER app
EXPOSE 8080
# A imagem JRE não tem curl/wget: usa o /dev/tcp do bash (mesmo teste do compose.yaml)
HEALTHCHECK --interval=15s --start-period=60s --retries=5 \
  CMD bash -c "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health HTTP/1.0\r\n\r\n' >&3 && grep -q UP <&3"
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
