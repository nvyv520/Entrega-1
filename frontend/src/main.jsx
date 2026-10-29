
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import keycloak from "./auth/keycloak.js";
import "./index.css";

keycloak.init({
    onLoad: "login-required",
    pkceMethod: "S256",
    checkLoginIframe: false
}).then((authenticated) => {

    if (authenticated) {
        ReactDOM.createRoot(document.getElementById("root")).render(
            <React.StrictMode>
                <App />
            </React.StrictMode>
        );
    }

}).catch((error) => {
    console.error("Keycloak initialization failed:", error);

    ReactDOM.createRoot(document.getElementById("root")).render(
        <p>No se pudo conectar con Keycloak. Revisa la consola.</p>
    );
});
