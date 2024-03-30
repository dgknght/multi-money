FROM clojure:lein-2.10.0-jammy AS util
RUN mkdir -p /usr/local/src/multi-money
WORKDIR /usr/local/src/multi-money
COPY project.clj .
RUN <<EOF
echo "{:dev {}}" > profiles.clj
lein deps
EOF
COPY . .

FROM with-sass AS build
RUN <<EOF
apt-get update
apt-get install --yes nodejs npm
npm install --global sass
EOF

# these can be anything to pass the validation in the library
# The correct values will be supplied to the container
ENV GOOGLE_OAUTH_CLIENT_ID=build-client-id
ENV GOOGLE_OAUTH_CLIENT_SECRET=build-client-secret
RUN <<EOF
mkdir resources/public/css
sass src/scss/site.scss resources/public/css/site.css
lein do fig:prod, uberjar
EOF

FROM eclipse-temurin:11-jre-jammy as web
WORKDIR /usr/local/bin
COPY --from=build /usr/local/src/multi-money/target/multi-money.jar .
CMD ["java", "-cp", "multi-money.jar", "clojure.main", "-m", "multi-money.server"]

# Default port for the service
EXPOSE 3000
