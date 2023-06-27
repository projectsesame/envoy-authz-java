FROM maven:3.9.1 AS builder
WORKDIR /app

COPY ./  ./
RUN mvn clean package -DskipTests

FROM openjdk:11.0.2-jre-slim-stretch

COPY --from=builder /app/authz-grpc-server/target/authz-grpc-server.jar  /bin/

EXPOSE 18081

ENTRYPOINT java -XX:+PrintFlagsFinal \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseContainerSupport \
 $JAVA_OPTS -jar /bin/authz-grpc-server.jar
