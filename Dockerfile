# Stage 1: Compile dan Build proyek dengan Maven menggunakan JDK 17 yang valid
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Salin file konfigurasi pom.xml dan seluruh source code
COPY pom.xml .
COPY src ./src

# Jalankan command Maven untuk membungkus proyek menjadi .jar (lewati unit test agar cepat)
RUN mvn clean package -DskipTests

# Stage 2: Jalankan file .jar menggunakan JRE 17 yang jauh lebih ringan dan hemat RAM
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Salin file .jar hasil dari Stage 1 ke dalam container jalannya aplikasi
COPY --from=build /app/target/*.jar app.jar

# PENTING: Batasi penggunaan RAM JVM agar tidak terkena Out of Memory (OOM) di Free Tier Render (RAM 512MB)
ENV JAVA_OPTS="-Xmx256m -Xms256m"

# Buka port 8080 agar bisa diakses oleh Render
EXPOSE 8080

# Jalankan aplikasi Spring Boot Anda dengan membaca konfigurasi RAM di atas
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
