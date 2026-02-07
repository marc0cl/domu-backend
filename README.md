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

> **Nota:** El archivo `pom.xml` incluido en el proyecto es obsoleto y no debe utilizarse. El proyecto se gestiona exclusivamente con **Gradle** (`build.gradle.kts`).

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

## Ejemplos de curl para visitas

> Requiere haber aplicado las migraciones `005_visits.sql` y contar con un usuario residente (con `unitId` asociado) o un conserje/admin para asignar la unidad manualmente.

Registrar visita (residente: la unidad se toma del usuario):
```bash
curl -X POST http://localhost:7000/api/visits \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "visitorName": "María Soto",
    "visitorDocument": "12345678-9",
    "visitorType": "VISIT",
    "validForMinutes": 120
  }'
```

Registrar visita asignando unidad (conserje/admin, usa `unitId` explícito):
```bash
curl -X POST http://localhost:7000/api/visits \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "visitorName": "Proveedor Técnico",
    "visitorDocument": "11111111-1",
    "visitorType": "PROVEEDOR",
    "unitId": 12,
    "validForMinutes": 90
  }'
```

Listar mis visitas (residente ve las propias; admin/conserje ve las que registró):
```bash
curl http://localhost:7000/api/visits/my \
  -H "Authorization: Bearer $TOKEN"
```

Marcar ingreso de una visita (usa `authorizationId` devuelto al crear/listar):
```bash
curl -X POST http://localhost:7000/api/visits/1/check-in \
  -H "Authorization: Bearer $TOKEN"
```

## Ejemplos de curl para incidentes

> Requiere haber aplicado la migración `006_incidents.sql`.

Reportar incidente (residente o admin/conserje):
```bash
curl -X POST http://localhost:7000/api/incidents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Fuga de agua en pasillo",
    "description": "Se observa agua corriendo por el ducto del piso 3",
    "category": "maintenance",
    "priority": "HIGH"
  }'
```

Listar incidentes propios (residente) o todos (admin/conserje) con filtro de fechas opcional:
```bash
# sin filtro
curl http://localhost:7000/api/incidents/my \
  -H "Authorization: Bearer $TOKEN"

# con rango de fechas (YYYY-MM-DD)
curl "http://localhost:7000/api/incidents/my?from=2025-01-01&to=2025-01-31" \
  -H "Authorization: Bearer $TOKEN"
```

## Ejemplos de curl para gastos comunes (GGCC)

> Requiere haber aplicado la migración `016_ggcc_enhancements.sql`. Para acciones de administrador necesitas enviar el header `X-Building-Id` con el edificio seleccionado en el frontend.

Crear un periodo de GGCC (admin):
```bash
curl -X POST http://localhost:7000/api/finance/periods \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Building-Id: 7" \
  -d '{
    "period": "2025-10",
    "dueDate": "2025-10-10",
    "notes": "Gasto común Octubre"
  }'
```

Agregar cargos al periodo (admin):
```bash
curl -X POST http://localhost:7000/api/finance/periods/12/charges \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Building-Id: 7" \
  -d '{
    "charges": [
      {
        "unitId": 21,
        "category": "Mantención",
        "description": "Aseo y jardinería",
        "amount": 23500,
        "origin": "PROVEEDOR",
        "paid": false
      }
    ]
  }'
```

Subir boleta para un cargo (admin, archivo opcional):
```bash
curl -X POST http://localhost:7000/api/finance/charges/55/receipt \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Building-Id: 7" \
  -F "file=@/path/a/boleta.pdf"
```

Listar periodos creados (admin):
```bash
curl http://localhost:7000/api/finance/periods \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Building-Id: 7"
```

Listar periodos disponibles para el residente:
```bash
curl http://localhost:7000/api/finance/my-periods \
  -H "Authorization: Bearer $TOKEN"
```

Ver detalle del periodo para el residente:
```bash
curl http://localhost:7000/api/finance/my-periods/12 \
  -H "Authorization: Bearer $TOKEN"
```

Descargar PDF del periodo (residente):
```bash
curl -o ggcc-2025-10.pdf \
  http://localhost:7000/api/finance/my-periods/12/pdf \
  -H "Authorization: Bearer $TOKEN"
```

