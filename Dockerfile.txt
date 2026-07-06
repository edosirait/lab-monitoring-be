# Stage 1: Compile dan Build proyek dengan Maven menggunakan JDK 17
FROM maven:3.8.8-openjdk-17 AS build
WORKDIR /app

# Salin file konfigurasi pom.xml dan seluruh source code
COPY pom.xml .
COPY src ./src

# Jalankan command Maven untuk membungkus proyek menjadi .jar (lewati unit test agar cepat)
RUN mvn clean package -DskipTests

# Stage 2: Jalankan file .jar yang sudah jadi menggunakan JRE yang lebih ringan
FROM openjdk:17-jdk-slim
WORKDIR /app

# Salin file .jar hasil dari Stage 1 ke dalam container jalannya aplikasi
COPY --from=build /app/target/*.jar app.jar

# Buka port 8080 agar bisa diakses oleh Render
EXPOSE 8080

# Jalankan aplikasi Spring Boot Anda
ENTRYPOINT ["java", "-jar", "app.jar"]
