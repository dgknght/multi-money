FROM clojure:lein-2.10.0-jammy AS base
WORKDIR /usr/local/src
COPY . .

FROM base AS util
RUN lein deps

FROM util AS test
RUN lein cloverage

FROM util AS with-sass
RUN apt-get update \
      && apt-get install --yes nodejs npm \
      && npm install --global sass

FROM with-sass AS build
# these can be anything to pass the validation in the library
# The correct values will be supplied to the container
ENV GOOGLE_OAUTH_CLIENT_ID=build-client-id
ENV GOOGLE_OAUTH_CLIENT_SECRET=build-client-secret
RUN mkdir resources/public/css \
  && sass src/scss/site.scss resources/public/css/site.css \
  && lein do fig:prod, uberjar \
  && pwd && ls

FROM eclipse-temurin:11-jre-jammy as web
WORKDIR /usr/local/bin
COPY --from=build /usr/local/src/target/multi-money.jar .
CMD ["java", "-cp", "multi-money.jar", "clojure.main", "-m", "multi-money.server"]

# Default port for the service
EXPOSE 3000
