# TweetLite — Aplicación tipo Twitter con Microservicios y Auth0

Proyecto para el taller de Arquitecturas Empresariales de la Escuela Colombiana de Ingeniería Julio Garavito. Aplicación tipo Twitter con seguridad JWT usando Auth0, que evoluciona de un monolito Spring Boot a microservicios serverless en AWS Lambda.

---

## Descripción general

TweetLite permite a usuarios autenticados publicar mensajes cortos (máximo 140 caracteres) en un feed público global. El proyecto parte de un monolito Spring Boot y se migra a tres microservicios independientes desplegados en AWS Lambda con API Gateway, frontend React en S3, y autenticación completa con Auth0.

---

## Arquitectura final

```
[Usuario] 
    │
    ▼
[https://tweetlite.duckdns.org]   ← EC2 (Amazon Linux) + Caddy (HTTPS reverse proxy)
    │
    ▼
[S3 Static Website]               ← Frontend React (Create React App)
    │
    ▼
[Auth0]                           ← Login / Logout / JWT tokens
    │
    ▼
[API Gateway HTTP API]            ← https://m4ro71bgz4.execute-api.us-east-1.amazonaws.com/prod
    │
    ├── GET  /api/stream   ──►  Lambda: tweetlite-stream   ──►  DynamoDB: twitter-posts
    ├── GET  /api/posts    ──►  Lambda: tweetlite-posts    ──►  DynamoDB: twitter-posts
    ├── POST /api/posts    ──►  Lambda: tweetlite-posts    ──►  DynamoDB: twitter-posts
    └── GET  /api/me       ──►  Lambda: tweetlite-users    ──►  DynamoDB: twitter-users
```

**Flujo de seguridad:**
1. El usuario inicia sesión en el frontend con Auth0.
2. Auth0 emite un JWT (access token) con audience `https://tweetlite-api`.
3. El frontend envía el token en el header `Authorization: Bearer <token>`.
4. Cada Lambda valida el JWT internamente usando `JwtValidator` (verifica issuer + audience + firma RS256).
5. Endpoints públicos (`GET /stream`, `GET /posts`) no requieren token.
6. Endpoints protegidos (`POST /posts`, `GET /me`) retornan 401 si no hay token válido.

---

## Links del proyecto desplegado

| Recurso | URL |
|---|---|
| Frontend (HTTPS) | https://tweetlite.duckdns.org |
| Frontend (S3 HTTP) | http://tweetlite-962733155713.s3-website-us-east-1.amazonaws.com |
| API Gateway base URL | https://m4ro71bgz4.execute-api.us-east-1.amazonaws.com/prod |
| Swagger UI (monolito local) | http://localhost:8080/swagger-ui/index.html |

---

## Estructura del repositorio

```
tweetlite/
├── monolith/                         # Spring Boot monolito
│   └── src/main/java/co/edu/escuelaing/
├── microservices/
│   ├── posts-service/                # Lambda: GET y POST /posts
│   │   └── src/main/java/co/edu/escuelaing/
│   │       ├── PostsHandler.java
│   │       └── JwtValidator.java
│   ├── users-service/                # Lambda: GET /me
│   │   └── src/main/java/co/edu/escuelaing/
│   │       ├── UserHandler.java
│   │       └── JwtValidator.java
│   └── stream-service/               # Lambda: GET /stream (público)
│       └── src/main/java/co/edu/escuelaing/
│           └── StreamHandler.java
└── frontend/                         # React + Auth0 SDK
    ├── src/
    ├── .env
    └── build/                        # Output del build (desplegado en S3)
```

---

## Configuración de Auth0

### 1. Crear API (Resource Server)

- Auth0 Dashboard → Applications → APIs → Create API
- **Name:** `TweetLite API`
- **Identifier (Audience):** `https://tweetlite-api`
- **Signing algorithm:** RS256
- Scopes recomendados: `read:posts`, `write:posts`, `read:profile`

### 2. Crear aplicación SPA

- Applications → Create Application → **Single Page Application**
- **Name:** `TweetLite Frontend`
- Settings:
    - **Allowed Callback URLs:** `https://tweetlite.duckdns.org`
    - **Allowed Logout URLs:** `https://tweetlite.duckdns.org`
    - **Allowed Web Origins:** `https://tweetlite.duckdns.org`

### 3. Autorizar la SPA contra la API

- Applications → APIs → TweetLite API → pestaña **Applications**
- Habilitar (toggle ON) la SPA `TweetLite Frontend`

### 4. Valores usados en el proyecto

| Variable | Valor |
|---|---|
| `AUTH0_DOMAIN` | `dev-wtyv3mytuxckqffv.us.auth0.com` |
| `AUTH0_AUDIENCE` | `https://tweetlite-api` |
| `REACT_APP_AUTH0_CLIENT_ID` | `Mr2Tjbd41ZOZP5XKfzMwFcqkLncUGnmW` |

