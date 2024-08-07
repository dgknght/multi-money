# multi-money

[![Clojure CI](https://github.com/dgknght/multi-money/actions/workflows/clojure.yml/badge.svg)](https://github.com/dgknght/multi-money/actions/workflows/clojure.yml)

Double-entry accounting system written in Clojure and designed to
use multiple back-end storage strategies.

## Overview

This project is a combination of my desire to have a cloud-based,
easy-to-use accounting tool, and a vehicle with which I test programming
tools and strategies.

### Models
```mermaid
erDiagram
  user ||--|{ identity : "is identified by"
  user {
    string given-name
    string surname
    string email
  }
  identity {
    string oauth-id
    string oauth-provider
  }
  user }|--|| entity : "has one or more"
  entity {
    string name
    user owner
    date first-trx-date
    date last-trx-date
    commodity default-commodity
  }
  entity }|--|| commodity : "has one or more"
  commodity {
    string name
    string symbol
    keyword type
    entity entity
  }
  entity }|--|| account : "has many"
  account {
    string name
    keyword type
    entity entity
    commodity commodity
  }
  transaction }|--|| entity : "has many"
  transaction {
    local-date date
    string description
  }
  transaction-item }|--|| transaction : "has one or more"
  transaction-item {
    account debit-account
    account credit-account
    money quantity
  }
  transaction-item ||--|| account : "debits"
  transaction-item ||--|| account : "credits"
```
## Setup

### OAuth

#### Google
Set the following environment variables
- GOOGLE_OAUTH_CLIENT_ID
- GOOGLE_OAUTH_CLIENT_SECRET

### Docker
Create a file at `docker-compose/config/config.edn`. Start with the contents
of `config/test/config.edn` and edit as necessary.

Start the necessary services with docker compose
```bash
docker compose up -d --profile all
```
The compose file will launch jobs that initialize each data storage strategy.

Optionally, you can specify one of the following profiles:
- sql
- mongo
- datomic-peer
- datomic-client

### Test the server
```bash
lein test
```

### Front End
Install sass
```bash
apt-get install nodejs npm
npm install --global sass
```

Build the stylesheet with sass
```bash
sass src/scss/site.scss resources/public/css/site.css
```

To get an interactive development environment run:
```bash
lein fig:build
```

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:
```bash
(js/alert "Am I connected?")
```

and you should see an alert in the browser window.

To clean all compiled files:
```bash
lein clean
```

To create a production build run:
```bash
lein do clean, fig:min
```

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
