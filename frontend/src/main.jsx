
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import keycloak from "./auth/keycloak.js";
import "./index.css";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { CssBaseline } from "@mui/material";

keycloak.init({
    onLoad: "login-required",
    pkceMethod: "S256",
    checkLoginIframe: false
}).then((authenticated) => {

    if (authenticated) {
        // Provides the router context required to block unconfirmed navigation.
        const router = createBrowserRouter([
            { path: "*", element: <App /> }
        ]);
        ReactDOM.createRoot(document.getElementById("root")).render(
            <React.StrictMode>
                <CssBaseline />
                <RouterProvider router={router} />
            </React.StrictMode>
        );
    }

}).catch((error) => {
    console.error("Keycloak initialization failed:", error);

    ReactDOM.createRoot(document.getElementById("root")).render(
        <p>No se pudo conectar con Keycloak. Revisa la consola.</p>
    );
});