---

## Ejecución local (monolito + frontend)

### Requisitos

- Java 17+
- Maven 3.8+
- Node.js 18+
- npm 9+

### Backend (monolito Spring Boot) — desde CMD

```cmd
cd monolith
set AUTH0_DOMAIN=dev-wtyv3mytuxckqffv.us.auth0.com
set AUTH0_AUDIENCE=https://tweetlite-api
set CORS_ALLOWED_ORIGINS=http://localhost:3000
mvn spring-boot:run
```

URLs disponibles localmente:
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- H2 Console: `http://localhost:8080/h2-console`

### Frontend — desde CMD o PowerShell

```bash
cd frontend
npm install
npm start
```

URL: `http://localhost:3000`

### Variables de entorno del frontend (`frontend/.env`)

```env
REACT_APP_AUTH0_DOMAIN=dev-wtyv3mytuxckqffv.us.auth0.com
REACT_APP_AUTH0_CLIENT_ID=Mr2Tjbd41ZOZP5XKfzMwFcqkLncUGnmW
REACT_APP_AUTH0_AUDIENCE=https://tweetlite-api
REACT_APP_API_URL=http://localhost:8080
```

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



---

## Despliegue completo en AWS (PowerShell — Windows)

### Prerrequisitos

- AWS CLI instalado (`aws --version`)
- Credenciales del laboratorio configuradas (ver paso 1)
- Java 17 y Maven instalados
- Node.js 18+ instalado

---

### Paso 1 — Configurar credenciales AWS

En AWS Academy / Learner Lab, al iniciar la sesión aparece el botón **"AWS Details"** o **"Show"** junto a "AWS CLI". Copiá las credenciales y pegálas en el archivo de credenciales:

```powershell
notepad "$env:USERPROFILE\.aws\credentials"
```

El archivo debe quedar así (reemplazá con los valores de tu sesión actual):

```
[default]
aws_access_key_id=TU_ACCESS_KEY_ID
aws_secret_access_key=TU_SECRET_ACCESS_KEY
aws_session_token=TU_SESSION_TOKEN
```

> **Importante:** Las credenciales del lab expiran al cerrar la sesión. Cada vez que abras el lab debés actualizar este archivo con las nuevas credenciales.

Verificá que funcionan:

```powershell
aws sts get-caller-identity
```

Respuesta esperada:

```json
{
    "UserId": "...",
    "Account": "962733155713",
    "Arn": "arn:aws:sts::962733155713:assumed-role/voclabs/..."
}
```

---

### Paso 2 — Definir variables de entorno en PowerShell

Ejecutá este bloque completo en la misma ventana de PowerShell que usarás durante todo el despliegue. Si cerrás la ventana, debés volver a ejecutarlo:

```powershell
$REGION = "us-east-1"
$AUTH0_DOMAIN = "dev-wtyv3mytuxckqffv.us.auth0.com"
$AUTH0_AUDIENCE = "https://tweetlite-api"
$POSTS_TABLE = "twitter-posts"
$USERS_TABLE = "twitter-users"
$ACCOUNT_ID = (aws sts get-caller-identity --query Account --output text)
$LAMBDA_ROLE_ARN = "arn:aws:iam::" + $ACCOUNT_ID + ":role/LabRole"
```

Verificá que todo quedó bien:

```powershell
echo $ACCOUNT_ID         # 962733155713
echo $LAMBDA_ROLE_ARN    # arn:aws:iam::962733155713:role/LabRole
```

---

### Paso 3 — Crear tablas DynamoDB

```powershell
aws dynamodb create-table `
  --table-name $POSTS_TABLE `
  --attribute-definitions AttributeName=id,AttributeType=S `
  --key-schema AttributeName=id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST `
  --region $REGION

aws dynamodb create-table `
  --table-name $USERS_TABLE `
  --attribute-definitions AttributeName=auth0Id,AttributeType=S `
  --key-schema AttributeName=auth0Id,KeyType=HASH `
  --billing-mode PAY_PER_REQUEST `
  --region $REGION
