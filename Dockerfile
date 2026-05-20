# ==========================================
# STAGE 1: Build the Jar file using Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the dependency management file and source code
COPY pom.xml .
COPY src ./src

# Compile and package the application, skipping unit tests for faster cloud builds
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Run the application using JRE
# ==========================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the compiled jar file from Stage 1
# The wildcard matching ensures it catches your jar regardless of the artifactId name
COPY --from=build /app/target/expense-tracker-*.jar app.jar

# Expose the standard Spring Boot embedded Tomcat web server port
EXPOSE 8080

# The execution command to launch your Spring Boot environment
ENTRYPOINT ["java", "-jar", "app.jar"]