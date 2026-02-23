FROM maven:3.9-amazoncorretto-21-al2023 AS build
ARG WORK_DIR
WORKDIR /app

# copy the project files
COPY ${WORK_DIR}/pom.xml ./pom.xml
COPY build/maven/start.sh ./start.sh

# not useful for stateless builds
# RUN mvn -B dependency:go-offline

COPY ${WORK_DIR}/src ./src
RUN mvn -B -f /app/pom.xml package


# Create runtime image
FROM amazoncorretto:21-alpine-jdk

WORKDIR /opt/arghyam

COPY --from=build /app/target/*.jar /app/start.sh /opt/arghyam/

RUN chmod +x /opt/arghyam/start.sh

CMD ["/opt/arghyam/start.sh"]
