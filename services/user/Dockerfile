FROM maven:3.9.4-amazoncorretto-21 AS build

WORKDIR /app

COPY parent-pom/pom.xml parent-pom/pom.xml

RUN mvn -f parent-pom/pom.xml install -DskipTests

COPY commons/pom.xml commons/pom.xml
COPY commons/src commons/src

RUN mvn -f commons/pom.xml clean install -DskipTests

COPY user/pom.xml user/pom.xml

WORKDIR /app/user

RUN mvn dependency:go-offline -B

COPY user/src/ src/

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine

RUN adduser -S user -u 1001

WORKDIR /app

COPY --from=build --chown=user:user /app/user/target/*.jar user-service.jar

RUN chmod 444 user-service.jar

EXPOSE 8080
USER user

ENTRYPOINT ["java", "-jar", "user-service.jar"]