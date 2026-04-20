# TweetLite - Monolito + Migracion a Microservicios

Proyecto para el taller de Arquitecturas Empresariales: aplicacion tipo Twitter con seguridad JWT usando Auth0, evolucionando de monolito Spring Boot a microservicios en AWS Lambda.

## Estado actual real del repositorio

### Lo que ya cumple

- Monolito Spring Boot con entidades `User` y `Post`.
- Endpoints base en monolito:
  - `GET /api/posts` (publico)
  - `GET /api/stream` (publico)
  - `POST /api/posts` (protegido)
  - `GET /api/me` (protegido)
- Validacion de longitud maxima de post (140 caracteres).
- Seguridad como OAuth2 Resource Server (Auth0 + validacion de `audience`).
- OpenAPI/Swagger configurado con Springdoc.
- Frontend React con Auth0 (`@auth0/auth0-react`), login/logout, ver stream y crear posts.

### Lo que falta para cumplir 100% la rubrica

- Falta completar y dejar documentado el despliegue real en AWS (DynamoDB + Lambda + API Gateway + S3) y dejar los links finales en este README.
- Falta evidencia final (capturas/links) del despliegue y pruebas en AWS, y el video demo.
- Falta ejecutar y documentar pruebas finales en tu entorno local/AWS.
- Falta completar configuracion real de Auth0 con datos de tu tenant.

## Cambio importante realizado en esta version

Se actualizo `monolith/src/main/resources/application.yml` para usar variables de entorno reales:

- `AUTH0_DOMAIN`
- `AUTH0_AUDIENCE`
- `CORS_ALLOWED_ORIGINS`

Esto evita dejar valores fijos y te permite ejecutar el proyecto con configuracion por entorno.

## Variables de entorno explicadas (la duda de `application.yml` linea 23-24)

En Spring Boot, esta parte:

- `issuer-uri`: URL del tenant Auth0 que emite los tokens.
- `audiences`: identificador del API en Auth0 (tu audience).

Quedo asi:

- `issuer-uri: https://${AUTH0_DOMAIN:YOUR_AUTH0_DOMAIN}/`
- `audiences: ${AUTH0_AUDIENCE:YOUR_AUTH0_AUDIENCE}`

Significa:

- Si exportas `AUTH0_DOMAIN`, usa ese valor.
- Si no existe, usa el texto por defecto (`YOUR_AUTH0_DOMAIN`) y fallara validacion (esperado en desarrollo si no configuraste Auth0).

## Paso a paso - Auth0 desde cero

