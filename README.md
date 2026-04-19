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
- No hay evidencia de despliegue real en AWS (Lambda/API Gateway/S3) en este repo.
- No hay evidencia de video demo, links en vivo ni galeria de pruebas.
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

1) Crear tenant en [Auth0](https://auth0.com).
- Ejemplo de dominio: `dev-tu-equipo.us.auth0.com`

2) Crear API (Resource Server).
- Auth0 Dashboard -> Applications -> APIs -> Create API
- Name: `TweetLite API`
- Identifier (Audience): por ejemplo `https://tweetlite-api`
- Signing algorithm: `RS256`

3) Crear scopes recomendados.
- `read:posts`
- `write:posts`
- `read:profile`

4) Crear aplicacion SPA para frontend.
- Applications -> Create Application -> Single Page Application
- Name: `TweetLite Frontend`
- Configurar:
  - Allowed Callback URLs: `http://localhost:3000`
  - Allowed Logout URLs: `http://localhost:3000`
  - Allowed Web Origins: `http://localhost:3000`

5) Copiar y guardar:
- Domain del tenant
- Client ID de la SPA
- Audience del API

## Ejecucion local (monolito + frontend)

### Requisitos
- Java 17+
- Maven 3.8+
- Node.js 18+
- npm 9+

### 1) Backend monolito

```bash
cd monolith
export AUTH0_DOMAIN=dev-tu-equipo.us.auth0.com
export AUTH0_AUDIENCE=https://tweetlite-api
export CORS_ALLOWED_ORIGINS=http://localhost:3000
mvn spring-boot:run
```

URLs:
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2: `http://localhost:8080/h2-console`

### 2) Frontend

```bash
cd frontend
cp .env.example .env
```

Editar `.env`:

```env
REACT_APP_AUTH0_DOMAIN=dev-tu-equipo.us.auth0.com
REACT_APP_AUTH0_CLIENT_ID=TU_CLIENT_ID
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

1) Sin login:
- `GET /api/stream` responde 200.

2) Con login:
- Crear post desde frontend.
- Validar que aparezca en stream.

3) Probar endpoint protegido:
- `GET /api/me` con token valido responde 200.
- sin token responde 401.

4) Swagger:
- abrir `swagger-ui.html`.
- usar `Authorize` con un JWT valido.

## Microservicios implementados en codigo

### Estructura objetivo requerida

```text
microservices/
  posts-service/
    src/main/java/.../PostsHandler.java
    src/main/java/.../JwtValidator.java
  users-service/
    src/main/java/.../UserHandler.java
    src/main/java/.../JwtValidator.java (o libreria compartida)
  stream-service/
    src/main/java/.../StreamHandler.java
```

Estado actual:
- `posts-service`: `PostsHandler` (`GET /posts`, `POST /posts`) + `JwtValidator`.
- `users-service`: `UserHandler` (`GET /me`) + `JwtValidator`.
- `stream-service`: `StreamHandler` (`GET /stream`) publico.
- `UserHandler` ya no esta mezclado dentro de `posts-service`.
- Cada microservicio ya tiene su `pom.xml` y empaquetado para Lambda.

## Despliegue AWS - guia practica

### 1) AWS CLI

```bash
aws configure
aws sts get-caller-identity
```

### 2) DynamoDB

```bash
aws dynamodb create-table \
  --table-name twitter-posts \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1

aws dynamodb create-table \
  --table-name twitter-users \
  --attribute-definitions AttributeName=auth0Id,AttributeType=S \
  --key-schema AttributeName=auth0Id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### 3) Compilar cada microservicio

```bash
cd microservices/posts-service && mvn clean package && cd ../..
cd microservices/users-service && mvn clean package && cd ../..
cd microservices/stream-service && mvn clean package && cd ../..
```

### 4) Crear Lambdas

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

aws lambda create-function \
  --function-name tweetlite-posts \
  --runtime java17 \
  --handler co.edu.escuelaing.PostsHandler::handleRequest \
  --zip-file fileb://microservices/posts-service/target/posts-service-1.0-SNAPSHOT.jar \
  --role arn:aws:iam::$ACCOUNT_ID:role/LabRole \
  --timeout 30 \
  --memory-size 512 \
  --environment "Variables={POSTS_TABLE=twitter-posts,AUTH0_DOMAIN=dev-tu-equipo.us.auth0.com,AUTH0_AUDIENCE=https://tweetlite-api}"
```

Crear de forma similar:

```bash
aws lambda create-function \
  --function-name tweetlite-users \
  --runtime java17 \
  --handler co.edu.escuelaing.UserHandler::handleRequest \
  --zip-file fileb://microservices/users-service/target/users-service-1.0-SNAPSHOT.jar \
  --role arn:aws:iam::$ACCOUNT_ID:role/LabRole \
  --timeout 30 \
  --memory-size 512 \
  --environment "Variables={USERS_TABLE=twitter-users,AUTH0_DOMAIN=dev-tu-equipo.us.auth0.com,AUTH0_AUDIENCE=https://tweetlite-api}"

aws lambda create-function \
  --function-name tweetlite-stream \
  --runtime java17 \
  --handler co.edu.escuelaing.StreamHandler::handleRequest \
  --zip-file fileb://microservices/stream-service/target/stream-service-1.0-SNAPSHOT.jar \
  --role arn:aws:iam::$ACCOUNT_ID:role/LabRole \
  --timeout 30 \
  --memory-size 512 \
  --environment "Variables={POSTS_TABLE=twitter-posts}"
```

### 5) API Gateway

Configurar rutas:
- `GET /posts` -> posts lambda (publico)
- `POST /posts` -> posts lambda (jwt)
- `GET /stream` -> stream lambda (publico)
- `GET /me` -> users lambda (jwt)

Habilitar CORS y desplegar stage `prod`.

### 6) Frontend en S3

```bash
cd frontend
npm run build
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET=tweetlite-$ACCOUNT_ID
aws s3 mb s3://$BUCKET --region us-east-1
aws s3 website s3://$BUCKET --index-document index.html --error-document index.html
aws s3 sync build/ s3://$BUCKET --delete
echo "http://$BUCKET.s3-website-us-east-1.amazonaws.com"
```

Para Auth0 en produccion, usar HTTPS (CloudFront o dominio propio) y registrar esa URL en Allowed Callback/Logout/Web Origins.

## Checklist de entrega final (rubrica)

- [x] Monolito con endpoints principales y seguridad Auth0.
- [x] Swagger/OpenAPI accesible.
- [x] Frontend con login/logout y creacion de posts.
- [x] Refactor completo a 3 microservicios independientes (en codigo).
- [ ] Despliegue real en Lambda + API Gateway.
- [ ] Frontend publicado en S3 (idealmente HTTPS con CloudFront).
- [x] README tecnico con pasos completos de ejecucion/despliegue.
- [ ] README final con links reales, evidencias y pruebas de ejecucion.
- [ ] Video demo 5-8 minutos.

## Seguridad y buenas practicas

- No subir `.env`, secretos ni tokens al repo.
- Usar variables de entorno para `AUTH0_DOMAIN`, `AUTH0_AUDIENCE`, `CLIENT_ID`.
- Validar JWT siempre por issuer + audience.
- Mantener endpoints publicos y protegidos segun la rubrica.
