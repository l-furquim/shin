FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY commons/pom.xml commons/pom.xml
COPY commons/src commons/src

RUN mvn -f commons/pom.xml clean install -DskipTests

COPY subscription/pom.xml subscription/pom.xml

WORKDIR /app/subscription

RUN mvn dependency:go-offline -B

COPY subscription/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S subscription -u 1001

WORKDIR /app

COPY --from=build --chown=subscription:subscription /app/user/target/*.jar subscription-service.jar

RUN chmod 444 subscription-service.jar

EXPOSE 8080
USER subscription

ENTRYPOINT ["java", "-jar", "subscription-service.jar"]