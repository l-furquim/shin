FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY config-server/pom.xml config-server/pom.xml

WORKDIR /app/config-server

RUN mvn dependency:go-offline -B

COPY config-server/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S config -u 1001

WORKDIR /app

COPY --from=build --chown=config:config /app/config-server/target/*.jar config-service.jar

RUN chmod 444 config-service.jar

EXPOSE 8080
USER config

ENTRYPOINT ["java", "-jar", "config-service.jar"]