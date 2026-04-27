FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY commons/pom.xml commons/pom.xml
COPY commons/src commons/src

RUN mvn -f commons/pom.xml clean install -DskipTests

COPY streaming/pom.xml streaming/pom.xml

WORKDIR /app/streaming

RUN mvn dependency:go-offline -B

COPY streaming/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S streaming -u 1001

WORKDIR /app

COPY --from=build --chown=streaming:streaming /app/streaming/target/*.jar streaming-service.jar

RUN chmod 444 streaming-service.jar

EXPOSE 8080
USER streaming

ENTRYPOINT ["java", "-jar", "streaming-service.jar"]