```

Verificar que se crearon:

```powershell
aws dynamodb list-tables --region $REGION
```

Resultado esperado: `["twitter-posts", "twitter-users"]`

> Si alguna tabla ya existe, el comando da error `ResourceInUseException`. Eso es normal, la tabla ya está creada y podés continuar.

---

### Paso 4 — Compilar los microservicios (fat JAR)

Navegá a la raíz del proyecto y compilá cada microservicio:

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

Cada compilación debe terminar con `BUILD SUCCESS`. Los JARs quedan en:

- `microservices/posts-service/target/posts-service-1.0-SNAPSHOT.jar`
- `microservices/users-service/target/users-service-1.0-SNAPSHOT.jar`
- `microservices/stream-service/target/stream-service-1.0-SNAPSHOT.jar`

---

### Paso 5 — Crear o actualizar las Lambdas

**Si es la primera vez (crear):**

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

**Si las Lambdas ya existen (actualizar código):**

```powershell
aws lambda update-function-code --function-name tweetlite-posts --zip-file fileb://microservices/posts-service/target/posts-service-1.0-SNAPSHOT.jar --region $REGION

aws lambda update-function-code --function-name tweetlite-users --zip-file fileb://microservices/users-service/target/users-service-1.0-SNAPSHOT.jar --region $REGION

aws lambda update-function-code --function-name tweetlite-stream --zip-file fileb://microservices/stream-service/target/stream-service-1.0-SNAPSHOT.jar --region $REGION
```

---

### Paso 6 — API Gateway

La API Gateway se configura desde la consola web de AWS (más confiable que la CLI para este paso).

1. Abrí la consola de AWS → buscá **API Gateway** → entrá
2. Verificá si ya existe `tweetlite-api`. Si existe, abrila. Si no, creá una nueva **HTTP API**.
3. Las rutas necesarias son:

| Método | Ruta | Lambda |
|---|---|---|
| GET | /api/stream | tweetlite-stream |
| GET | /api/posts | tweetlite-posts |
| POST | /api/posts | tweetlite-posts |
| GET | /api/me | tweetlite-users |

4. Para cada ruta: clic en la ruta → **Attach integration** → Lambda → seleccioná la función correspondiente.
5. En el menú izquierdo → **CORS** → configurá:
    - **Allow origins:** `https://tweetlite.duckdns.org`
    - **Allow headers:** `Content-Type, Authorization`
    - **Allow methods:** `GET, POST, OPTIONS`
    - Guardá
6. En **Stages** → verificá que existe el stage `prod` con **Automatic Deployment** habilitado.

La URL base de la API queda: `https://m4ro71bgz4.execute-api.us-east-1.amazonaws.com/prod`

> **Nota:** La validación de JWT se hace dentro de cada Lambda usando `JwtValidator`, por lo que no se necesita un authorizer en API Gateway.

---

### Paso 7 — Build y deploy del frontend en S3

Creá el archivo `frontend/.env` con los valores de producción:

```env
REACT_APP_AUTH0_DOMAIN=dev-wtyv3mytuxckqffv.us.auth0.com
REACT_APP_AUTH0_CLIENT_ID=Mr2Tjbd41ZOZP5XKfzMwFcqkLncUGnmW
REACT_APP_AUTH0_AUDIENCE=https://tweetlite-api
REACT_APP_API_URL=https://m4ro71bgz4.execute-api.us-east-1.amazonaws.com/prod
```

Build y deploy:

```powershell
cd frontend
npm install
npm run build
cd ..

$BUCKET = "tweetlite-" + $ACCOUNT_ID

aws s3 mb "s3://$BUCKET" --region $REGION

aws s3 website "s3://$BUCKET" --index-document index.html --error-document index.html

aws s3 sync "frontend/build/" "s3://$BUCKET" --delete
```

La URL del bucket S3 queda: `http://tweetlite-962733155713.s3-website-us-east-1.amazonaws.com`

> **Problema conocido:** Auth0 requiere HTTPS en producción. El endpoint de S3 es HTTP, por lo que el login con Auth0 falla. Solución en el paso siguiente.

---

### Paso 8 — HTTPS con EC2 + Caddy + DuckDNS (Plan B sin CloudFront)

Este paso agrega HTTPS al frontend. S3 sigue sirviendo los archivos estáticos, pero la EC2 actúa como reverse proxy HTTPS.

#### 8.1 Crear dominio en DuckDNS

1. Entrá a https://www.duckdns.org e iniciá sesión con Google o GitHub
2. Creá un subdominio, por ejemplo `tweetlite`
3. En el campo **current ip** ingresá la IP pública de la EC2 (se obtiene en el siguiente paso)
4. Clic en **update ip**

El dominio quedará así: `tweetlite.duckdns.org`

#### 8.2 Crear EC2 (Amazon Linux 2023)

Desde la consola AWS → EC2 → **Launch instance**:

- **Name:** `tweetlite-proxy`
- **AMI:** Amazon Linux 2023 AMI (primera opción por defecto)
- **Instance type:** `t2.micro`
- **Key pair:** el `.pem` del lab (Download PEM desde la pantalla del lab)
- **Security group:** abrir puertos:
    - SSH (22) — My IP
    - HTTP (80) — Anywhere
    - HTTPS (443) — Anywhere

