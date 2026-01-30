# Domu Backend

Aplicativo base construido con **Java 21**, **Gradle** y **Javalin 6** que expone endpoints para registrar y autenticar usuarios. El proyecto viene configurado con un conjunto fijo de dependencias y buenas prácticas de seguridad, incluyendo:

- Pool de conexiones con [HikariCP 5.1.0](https://mvnrepository.com/artifact/com.zaxxer/HikariCP)
- Logging con **SLF4J 2.0.13** y **Log4j 2.23.1** (`log4j-core`, `log4j-api`, `log4j-slf4j2-impl`)
- Encriptado seguro de contraseñas con **BCrypt (org.mindrot:jbcrypt 0.4)**
- Generación de tokens JWT mediante **java-jwt 4.4.0**

## Estructura del proyecto

```
src/
└── main/
    ├── java/com/domu/
    │   ├── Main.java                    # Punto de entrada
    │   ├── config/                      # Configuración cargada desde properties + variables de entorno
    │   ├── database/                    # Fábrica de DataSource y repositorios JDBC
    │   ├── domain/                      # Entidades del dominio organizadas por módulo (core, access, community, finance, facility, staff, ticket, vendor, voting)
    │   ├── dto/                         # Objetos de transporte expuestos vía HTTP
    │   ├── security/                    # Implementaciones de hashing y JWT
    │   ├── service/                     # Casos de uso y lógica de aplicación
    │   └── web/                         # Controladores, mapeadores y servidor Javalin
    └── resources/                       # Configuración, migraciones y logging
```

## Requisitos

- JDK 21
- Docker o un servidor MySQL accesible

## Variables de entorno

Las siguientes variables controlan la configuración en tiempo de ejecución. Todas tienen valores por defecto pensados para desarrollo local, pero **deben** sobrescribirse en producción.

| Variable | Descripción | Valor por defecto |
| --- | --- | --- |
| `DB_HOST` | Host de MySQL (usado si no hay `DB_URI`) | `localhost` |
| `DB_PORT` | Puerto de MySQL | `3306` |
| `DB_NAME` | Base de datos a utilizar | `domu` |
| `DB_USER` | Usuario de la base de datos | `domu` |
| `DB_PASSWORD` | Contraseña de la base de datos | `domu` |
| `DB_URI` | URI JDBC completa (tiene prioridad) | `jdbc:mysql://localhost:3306/domu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `JWT_SECRET` | Llave privada para firmar JWT | `change-this-secret` |
| `JWT_ISSUER` | Issuer que se incluirá en los tokens | `domu-backend` |
| `JWT_EXPIRATION_MINUTES` | Minutos de vigencia del token | `60` |
| `APP_SERVER_PORT` | Puerto HTTP del servidor | `7000` |

## Preparar la base de datos

Crea una base de datos MySQL y un usuario con permisos de lectura/escritura. Por ejemplo:

```sql
CREATE DATABASE domu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'domu'@'%' IDENTIFIED BY 'domu';
GRANT ALL PRIVILEGES ON domu.* TO 'domu'@'%';
FLUSH PRIVILEGES;
```

Antes de iniciar la aplicación ejecuta el script `src/main/resources/migrations/001_create_auth_schema.sql` en tu base de datos para crear las tablas mínimas de autenticación (`rol`, `unidad` y `usuario`). Por ejemplo:

```bash
mysql -u${DB_USER} -p${DB_PASSWORD} -h${DB_HOST} -P${DB_PORT} ${DB_NAME} < src/main/resources/migrations/001_create_auth_schema.sql
```

El modelo relacional completo incorpora además entidades para edificios, unidades, roles, foros comunitarios, personal, turnos, tareas, tickets, módulos financieros, proveedores, reservas, accesos y votaciones, todos representados como `record` en el paquete `com.domu.domain`.

## Ejecutar el proyecto

```bash
./gradlew run
```

El servidor levanta en `http://localhost:7000` (o el puerto definido en `APP_SERVER_PORT`). Endpoints principales:

- `POST /api/auth/register`: registra un nuevo usuario. Ejemplo de cuerpo:
  ```json
  {
    "unitId": 1,
    "roleId": 2,
    "firstName": "Juan",
    "lastName": "Pérez",
    "birthDate": "1995-08-20",
    "email": "juan.perez@example.com",
    "phone": "+56 9 5555 1234",
    "documentNumber": "12.345.678-9",
    "resident": true,
    "password": "UnaClaveMuySegura123!"
  }
  ```
- `POST /api/auth/login`: entrega un token JWT para el usuario autenticado.
- `GET /api/users/me`: requiere header `Authorization: Bearer <token>` y devuelve la información del usuario autenticado.
- `GET /health`: verificación rápida de disponibilidad.

### Ejemplos de curl para autenticación

Registra un usuario de prueba (ajusta los campos según tu caso):

```bash
curl -X POST http://localhost:7000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "unitId": 1,
    "roleId": 2,
    "firstName": "Juan",
    "lastName": "Pérez",
    "birthDate": "1995-08-20",
    "email": "juan.perez@example.com",
    "phone": "+56 9 5555 1234",
    "documentNumber": "12.345.678-9",
    "resident": true,
    "password": "UnaClaveMuySegura123!"
  }'
```

Autentica al usuario y almacena el JWT en una variable `TOKEN` (requiere `jq` instalado):

```bash
TOKEN=$(curl -s -X POST http://localhost:7000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "juan.perez@example.com", "password": "UnaClaveMuySegura123!"}' | jq -r '.token')
echo "Token: $TOKEN"
```

Consulta los datos del usuario autenticado usando el token obtenido:

```bash
curl http://localhost:7000/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

## Pruebas

Ejecuta la suite de pruebas con:

```bash
./gradlew test
```

Esto valida, entre otras cosas, la política de hash de contraseñas basada en BCrypt.

## Notas de seguridad

- Cambia inmediatamente `APP_JWT_SECRET` por un valor robusto generado aleatoriamente antes de desplegar.
- Usa conexiones TLS hacia MySQL en entornos productivos ajustando `APP_JDBC_URL`.
- Considera agregar un servicio de gestión de secretos para credenciales y llaves.
