import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8081",
    realm: import.meta.env.VITE_KEYCLOAK_REALM || "SIGA",
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "siga-frontend"
});

export default keycloak;