Lanzar la instancia y copiar la **IP pública** (ej: `3.218.72.189`). Actualizar esa IP en DuckDNS.

#### 8.3 Conectarse por SSH

```powershell
ssh -i "C:\Users\USUARIO\Downloads\labsuser.pem" ec2-user@3.218.72.189
```

#### 8.4 Instalar Caddy (binario directo)

```bash
curl -fsSL https://github.com/caddyserver/caddy/releases/download/v2.9.1/caddy_2.9.1_linux_amd64.tar.gz -o caddy.tar.gz
tar -xzf caddy.tar.gz
sudo mv caddy /usr/local/bin/
caddy version
```

#### 8.5 Configurar Caddy como reverse proxy

```bash
sudo mkdir -p /etc/caddy
sudo nano /etc/caddy/Caddyfile
```

Contenido del Caddyfile:

```
tweetlite.duckdns.org {
  reverse_proxy http://tweetlite-962733155713.s3-website-us-east-1.amazonaws.com {
    header_up Host tweetlite-962733155713.s3-website-us-east-1.amazonaws.com
  }
}
```

Guardar con `Ctrl+X` → `Y` → Enter.

Iniciar Caddy:

```bash
sudo caddy start --config /etc/caddy/Caddyfile
```

Caddy obtiene automáticamente el certificado SSL de Let's Encrypt. El frontend queda disponible en: `https://tweetlite.duckdns.org`

#### 8.6 Actualizar Auth0 con la URL HTTPS

En Auth0 Dashboard → Applications → TweetLite Frontend → Settings:

- **Allowed Callback URLs:** `https://tweetlite.duckdns.org`
- **Allowed Logout URLs:** `https://tweetlite.duckdns.org`
- **Allowed Web Origins:** `https://tweetlite.duckdns.org`

Guardar cambios.

---

## Verificación funcional

### Endpoints públicos (sin token)

```bash
# Debe responder 200
curl https://m4ro71bgz4.execute-api.us-east-1.amazonaws.com/prod/api/stream
curl https://m4ro71bgz4.execute-api.us-east-1.amazonaws.com/prod/api/posts
```

### Endpoints protegidos (sin token)

```bash
# Deben responder 401
curl -X POST https://m4ro71bgz4.execute-api.us-east-1.amazonaws.com/prod/api/posts
curl https://m4ro71bgz4.execute-api.us-east-1.amazonaws.com/prod/api/me
```

### Frontend

1. Abrir `https://tweetlite.duckdns.org`
2. Clic en **Sign In** → redirige a Auth0 → iniciar sesión
3. Ver el feed de posts públicos
4. Publicar un nuevo post (máximo 140 caracteres)
5. Verificar que el post aparece en el feed

### Swagger (monolito local)

1. Levantar el monolito localmente (ver sección ejecución local)
2. Abrir `http://localhost:8080/swagger-ui/index.html`
3. Clic en **Authorize** → pegar el access token de Auth0
4. Probar `GET /api/stream` → 200
5. Probar `POST /api/posts` sin token → 401
6. Probar `POST /api/posts` con token y body `{"content": "hola"}` → 201
7. Probar `GET /api/me` con token → 200

---

## Microservicios — Resumen técnico

| Microservicio | Handler | Rutas | DynamoDB |
|---|---|---|---|
| posts-service | `PostsHandler` | GET /api/posts, POST /api/posts | twitter-posts |
| users-service | `UserHandler` | GET /api/me | twitter-users |
| stream-service | `StreamHandler` | GET /api/stream | twitter-posts (lectura) |

Cada microservicio incluye su propio `JwtValidator` que valida el JWT directamente contra el JWKS de Auth0 (`https://{domain}/.well-known/jwks.json`), verificando issuer y audience.

---

## Buenas prácticas de seguridad

- No subir `.env`, secretos ni tokens al repositorio
- Usar variables de entorno para `AUTH0_DOMAIN`, `AUTH0_AUDIENCE`, `CLIENT_ID`
- Las credenciales de AWS del lab tienen expiración — nunca commitearlas
- JWT validado siempre por issuer + audience + firma RS256
- Endpoints públicos y protegidos claramente diferenciados según la rúbrica

---

## Checklist de entrega

- ✅ Monolito Spring Boot con endpoints principales y seguridad Auth0
- ✅ Swagger/OpenAPI accesible en `/swagger-ui/index.html`
- ✅ Frontend React con login/logout, ver feed y crear posts
- ✅ Refactor completo a 3 microservicios independientes
- ✅ Despliegue real en Lambda + API Gateway
- ✅ Frontend publicado en S3 con HTTPS via DuckDNS + Caddy
- ✅ README técnico con pasos completos de ejecución y despliegue
- ⬜ Video demo (5–8 minutos)