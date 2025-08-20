# KeyCloak integration

## Prerequisites

### Install KeyCloak on Docker

`docker pull quay.io/keycloak/keycloak:26.3.2`

Run keyCloak on port 8180

`docker run -d --name keycloak-26.3.2 -p 8180:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=251101 quay.io/keycloak/keycloak:26.3.2 start-dev`