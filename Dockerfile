FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY *.java ./
RUN javac *.java
EXPOSE 8080
CMD ["sh", "-c", "java ApiServer ${PORT:-8080}"]
