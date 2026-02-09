FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY Backend/App.java .
COPY Backend/postgresql.jar .
COPY Frontend/index.html .
RUN javac App.java
CMD ["java","-cp",".:postgresql.jar","App"]