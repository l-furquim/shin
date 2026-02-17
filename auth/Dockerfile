FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY auth/pom.xml auth/pom.xml

WORKDIR /app/auth

RUN mvn dependency:go-offline -B

COPY auth/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S auth -u 1001

WORKDIR /app

COPY --from=build --chown=auth:auth /app/auth/target/*.jar auth-service.jar

RUN chmod 444 auth-service.jar

EXPOSE 8080
USER auth

ENTRYPOINT ["java", "-jar", "auth-service.jar"]
