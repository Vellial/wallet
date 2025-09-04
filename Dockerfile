FROM openjdk:21-jdk-bullseye
COPY ./demo-0.0.1-SNAPSHOT.jar /app/app.jar
#  locale-gen
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar",  "--spring.config.location=optional:classpath:/,optional:file:config/"]