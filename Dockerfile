# --- Etapa 1: compilación ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Etapa 2: imagen final, ligera ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# El servidor embebido escucha en SERVER_PORT (por defecto 8080). No hay ningun
# sitio que lo cambie, asi que el puerto real es 8080: es el que hay que poner
# en "Ports Exposes" de Coolify.
EXPOSE 8080

# IMPORTANTE en produccion: hay que pasar  SPRING_PROFILES_ACTIVE=prod  como
# variable de entorno. Sin ella la app arranca en perfil "dev" con base de
# datos H2 dentro del contenedor y pierde TODOS los datos en cada redeploy.
# La lista completa de variables esta en DEPLOY.md.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
