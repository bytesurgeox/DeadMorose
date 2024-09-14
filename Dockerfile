FROM maven:3.8.4-openjdk-17
WORKDIR /app
COPY . /app
RUN mvn clean package
CMD ["java", "-jar", "out/DeadMorose.jar"]