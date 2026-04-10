FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY commons/pom.xml commons/pom.xml
COPY commons/src commons/src

RUN mvn -f commons/pom.xml clean install -DskipTests

COPY comment/pom.xml comment/pom.xml

WORKDIR /app/comment

RUN mvn dependency:go-offline -B

COPY comment/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S comment -u 1001

WORKDIR /app

COPY --from=build --chown=comment:comment /app/comment/target/*.jar comment-service.jar

RUN chmod 444 comment-service.jar

EXPOSE 8080
USER comment

ENTRYPOINT ["java", "-jar", "comment-service.jar"]