FROM clojure:lein-2.10.0-jammy AS datomic-pro
WORKDIR /tmp

# Install maven
ENV M2_HOME /opt/maven
ENV PATH $M2_HOME/bin:$PATH

RUN <<EOF
apt-get update
apt-get install -y curl unzip
curl https://mirrors.estointernet.in/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -O
tar -xvf apache-maven-3.6.3-bin.tar.gz
mv apache-maven-3.6.3 /opt/maven
mvn -version
EOF

# Install datomic
RUN <<EOF
curl https://datomic-pro-downloads.s3.amazonaws.com/1.0.7075/datomic-pro-1.0.7075.zip -O
unzip datomic-pro-1.0.7075.zip
EOF
WORKDIR /tmp/datomic-pro-1.0.7075
RUN bin/maven-install

FROM datomic-pro AS util
RUN mkdir -p /usr/local/src/multi-money
WORKDIR /usr/local/src/multi-money
COPY project.clj .
RUN <<EOF
apt-get update
apt-get install -y postgresql-client
EOF
COPY . .

FROM util AS build
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
