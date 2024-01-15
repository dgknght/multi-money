FROM clojure:lein-2.10.0-jammy as base
WORKDIR /usr/local/src
COPY . .

FROM base as setup
RUN lein deps \
  && apt-get update \
  && apt-get install --yes nodejs npm \
  && npm install --global sass

FROM setup as test
RUN lein cloverage

FROM setup as build
# I believe these can be anything to pass the validation in the library
# and then we can supply correct values at rune time
ENV GOOGLE_OAUTH_CLIENT_ID=build-client-id
ENV GOOGLE_OAUTH_CLIENT_SECRET=build-client-secret
RUN mkdir resources/public/css \
  && sass src/scss/site.scss resources/public/css/site.css \
  && lein do fig:prod, uberjar \
  && pwd && ls

# Create a separate image we can use to run migrations automatically
# when ArgoCD syncs the k8s files
FROM base as migrate
CMD lein migrate

FROM eclipse-temurin:11-jre-jammy as web
WORKDIR /usr/local/bin
COPY --from=build /usr/local/src/target/multi-money.jar .
CMD ["java", "-cp", "multi-money.jar", "clojure.main", "-m", "multi-money.server", "3000"]

EXPOSE 3000
