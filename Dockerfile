FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Instala Maven en la imagen de build (algunas combinaciones maven+jdk no existen públicamente)
RUN apt-get update && apt-get install -y --no-install-recommends maven ca-certificates && rm -rf /var/lib/apt/lists/*

COPY pom.xml ./
COPY src ./src

# Build the fat-jar and copy it to a stable path in the same layer to avoid cache issues
RUN mvn -B -DskipTests package && \
	mkdir -p /workspace/out && \
	sh -c 'for f in /workspace/target/*.jar; do if [ -f "$f" ]; then cp "$f" /workspace/out/app.jar && break; fi; done'

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /workspace/out/app.jar app.jar

# Instala netcat (openbsd) para poder esperar hasta que la DB acepte conexiones
RUN apt-get update && apt-get install -y --no-install-recommends netcat-openbsd && rm -rf /var/lib/apt/lists/*

# Variables por defecto para el host/puerto de la BD
ENV PORT=8080 DB_HOST=db DB_PORT=3306
EXPOSE 8080

# Espera hasta que la base de datos esté aceptando conexiones antes de ejecutar la app
CMD ["sh","-c","until nc -z ${DB_HOST} ${DB_PORT}; do echo \"waiting for db at ${DB_HOST}:${DB_PORT}\"; sleep 1; done; exec java -jar /app/app.jar"]
