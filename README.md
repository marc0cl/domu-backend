# Domu Backend

Aplicativo base construido con **Java 21**, **Gradle** y **Javalin 6** que expone endpoints para registrar y autenticar usuarios. El proyecto viene configurado con un conjunto fijo de dependencias y buenas prácticas de seguridad, incluyendo:

- Pool de conexiones con [HikariCP 5.1.0](https://mvnrepository.com/artifact/com.zaxxer/HikariCP)
- Logging con **SLF4J 2.0.13** y **Log4j 2.23.1** (`log4j-core`, `log4j-api`, `log4j-slf4j2-impl`)
- Encriptado seguro de contraseñas con **BCrypt (org.mindrot:jbcrypt 0.4)**
- Generación de tokens JWT mediante **java-jwt 4.4.0**

## Requisitos

- JDK 21
- Docker o un servidor MySQL accesible

## Variables de entorno

Las siguientes variables controlan la configuración en tiempo de ejecución. Todas tienen valores por defecto pensados para desarrollo local, pero **deben** sobrescribirse en producción.

| Variable | Descripción | Valor por defecto |
| --- | --- | --- |
| `APP_JDBC_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/domu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `APP_DB_USER` | Usuario de la base de datos | `domu` |
| `APP_DB_PASSWORD` | Contraseña de la base de datos | `domu` |
| `APP_JWT_SECRET` | Llave privada para firmar JWT | `change-this-secret` |
| `APP_JWT_ISSUER` | Issuer que se incluirá en los tokens | `domu-backend` |
| `APP_JWT_EXP_MINUTES` | Minutos de vigencia del token | `60` |
| `APP_SERVER_PORT` | Puerto HTTP del servidor | `7070` |

## Preparar la base de datos

Crea una base de datos MySQL y un usuario con permisos de lectura/escritura. Por ejemplo:

```sql
CREATE DATABASE domu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'domu'@'%' IDENTIFIED BY 'domu';
GRANT ALL PRIVILEGES ON domu.* TO 'domu'@'%';
FLUSH PRIVILEGES;
```

Antes de iniciar la aplicación ejecuta el script `app/src/main/resources/db/migration/V1__create_usuario_table.sql` en tu base de datos para crear la tabla `usuario` con los campos:

- `id_usuario` (PK, auto incremental)
- `id_unidad` (FK opcional)
- `id_rol` (FK opcional)
- `nombres`, `apellidos`
- `fecha_nacimiento`
- `correo` (único)
- `password_hash`

## Ejecutar el proyecto

```bash
./gradlew :app:run
```

El servidor levanta en `http://localhost:7070`. Endpoints principales:

- `POST /api/auth/register`: registra un nuevo usuario. Ejemplo de cuerpo:
  ```json
  {
    "unitId": 1,
    "roleId": 2,
    "firstName": "Juan",
    "lastName": "Pérez",
    "birthDate": "1995-08-20",
    "email": "juan.perez@example.com",
    "password": "UnaClaveMuySegura123!"
  }
  ```
- `POST /api/auth/login`: entrega un token JWT para el usuario autenticado.
- `GET /api/users/me`: requiere header `Authorization: Bearer <token>` y devuelve la información del usuario autenticado.
- `GET /health`: verificación rápida de disponibilidad.

## Pruebas

Ejecuta la suite de pruebas con:

```bash
./gradlew :app:test
```

Esto valida, entre otras cosas, la política de hash de contraseñas basada en BCrypt.

## Notas de seguridad

- Cambia inmediatamente `APP_JWT_SECRET` por un valor robusto generado aleatoriamente antes de desplegar.
- Usa conexiones TLS hacia MySQL en entornos productivos ajustando `APP_JDBC_URL`.
- Considera agregar un servicio de gestión de secretos para credenciales y llaves.
