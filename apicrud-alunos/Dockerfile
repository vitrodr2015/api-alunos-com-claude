# Fase 1: compila a aplicação com Maven
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copia só o necessário para baixar as dependências (aproveita o cache do Docker)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q clean package -DskipTests

# Fase 2: roda só com o Java, sem Maven nem código-fonte
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
