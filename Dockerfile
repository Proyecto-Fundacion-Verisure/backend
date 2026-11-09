# Imagen en dos etapas: se compila con el JDK completo y se ejecuta con el JRE,
# así la imagen final no lleva Maven ni el código fuente.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Primero solo el POM: si las dependencias no cambian, esta capa se reutiliza.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

# Usuario sin privilegios: el proceso no necesita ser root.
RUN useradd --system --create-home --uid 10001 spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
