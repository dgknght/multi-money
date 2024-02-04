FROM clojure:lein-2.10.0-jammy AS base
WORKDIR /usr/local/src
COPY . .

FROM base AS with-deps
RUN lein deps

FROM with-deps AS test
RUN lein cloverage

FROM with-deps AS with-sass
RUN apt-get update \
      && apt-get install --yes nodejs npm \
      && npm install --global sass

FROM with-sass AS build
# I believe these can be anything to pass the validation in the library
# and then we can supply correct values at run time
ENV GOOGLE_OAUTH_CLIENT_ID=build-client-id
ENV GOOGLE_OAUTH_CLIENT_SECRET=build-client-secret
RUN mkdir resources/public/css \
  && sass src/scss/site.scss resources/public/css/site.css \
  && lein do fig:prod, uberjar \
  && pwd && ls

# Create a separate image we can create the database
# when ArgoCD syncs the k8s files the first time
FROM with-deps AS create-db
CMD lein create-db

# Create a separate image we can use to run migrations automatically
# when ArgoCD syncs the k8s files
FROM with-deps AS migrate
CMD lein migrate

FROM eclipse-temurin:11-jre-jammy as web
WORKDIR /usr/local/bin
COPY --from=build /usr/local/src/target/multi-money.jar .
CMD ["java", "-cp", "multi-money.jar", "clojure.main", "-m", "multi-money.server"]

# Default port for the service
EXPOSE 3000
