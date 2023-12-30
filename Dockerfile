FROM eclipse-temurin:21.0.1_12-jre-jammy

# copy our application into the image
WORKDIR /usr/local/bin
COPY ./target/multi-money.jar .
CMD ["java", "-cp", "multi-money.jar", "clojure.main", "-m", "multi-money.server", "3204"]

EXPOSE 3204
