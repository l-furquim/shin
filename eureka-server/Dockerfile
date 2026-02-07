FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY eureka-server/pom.xml eureka-server/pom.xml

WORKDIR /app/eureka-server

RUN mvn dependency:go-offline -B

COPY eureka-server/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S eureka -u 1001

WORKDIR /app

COPY --from=build --chown=eureka:eureka /app/eureka-server/target/*.jar eureka-service.jar

RUN chmod 444 eureka-service.jar

EXPOSE 8080
USER eureka

ENTRYPOINT ["java", "-jar", "eureka-service.jar"]