# TweetLite — Microservices Twitter Clone

**Escuela Colombiana de Ingeniería Julio Garavito**  
Proyecto: Taller AREP — Arquitecturas Seguras con Microservicios y Auth0

---

## Descripción

TweetLite es una aplicación tipo Twitter simplificada que implementa:
- Un **stream público único** donde los usuarios autenticados pueden publicar posts de máximo 140 caracteres.
- Arquitectura que evoluciona de **monolito Spring Boot** a **microservicios serverless en AWS Lambda**.
- Seguridad completa mediante **Auth0** (JWT, OAuth2 Resource Server).

---

## Arquitectura

### Fase 1 — Monolito
```
Browser (React) ──→ Spring Boot Monolith (8080)
                         ├── /api/stream      [público]
                         ├── /api/posts GET   [público]
                         ├── /api/posts POST  [Auth JWT]
                         ├── /api/me          [Auth JWT]
                         └── /swagger-ui.html
                         H2 in-memory DB
Auth0 ──── JWT validation ──→ Spring Security OAuth2 Resource Server
```

### Fase 2 — Microservicios en AWS
```
                    ┌─────────────────────────────────────┐
Browser (React)     │           AWS API Gateway            │
hosted on S3  ────→ │  /users  /posts  /stream            │
                    └──────┬──────────┬──────────┬────────┘
                           ↓          ↓          ↓
                    Lambda         Lambda      Lambda
                 UserHandler   PostsHandler StreamHandler
                           ↓          ↓          ↓
                    DynamoDB    DynamoDB     DynamoDB
                    (users)     (posts)      (posts-read)
Auth0 ──── JWT ──→ Validation inside each Lambda handler
```

### Diagrama de flujo Auth0
```
User → Login → Auth0 Universal Login
Auth0 → JWT Access Token → Frontend
Frontend → Bearer Token → API Gateway / Spring Boot
Backend → Validate JWT (JWKS endpoint) → 200 / 401
```

---

## Configuración inicial — Auth0

### 1. Crear una cuenta en Auth0
- Ve a https://auth0.com y crea una cuenta gratuita.

### 2. Crear una SPA Application
- Dashboard → Applications → Create Application
- Nombre: `TweetLite Frontend`
- Tipo: **Single Page Application**
- En Settings:
    - **Allowed Callback URLs**: `http://localhost:3000, https://TU_S3_URL`
    - **Allowed Logout URLs**: `http://localhost:3000, https://TU_S3_URL`
    - **Allowed Web Origins**: `http://localhost:3000, https://TU_S3_URL`
- Copia el `Domain` y el `Client ID`.

### 3. Crear una API en Auth0
- Dashboard → APIs → Create API
- Nombre: `TweetLite API`
- Identifier (Audience): `https://api.tweetlite.com`
- En Permissions agrega:
    - `read:posts`
    - `write:posts`
    - `read:profile`

---

## Ejecución Local

### Pre-requisitos
- Java 17+
- Maven 3.8+
- Node.js 18+
- npm 9+

### 1. Monolito Spring Boot

```bash
cd monolith

# Edita application.yml con tus credenciales Auth0
# auth0.domain: tu-tenant.auth0.com
# auth0.audience: https://api.tweetlite.com

mvn spring-boot:run
```

Disponible en: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html  
H2 Console: http://localhost:8080/h2-console

### 2. Frontend React

```bash
cd frontend
cp .env.example .env

# Edita .env:
# REACT_APP_AUTH0_DOMAIN=tu-tenant.auth0.com
# REACT_APP_AUTH0_CLIENT_ID=tu_client_id
# REACT_APP_AUTH0_AUDIENCE=https://api.tweetlite.com
# REACT_APP_API_URL=http://localhost:8080

npm install
npm start
```

Disponible en: http://localhost:3000

---

## Despliegue en AWS

### A) Frontend en S3

```bash
cd frontend
npm run build

# Crear bucket S3
aws s3 mb s3://tweetlite-frontend-TUUNIQUEID

# Habilitar static website hosting
aws s3 website s3://tweetlite-frontend-TUUNIQUEID \
  --index-document index.html \
  --error-document index.html

# Política pública del bucket (guarda como policy.json)
cat > policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "PublicReadGetObject",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::tweetlite-frontend-TUUNIQUEID/*"
  }]
}
EOF

aws s3api put-bucket-policy \
  --bucket tweetlite-frontend-TUUNIQUEID \
  --policy file://policy.json

# Subir build
aws s3 sync build/ s3://tweetlite-frontend-TUUNIQUEID --delete
```

URL: `http://tweetlite-frontend-TUUNIQUEID.s3-website-us-east-1.amazonaws.com`

### B) Tablas DynamoDB

```bash
# Tabla de posts
aws dynamodb create-table \
  --table-name TweetLitePosts \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

# Tabla de usuarios
aws dynamodb create-table \
  --table-name TweetLiteUsers \
  --attribute-definitions AttributeName=auth0Id,AttributeType=S \
  --key-schema AttributeName=auth0Id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

### C) Microservicio Posts — Lambda

```bash
cd microservices/posts-service
mvn package

# Crear función Lambda
aws lambda create-function \
  --function-name TweetLitePostsService \
  --runtime java17 \
  --handler co.edu.escuelaing.PostsHandler::handleRequest \
  --role arn:aws:iam::TU_ACCOUNT_ID:role/LabRole \
  --zip-file fileb://target/posts-service-1.0-SNAPSHOT.jar \
  --timeout 30 \
  --memory-size 512 \
  --environment Variables="{POSTS_TABLE=TweetLitePosts,AUTH0_DOMAIN=tu-tenant.auth0.com,AUTH0_AUDIENCE=https://api.tweetlite.com}"
