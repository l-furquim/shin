FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY gateway/pom.xml gateway/pom.xml

WORKDIR /app/gateway

RUN mvn dependency:go-offline -B

COPY gateway/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S gateway -u 1001

WORKDIR /app

COPY --from=build --chown=gateway:gateway /app/gateway/target/*.jar gateway-service.jar

RUN chmod 444 gateway-service.jar

EXPOSE 8080
USER gateway

ENTRYPOINT ["java", "-jar", "gateway-service.jar"]
