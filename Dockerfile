FROM openjdk:11.0.2-jre-slim-stretch as builder

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" >> /etc/timezone

RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list \
&& apt-get clean \
&& apt-get update \
&& apt-get install -y curl unzip

RUN curl -o apache-maven-3.8.6-bin.zip "https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.zip"

RUN mkdir maven
RUN unzip apache-maven-3.8.6-bin.zip -d maven

COPY settings.xml /maven/apache-maven-3.8.6/conf/settings.xml

COPY . .
RUN /maven/apache-maven-3.8.6/bin/mvn -DskipTests=true package

FROM openjdk:11.0.2-jre-slim-stretch

COPY --from=builder /authz-grpc-server/target/authz-grpc-server.jar  /bin/

ENV JAVA_OPTS ''

ADD authz-grpc-server/target/authz-grpc-server.jar .

EXPOSE 18081


ENTRYPOINT java -XX:+PrintFlagsFinal \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseContainerSupport \
 $JAVA_OPTS -jar /bin/authz-grpc-server.jar