```

### D) Microservicio Users — Lambda

```bash
cd microservices/user-service
mvn package

aws lambda create-function \
  --function-name TweetLiteUserService \
  --runtime java17 \
  --handler co.edu.escuelaing.UserHandler::handleRequest \
  --role arn:aws:iam::TU_ACCOUNT_ID:role/LabRole \
  --zip-file fileb://target/user-service-1.0-SNAPSHOT.jar \
  --timeout 30 \
  --memory-size 512 \
  --environment Variables="{USERS_TABLE=TweetLiteUsers,AUTH0_DOMAIN=tu-tenant.auth0.com,AUTH0_AUDIENCE=https://api.tweetlite.com}"
```

### E) API Gateway

1. Abre AWS Console → API Gateway → Create API → REST API → New API
2. Nombre: `TweetLiteAPI` → Regional
3. Crea los recursos:
    - `/posts` → Methods: GET (PostsHandler), POST (PostsHandler), OPTIONS
    - `/stream` → Method: GET (PostsHandler), OPTIONS
    - `/users/me` → Method: GET (UserHandler), OPTIONS
4. Para cada método Lambda:
    - Integration type: Lambda Function
    - Use Lambda Proxy integration: 
5. En **Integration Request** de POST /posts:
    - Ya que usamos proxy, el JWT llega directo al handler
6. Deploy → New Stage → `prod`

Para CORS en cada recurso:
- Actions → Enable CORS → Yes

URL final: `https://XXXXX.execute-api.us-east-1.amazonaws.com/prod`

---

## Pruebas

### Pruebas unitarias del monolito

```bash
cd monolith
mvn test
```

**Casos cubiertos:**
- `GET /api/stream` sin autenticación → 200 
- `POST /api/posts` sin token → 401 
- `POST /api/posts` con JWT válido → 200 
- `POST /api/posts` con contenido >140 chars → 400
- `GET /api/me` sin token → 401
- `GET /api/me` con JWT válido → 200

### Pruebas con curl

```bash
# 1. Stream público (sin auth)
curl http://localhost:8080/api/stream

# 2. Crear post sin auth (debe fallar)
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"content":"test"}'
# → 401 Unauthorized

# 3. Obtener token de Auth0 (para pruebas manuales)
# Ve a Auth0 Dashboard → APIs → TweetLite API → Test → Copy Token

TOKEN="eyJ..."

# 4. Crear post con auth
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Hola desde TweetLite!"}'

# 5. Mi perfil
curl http://localhost:8080/api/me \
  -H "Authorization: Bearer $TOKEN"

# 6. Post muy largo (debe fallar)
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}'
# → 400 Bad Request
```

### Pruebas de las Lambdas en AWS Console

Para cada función Lambda:
1. AWS Console → Lambda → Test
2. Usa los templates de **API Gateway Proxy**
3. Para GET `/posts`: deja el body vacío, sin Authorization
4. Para POST `/posts`: añade `"Authorization": "Bearer TOKEN"` en headers y body con content

---

## Estructura del repositorio

```
tweetlite/
├── monolith/
│   ├── pom.xml
│   └── src/
│       ├── main/java/co/edu/escuelaing/tweetlite/
│       │   ├── TweetliteApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── AudienceValidator.java
│       │   │   └── OpenApiConfig.java
│       │   ├── controller/
│       │   │   ├── PostController.java
│       │   │   └── UserController.java
│       │   ├── dto/
│       │   ├── model/
│       │   ├── repository/
│       │   └── service/
│       └── test/
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   │   ├── Header.js / .css
│   │   │   ├── Feed.js / .css
│   │   │   ├── PostCard.js / .css
│   │   │   ├── PostComposer.js / .css
│   │   │   └── LoginPage.js / .css
│   │   ├── App.js / .css
│   │   └── index.js
│   ├── .env.example
│   └── package.json
├── microservices/
│   ├── posts-service/
│   │   ├── pom.xml
│   │   └── src/main/java/co/edu/escuelaing/
│   │       ├── PostsHandler.java
│   │       └── JwtValidator.java
│   ├── user-service/
│   │   ├── pom.xml
│   │   └── src/main/java/co/edu/escuelaing/
│   │       └── UserHandler.java
│   └── stream-service/
│       ├── pom.xml
│       └── src/main/java/co/edu/escuelaing/
│           └── StreamHandler.java
└── README.md
```

---

## Links

- Frontend en S3: `http://tweetlite-frontend-TUUNIQUEID.s3-website-us-east-1.amazonaws.com`
- Swagger UI (monolito local): `http://localhost:8080/swagger-ui.html`
- Video demo:

---

## Notas de seguridad

- **No** subir al repositorio: `.env`, claves de AWS, secrets de Auth0
- Usar variables de entorno o AWS Secrets Manager para credenciales
- El LabRole de AWS Academy ya tiene permisos para Lambda, DynamoDB y S3
- Los JWT son validados en cada Lambda sin estado compartido

---

## Integrantes

| Nombre                          | GitHub |
|---------------------------------|--------|
| Daniel Esteban Rodriguez Suarez | [@usuario] |
| Tomas Espitia                   | [@] |
| Laura Natalia Perilla Quintero  | [@] |
