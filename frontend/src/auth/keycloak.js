import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
    url: "http://localhost:8081",
    realm: "SIGA",
    clientId: "siga-frontend"
});

export default keycloak;