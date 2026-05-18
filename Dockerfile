# Etapa 1: Construcción (Build)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos el pom y el código fuente
COPY pom.xml .
COPY src ./src

# CAMBIO CLAVE: Usamos 'mvn' en lugar de './mvnw' 
# Esto evita el error 126 de permisos de ejecución
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Runtime)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copiamos el .jar generado (Asegúrate de que el nombre coincida con tu pom.xml)
COPY --from=build /app/target/playstop-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]