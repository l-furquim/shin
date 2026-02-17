FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY commons/pom.xml commons/pom.xml
COPY commons/src commons/src

RUN mvn -f commons/pom.xml clean install -DskipTests

COPY upload/pom.xml upload/pom.xml

WORKDIR /app/upload

RUN mvn dependency:go-offline -B

COPY upload/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S upload -u 1001

WORKDIR /app

COPY --from=build --chown=upload:upload /app/upload/target/*.jar upload-service.jar

RUN chmod 444 upload-service.jar

EXPOSE 8080
USER upload

ENTRYPOINT ["java", "-jar", "upload-service.jar"]
