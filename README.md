# SIGA IPT — Entrega 1

Aplicación React/Vite y Spring Boot con PostgreSQL y autenticación mediante Keycloak.

## Verificación

```bash
cd backend
./mvnw clean verify
```

El comando ejecuta las pruebas unitarias y de integración y exige al menos 90% de
cobertura de líneas de la capa de servicios. La matriz y los reportes se describen
en [backend/TESTING.md](backend/TESTING.md).

```bash
cd frontend
npm ci
npm run lint
npm run build
```

El perfil `backend/src/test/resources/application-test.properties` debe estar
versionado junto con las pruebas; no necesita el `.env` local ni Keycloak real.

## Continuous Delivery

El workflow [.github/workflows/cd.yml](.github/workflows/cd.yml) ejecuta:

1. Backend: pruebas unitarias, integración y control de cobertura con `clean verify`.
2. Frontend: instalación desde el lockfile, ESLint y compilación.
3. Tras aprobar ambos trabajos, construcción y publicación de las dos imágenes en Docker Hub.

En los pull requests a `main` se ejecutan las verificaciones. En los pushes a `main`
y las ejecuciones manuales sobre `main` también se publican imágenes. Los reportes
del backend quedan disponibles como artefactos de GitHub Actions durante 14 días.
El workflow termina al publicar las imágenes; el despliegue con Compose es manual.

En GitHub, abrir **Settings → Secrets and variables → Actions** y configurar:

| Tipo | Nombre | Valor |
| --- | --- | --- |
| Variable | `DOCKERHUB_USERNAME` | Usuario de Docker Hub, en minúsculas. |
| Secret | `DOCKERHUB_TOKEN` | Token de Docker Hub con permiso para publicar imágenes. |
| Variable | `VITE_API_URL` | URL pública del backend incluyendo `/api`. Por defecto, `http://localhost:8080/api`. |
| Variable | `VITE_KEYCLOAK_URL` | URL pública de Keycloak. Por defecto, `http://localhost:8081`. |
| Variable | `VITE_KEYCLOAK_REALM` | Realm de Keycloak. Por defecto, `SIGA`. |
| Variable | `VITE_KEYCLOAK_CLIENT_ID` | Cliente público del frontend. Por defecto, `siga-frontend`. |

Crear los repositorios `siga-backend` y `siga-frontend` en Docker Hub bajo ese usuario.
El workflow publica estas etiquetas para cada aplicación:

```text
<DOCKERHUB_USERNAME>/siga-backend:latest
<DOCKERHUB_USERNAME>/siga-backend:<commit-sha>
<DOCKERHUB_USERNAME>/siga-frontend:latest
<DOCKERHUB_USERNAME>/siga-frontend:<commit-sha>
```

Los tokens se configuran como secrets de GitHub; no se guardan en el repositorio.
Referencia del flujo de construcción y publicación:
[documentación oficial de Docker para GitHub Actions](https://docs.docker.com/build/ci/github-actions/).

## Imagen del frontend

[frontend/Dockerfile](frontend/Dockerfile) compila con Node 22 y sirve `dist/`
mediante Nginx en el puerto 80. Incluye soporte para rutas de React Router y un
endpoint `/health`. Este Nginx sirve los archivos del frontend; el balanceador
para tres réplicas del backend sigue siendo una tarea independiente.

Compose construye esta imagen y publica el frontend en `http://localhost:5173`.
Para reconstruirla localmente:

```bash
docker compose build frontend
```

Las variables `VITE_*` se incorporan al JavaScript durante la **construcción**.
Cambiar el entorno de un contenedor ya construido no cambia esas direcciones;
hay que reconstruir y publicar otra imagen. Son valores públicos: nunca deben
contener secretos. Para desarrollo con Vite, consultar
[frontend/.env.example](frontend/.env.example).

Para usar las imágenes publicadas, definir en el `.env` del servidor:

```dotenv
BACKEND_IMAGE=<DOCKERHUB_USERNAME>/siga-backend:<commit-sha>
FRONTEND_IMAGE=<DOCKERHUB_USERNAME>/siga-frontend:<commit-sha>
```

Con las demás variables de PostgreSQL y Keycloak ya configuradas, la actualización
manual se realiza con:

```bash
docker compose pull backend frontend
docker compose up -d --no-build backend frontend
```

La configuración de CORS, las URL y redirecciones de Keycloak y la topología con
tres backends y balanceador deben completarse antes del despliegue público.

## Matrícula al registrar estudiantes

El formulario de administración solicita **Matriculado: Sí/No**. Si se elige Sí,
se debe seleccionar un período académico abierto. Se guardan el estudiante y su
matrícula en la misma transacción. Si se elige No, se guarda el estudiante sin
matrícula. La creación de la cuenta continúa en Keycloak.

La matrícula corresponde a un período; no es un atributo permanente del estudiante.
Las inscripciones requieren una matrícula previa y nunca la crean automáticamente.
La matrícula administrativa y la confirmación de asignaturas son independientes.
Los registros existentes de `period_matriculations` se conservan.
Para estudiantes ya registrados, el endpoint administrativo existente
`POST /api/period-matriculations` permite registrar una matrícula por período.

Ejemplo de los campos adicionales enviados al registrar un estudiante matriculado:

```json
{
  "matriculated": true,
  "academicPeriodId": 3
}
```

Para registrar sin matrícula: `"matriculated": false` y `"academicPeriodId": null`.
Estos campos se aplican al registro inicial; las actualizaciones de datos personales
no modifican matrículas. La suma de horas TEL de una asignatura debe ser inferior
a 8 tanto en el formulario como en el backend.
