FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY commons/pom.xml commons/pom.xml
COPY commons/src commons/src

RUN mvn -f commons/pom.xml clean install -DskipTests

COPY search/pom.xml search/pom.xml

WORKDIR /app/search

RUN mvn dependency:go-offline -B

COPY search/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S search -u 1001

WORKDIR /app

COPY --from=build --chown=search:search /app/search/target/*.jar search-service.jar

RUN chmod 444 search-service.jar

EXPOSE 8087
USER search

ENTRYPOINT ["java", "-jar", "search-service.jar"]