1. Crear tenant en [Auth0](https://auth0.com).

- Ejemplo de dominio: `dev-tu-equipo.us.auth0.com`

1. Crear API (Resource Server).

- Auth0 Dashboard -> Applications -> APIs -> Create API
- Name: `TweetLite API`
- Identifier (Audience): por ejemplo `https://tweetlite-api`
- Signing algorithm: `RS256`
- Importante: copia el Identifier exactamente igual (sin cambiar slash final).

1. Crear scopes recomendados.

- `read:posts`
- `write:posts`
- `read:profile`

1. Crear aplicacion SPA para frontend.

- Applications -> Create Application -> Single Page Application
- Name: `TweetLite Frontend`
- Configurar:
  - Allowed Callback URLs: `http://localhost:3000`
  - Allowed Logout URLs: `http://localhost:3000`
  - Allowed Web Origins: `http://localhost:3000`

1. Copiar y guardar:

- Domain del tenant
- Client ID de la SPA
- Audience del API

1. Autorizar la SPA contra tu API (esto corrige el error de "client is not authorized").

- En Auth0: `Applications -> APIs -> TweetLite API -> Applications`
- Busca tu SPA (`TweetLite Frontend`) y habilitala (Authorize/Toggle ON).
- Si no haces esto, al oprimir Sign in aparece:
  - `Client "<client_id>" is not authorized to access resource server "<audience>"`

## Ejecucion local (monolito + frontend)

### Requisitos

- Java 17+
- Maven 3.8+
- Node.js 18+
- npm 9+

### 1) Backend monolito en CMD

```bash
cd monolith
set AUTH0_DOMAIN=dev-wtyv3mytuxckqffv.us.auth0.com
set AUTH0_AUDIENCE=https://tweetlite-api
set CORS_ALLOWED_ORIGINS=http://localhost:3000
mvn spring-boot:run
```

URLs:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html` o `http://localhost:8080/swagger-ui/index.html`
- H2: `http://localhost:8080/h2-console`

### 2) Frontend

```bash
cd frontend
cp .env.example .env
```

Editar `.env`:

```env
REACT_APP_AUTH0_DOMAIN=dev-wtyv3mytuxckqffv.us.auth0.com
REACT_APP_AUTH0_CLIENT_ID=Mr2Tjbd41ZOZP5XKfzMwFcqkLncUGnmW
REACT_APP_AUTH0_AUDIENCE=https://tweetlite-api
REACT_APP_API_URL=http://localhost:8080
```

Ejecutar:

```bash
npm install
npm start
```

URL:

- `http://localhost:3000`

## Verificacion funcional minima

1. Sin login:

- `GET /api/stream` responde 200.

1. Con login:

- Crear post desde frontend.
- Validar que aparezca en stream.

1. Probar endpoint protegido:

- `GET /api/me` con token valido responde 200.
- sin token responde 401.

1. Swagger:

- abrir `swagger-ui.html`.
- usar `Authorize` con un JWT valido.

## Donde sacar el JWT para Swagger 

En este proyecto necesitas el **access token** (JWT) que Auth0 emite para tu API (audience `https://tweetlite-api`), no el `id_token`.

### Auth0 Dashboard (copiar token de prueba)

1. Auth0 -> `Applications -> APIs -> TweetLite API`.
2. Pestaña **Test**.
3. Selecciona tu aplicacion SPA.
4. Copia el **Access Token** (a veces aparece como `access_token`).

Ese string largo es el que pegas en Swagger en `Authorize` (sin la palabra `Bearer` si Swagger ya lo agrega; si te pide el valor completo, usa `Bearer <token>` segun lo que muestre el cuadro).

### Pruebas en Swagger

1. Abre `http://localhost:8080/swagger-ui/index.html`.
2. Verifica que cargue la documentacion (si no carga, revisa seccion "Problemas comunes").
3. Prueba publico:
  - `GET /api/stream` -> debe responder `200`.

    ![img1.png](images/img1.png)

4. Prueba protegido sin token:
  - `POST /api/posts` -> debe responder `401`.

    ![img2.png](images/img2.png)

5. Obtén un access token (ver seccion **Donde sacar el JWT** arriba).

    ![img3.png](images/img3.png)
    
6. En Swagger pulsa `Authorize` y pega el token (segun el formato que te pida Swagger).

    ![img4.png](images/img4.png)
    
7. Prueba protegido con token:
  - `POST /api/posts` con body `{"content":"hola"}` -> `200`/`201` segun endpoint.

  ![img5.png](images/img5.png)

  - `GET /api/me` -> `200`.

  ![img6.png](images/img6.png)

## Microservicios implementados en codigo

### Estructura objetivo requerida

```text
microservices/
  posts-service/
    src/main/java/co/edu/escuelaing/PostsHandler.java
    src/main/java/co/edu/escuelaing/JwtValidator.java
  users-service/
    src/main/java/co/edu/escuelaing/UserHandler.java
    src/main/java/co/edu/escuelaing/JwtValidator.java
  stream-service/
    src/main/java/co/edu/escuelaing/StreamHandler.java
```

Estado actual:

- `posts-service`: `PostsHandler` (`GET /posts`, `POST /posts`) + `JwtValidator`.
- `users-service`: `UserHandler` (`GET /me`) + `JwtValidator`.
- `stream-service`: `StreamHandler` (`GET /stream`) publico.
- `UserHandler` ya no esta mezclado dentro de `posts-service`.
- Cada microservicio ya tiene su `pom.xml` y empaquetado para Lambda.

## Despliegue AWS (PowerShell) - guia practica end-to-end

> Esta guia esta escrita para **Windows PowerShell** (tu entorno). Si usas Git Bash, puedes adaptar variables, pero evita mezclar sintaxis.

### Requisitos (AWS)

- AWS CLI instalado (`aws --version`)
- Credenciales configuradas (`aws configure`)
- Permisos para: DynamoDB, Lambda, API Gateway, S3
- Rol de ejecucion para Lambda. En ambientes de laboratorio suele existir `LabRole` (si no tienes permisos de IAM para crear roles, usa el rol pre-creado).

### Variables (define todo una vez)

En PowerShell, define estos valores (ajusta `AUTH0_DOMAIN`/`AUTH0_AUDIENCE`):

```powershell
$REGION = "us-east-1"
$AUTH0_DOMAIN = "dev-tu-equipo.us.auth0.com"
$AUTH0_AUDIENCE = "https://tweetlite-api"

$POSTS_TABLE = "twitter-posts"
$USERS_TABLE = "twitter-users"
```

Verifica tu cuenta:

```powershell
aws sts get-caller-identity
```

Guarda tu Account ID en variable:

```powershell
$ACCOUNT_ID = (aws sts get-caller-identity --query Account --output text)
```

### 1) DynamoDB (tablas)

> Importante: estas tablas deben coincidir con lo que usa el codigo:
> - Posts usa `id` como PK (string).
> - Users usa `auth0Id` como PK (string).

Crear tabla de posts:

```powershell
aws dynamodb create-table `
  --table-name $POSTS_TABLE `
  --attribute-definitions AttributeName=id,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST `
  --region $REGION
```

Crear tabla de users:

```powershell
aws dynamodb create-table `
  --table-name $USERS_TABLE `
  --attribute-definitions AttributeName=auth0Id,AttributeType=S `
  --key-schema AttributeName=auth0Id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST `
  --region $REGION
```

Validar que existen:

```powershell
aws dynamodb list-tables --region $REGION
```

### 2) Compilar (fat JAR) cada microservicio

```powershell
cd microservices\posts-service
mvn clean package
cd ..\..

cd microservices\users-service
mvn clean package
cd ..\..

cd microservices\stream-service
mvn clean package
cd ..\..
```

Los JAR quedan en:

- `microservices/posts-service/target/posts-service-1.0-SNAPSHOT.jar`
- `microservices/users-service/target/users-service-1.0-SNAPSHOT.jar`
- `microservices/stream-service/target/stream-service-1.0-SNAPSHOT.jar`

### 3) Crear Lambdas

> Si tu laboratorio no permite crear roles IAM, usa `LabRole`. Si no existe, crea un rol con permisos de DynamoDB + logs y actualiza el ARN.

Primero define el ARN del rol:

```powershell
$LAMBDA_ROLE_ARN = "arn:aws:iam::$ACCOUNT_ID:role/LabRole"
```

Crear `tweetlite-posts`:

```powershell
aws lambda create-function `
  --function-name tweetlite-posts `
  --runtime java17 `
  --handler co.edu.escuelaing.PostsHandler::handleRequest `
  --zip-file fileb://microservices/posts-service/target/posts-service-1.0-SNAPSHOT.jar `
  --role $LAMBDA_ROLE_ARN `
  --timeout 30 `
  --memory-size 512 `
  --environment "Variables={POSTS_TABLE=$POSTS_TABLE,AUTH0_DOMAIN=$AUTH0_DOMAIN,AUTH0_AUDIENCE=$AUTH0_AUDIENCE}" `
  --region $REGION
```

Crear `tweetlite-users`:

```powershell
aws lambda create-function `
  --function-name tweetlite-users `
  --runtime java17 `
  --handler co.edu.escuelaing.UserHandler::handleRequest `
  --zip-file fileb://microservices/users-service/target/users-service-1.0-SNAPSHOT.jar `
  --role $LAMBDA_ROLE_ARN `
  --timeout 30 `
  --memory-size 512 `
  --environment "Variables={USERS_TABLE=$USERS_TABLE,AUTH0_DOMAIN=$AUTH0_DOMAIN,AUTH0_AUDIENCE=$AUTH0_AUDIENCE}" `
  --region $REGION
```

Crear `tweetlite-stream`:

```powershell
aws lambda create-function `
  --function-name tweetlite-stream `
  --runtime java17 `
  --handler co.edu.escuelaing.StreamHandler::handleRequest `
  --zip-file fileb://microservices/stream-service/target/stream-service-1.0-SNAPSHOT.jar `
  --role $LAMBDA_ROLE_ARN `
  --timeout 30 `
  --memory-size 512 `
  --environment "Variables={POSTS_TABLE=$POSTS_TABLE}" `
  --region $REGION
```

Si ya existen (porque intentaste antes), actualiza el codigo con:

```powershell
aws lambda update-function-code --function-name tweetlite-posts --zip-file fileb://microservices/posts-service/target/posts-service-1.0-SNAPSHOT.jar --region $REGION
aws lambda update-function-code --function-name tweetlite-users --zip-file fileb://microservices/users-service/target/users-service-1.0-SNAPSHOT.jar --region $REGION
aws lambda update-function-code --function-name tweetlite-stream --zip-file fileb://microservices/stream-service/target/stream-service-1.0-SNAPSHOT.jar --region $REGION
```

### 4) API Gateway (REST API) - rutas + CORS

En consola AWS (recomendado para evitar errores de CLI):

1. API Gateway -> **REST API** -> Create API
2. Nombre: `TweetLiteAPI`
3. Resources:
   - `/posts`
     - `GET` (Lambda proxy -> `tweetlite-posts`)
     - `POST` (Lambda proxy -> `tweetlite-posts`)
     - `OPTIONS` (habilitar CORS)
   - `/stream`
     - `GET` (Lambda proxy -> `tweetlite-stream`)
     - `OPTIONS` (habilitar CORS)
   - `/me`
     - `GET` (Lambda proxy -> `tweetlite-users`)
     - `OPTIONS` (habilitar CORS)

4. En cada recurso, usa **Enable CORS** (asegurate de incluir `Authorization` en headers).
5. Deploy API -> stage: `prod`

Al final tendras un base URL asi:

`https://{apiId}.execute-api.{region}.amazonaws.com/prod`

Endpoints:

- `GET  /prod/stream` (publico)
- `GET  /prod/posts` (publico)
- `POST /prod/posts` (JWT)
- `GET  /prod/me` (JWT)

> Nota: este proyecto valida JWT **dentro de la Lambda** (`JwtValidator`), por eso no necesitas authorizer de API Gateway. Si tu profe exige authorizer, puedes agregar JWT authorizer como mejora, pero no es obligatorio si la validacion ya se hace.

### 5) Probar API Gateway rapido (curl / Postman)

Publico:

- `GET /stream` debe responder `200` sin token.

Protegido:

- `POST /posts` y `GET /me` deben responder `401` sin `Authorization: Bearer <token>`.

## Frontend en S3 (CRA build/)

Tu frontend es **Create React App**, por eso el output de build es la carpeta `build/` (no `dist/`).

### 1) Configurar variables de entorno para produccion

Crea `frontend/.env.production` (no lo subas al repo) con:

```env
REACT_APP_AUTH0_DOMAIN=dev-tu-equipo.us.auth0.com
REACT_APP_AUTH0_CLIENT_ID=TU_CLIENT_ID_DE_SPA
REACT_APP_AUTH0_AUDIENCE=https://tweetlite-api
REACT_APP_API_URL=https://TU_API_ID.execute-api.us-east-1.amazonaws.com/prod
```

### 2) Build

```powershell
cd frontend
npm install
npm run build
```

### 3) Crear bucket S3 y publicar

```powershell
cd ..

$BUCKET = "tweetlite-$ACCOUNT_ID"

aws s3 mb "s3://$BUCKET" --region $REGION

aws s3 website "s3://$BUCKET" --index-document index.html --error-document index.html

aws s3 sync "frontend/build/" "s3://$BUCKET" --delete

"http://$BUCKET.s3-website-$REGION.amazonaws.com"
```

> Si tu cuenta bloquea acceso publico (muy comun en laboratorios), vas a necesitar habilitar policy/ACL. Si no te dan permisos, deja evidencia del intento y usa el plan alterno HTTPS (abajo) para cumplir Auth0 en produccion.

## Importante: Auth0 + S3 Website (HTTP) y el problema de CloudFront

Auth0 **en produccion** normalmente requiere **HTTPS** (solo permite `http://localhost` en desarrollo). El endpoint de S3 static website es **HTTP**, por eso:

- Puede que el hosting en S3 funcione, pero el login/redirect de Auth0 falle si usas el URL HTTP del bucket como callback/origin.
- CloudFront solucionaria esto (HTTPS), pero si tu cuenta/lab no permite CloudFront, necesitas un plan B.

### Plan B recomendado (sin CloudFront): HTTPS via DuckDNS + Caddy reverse proxy

Este camino suele funcionar incluso cuando CloudFront da `AccessDenied`. La idea es:

- S3 sigue alojando tu frontend (cumples la rubrica “deploy en S3”).
- Un servidor pequeño (EC2/Lightsail/VPS) te da **HTTPS** y sirve como reverse proxy hacia el endpoint HTTP de S3.
- Auth0 se configura con tu dominio HTTPS (DuckDNS).

#### 1) Crear dominio DuckDNS

1. Crea un subdominio, por ejemplo: `tweetlite-grupoX.duckdns.org`
2. Apuntalo al IP publico de tu server (EC2/Lightsail/VPS).

#### 2) Instalar Caddy (Ubuntu) y configurar reverse proxy

En el server:

```bash
sudo apt update
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf https://dl.cloudsmith.io/public/caddy/stable/gpg.key | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install -y caddy
```

Edita `/etc/caddy/Caddyfile`:

```text
tweetlite-grupoX.duckdns.org {
  reverse_proxy http://tweetlite-ACCOUNTID.s3-website-us-east-1.amazonaws.com {
    header_up Host tweetlite-ACCOUNTID.s3-website-us-east-1.amazonaws.com
  }
}
```

Reinicia Caddy:

```bash
sudo systemctl restart caddy
sudo systemctl status caddy
```

En el Security Group del server abre puertos 80 y 443.

Tu frontend quedara en:

`https://tweetlite-grupoX.duckdns.org`

#### 3) Configurar Auth0 con el HTTPS final

En tu SPA (Auth0 Dashboard -> Applications -> TweetLite Frontend):

- Allowed Callback URLs: `https://tweetlite-grupoX.duckdns.org`
- Allowed Logout URLs: `https://tweetlite-grupoX.duckdns.org`
- Allowed Web Origins: `https://tweetlite-grupoX.duckdns.org`

Luego rebuild del frontend con:

- `REACT_APP_API_URL=https://TU_API_ID.execute-api.../prod`

Y vuelve a `aws s3 sync` del `frontend/build/`.

## Checklist de entrega final (rubrica)

- Monolito con endpoints principales y seguridad Auth0.
- Swagger/OpenAPI accesible.
- Frontend con login/logout y creacion de posts.
- Refactor completo a 3 microservicios independientes (en codigo).
- Despliegue real en Lambda + API Gateway.
- Frontend publicado en S3 (si CloudFront falla, usar HTTPS con DuckDNS + reverse proxy).
- README tecnico con pasos completos de ejecucion/despliegue (este documento).
- Actualizar este README con links reales:
  - Frontend (S3 y/o HTTPS DuckDNS)
  - API Gateway base URL
  - Swagger del monolito (local o deploy)
- Video demo 5-8 minutos.

## Seguridad y buenas practicas

- No subir `.env`, secretos ni tokens al repo.
- Usar variables de entorno para `AUTH0_DOMAIN`, `AUTH0_AUDIENCE`, `CLIENT_ID`.
- Validar JWT siempre por issuer + audience.
- Mantener endpoints publicos y protegidos segun la rubrica.