Descargar boleta de un cargo (residente):
```bash
curl -o boleta-55.pdf \
  http://localhost:7000/api/finance/charges/55/receipt \
  -H "Authorization: Bearer $TOKEN"
```

## Ejemplos de curl para gestión de unidades

> Requiere rol de administrador.

Listar unidades del edificio seleccionado:
```bash
curl http://localhost:7000/api/admin/housing-units \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Building-Id: 7"
```

Crear una nueva unidad:
```bash
curl -X POST http://localhost:7000/api/admin/housing-units \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Building-Id: 7" \
  -H "Content-Type: application/json" \
  -d '{
    "number": "301",
    "tower": "A",
    "floor": "3",
    "aliquotPercentage": 1.5,
    "squareMeters": 85.5
  }'
```

Vincular un residente a una unidad:
```bash
curl -X POST http://localhost:7000/api/admin/housing-units/15/residents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "userId": 42 }'
```

## Ejemplos de curl para votaciones (Polls)

Listar votaciones activas:
```bash
curl "http://localhost:7000/api/polls?status=OPEN" \
  -H "Authorization: Bearer $TOKEN"
```

Crear una nueva votación (Admin):
```bash
curl -X POST http://localhost:7000/api/polls \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Elección de color de fachada",
    "description": "Votación para decidir el nuevo color del edificio",
    "closesAt": "2025-12-31T23:59:00",
    "options": ["Blanco Invierno", "Gris Urbano", "Terracota"]
  }'
```

Emitir un voto:
```bash
curl -X POST http://localhost:7000/api/polls/5/votes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "optionId": 12 }'
```

## Ejemplos de curl para áreas comunes (Amenities)

Listar áreas comunes disponibles:
```bash
curl http://localhost:7000/api/amenities \
  -H "Authorization: Bearer $TOKEN"
```

Consultar disponibilidad de un área (Quincho):
```bash
curl "http://localhost:7000/api/amenities/3/availability?date=2025-11-15" \
  -H "Authorization: Bearer $TOKEN"
```

Reservar un bloque horario:
```bash
curl -X POST http://localhost:7000/api/amenities/3/reserve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2025-11-15",
    "startTime": "18:00",
    "endTime": "22:00",
    "guestCount": 10
  }'
```

## Ejemplos de curl para registro de comunidades (Admin)

> Este endpoint es público y permite a un administrador postular su comunidad. Requiere envío de archivo (documento de respaldo).

```bash
curl -X POST http://localhost:7000/api/buildings/requests \
  -F "name=Edificio Plaza Central" \
  -F "address=Av. Libertad 123" \
  -F "commune=Santiago" \
  -F "city=Metropolitana" \
  -F "adminName=Pedro Administrador" \
  -F "adminEmail=pedro.admin@example.com" \
  -F "adminPhone=+56911112222" \
  -F "proofText=Adjunto acta de asamblea de nombramiento" \
  -F "document=@/ruta/al/archivo/acta.pdf"
```

## Ejemplos de curl para contactos frecuentes (Visitas)

Gestión de "agenda" de visitantes para registros rápidos.

Crear contacto:
```bash
curl -X POST http://localhost:7000/api/visit-contacts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "visitorName": "Juan Amigo",
    "visitorDocument": "11.222.333-4",
    "visitorType": "VISIT",
    "notes": "Amigo cercano"
  }'
```

Registrar visita desde contacto existente:
```bash
curl -X POST http://localhost:7000/api/visit-contacts/5/register \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "validForMinutes": 180 }'
```

## Gestión de Perfil y Seguridad

Actualizar datos personales:
```bash
curl -X PUT http://localhost:7000/api/users/me/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Juan",
    "lastName": "Pérez Soto",
    "phone": "+56988887777",
    "documentNumber": "12.345.678-9"
  }'
```

Cambiar contraseña:
```bash
curl -X POST http://localhost:7000/api/users/me/password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "ClaveAntigua123!",
    "newPassword": "NuevaClaveSuperSegura2025!"
  }'
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
