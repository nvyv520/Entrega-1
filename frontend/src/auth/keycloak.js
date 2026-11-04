import Keycloak from "keycloak-js";

// configure the Keycloak client using the build environment
const keycloak = new Keycloak({
    url: new URL(import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8081", window.location.origin).href.replace(/\/$/, ""),
    realm: import.meta.env.VITE_KEYCLOAK_REALM || "SIGA",
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "siga-frontend"
});

export default keycloak;