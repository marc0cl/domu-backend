# ARQUITECTURA BACKEND - DOMU

## 1. Objetivo y alcance
Este documento describe la estructura de `domu-backend` desde una perspectiva de ingenieria de software:
- Que contiene cada carpeta principal.
- Que responsabilidad tiene cada archivo relevante.
- Que artefactos son fuente y cuales son generados.

No cubre la arquitectura de `domu-frontend` ni `domu-mobile`.

## 2. Stack tecnico
- Lenguaje y runtime: Java 21.
- Framework web: Javalin 6.
- Inyeccion de dependencias: Google Guice.
- Persistencia: JDBC + HikariCP + MySQL.
- Seguridad: JWT (java-jwt) + BCrypt.
- Build: Gradle Kotlin DSL.
- Storage de archivos: Google Cloud Storage (con soporte legado Box en datos historicos).
- Testing: JUnit 5 + Mockito + AssertJ.

## 3. Vista de alto nivel
Flujo principal de capas:
1. `web/` recibe HTTP/WS, valida entrada y extrae contexto de usuario.
2. `service/` aplica reglas de negocio y autorizacion por rol/edificio.
3. `database/` ejecuta SQL y mapea resultados.
4. `dto/` define contratos de entrada/salida de API.
5. `domain/` representa entidades de negocio (records y modelos legacy).

## 4. Estructura de la raiz (`domu-backend/`)

### 4.1 Carpetas
- `.git/`: metadatos de control de versiones.
- `.gradle/`: cache y metadatos de Gradle (generado).
- `.vscode/`: configuracion local del IDE.
- `bin/`: clases compiladas y recursos copiados (generado).
- `build/`: artefactos del build Gradle, reportes y clases (generado).
- `gradle/`: version catalog y wrapper de Gradle.
- `src/`: codigo fuente y pruebas.
- `target/`: artefactos de Maven/compilacion antigua (generado/legacy).

### 4.2 Archivos de raiz
- `.env`: variables de entorno locales (secretos/config runtime).
- `.gitattributes`: politicas de fin de linea por tipo de archivo.
- `.gitignore`: exclusion de secretos y artefactos generados.
- `build.gradle.kts`: definicion oficial del build y dependencias.
- `gradle.properties`: propiedades globales de Gradle (config cache).
- `gradlew` / `gradlew.bat`: wrapper para ejecutar Gradle sin instalacion global.
- `settings.gradle.kts`: nombre de proyecto y plugin resolver de toolchains.
- `README.md`: guia operativa de instalacion/ejecucion/endpoints.
- `TESTING.md`: guia de pruebas locales.
- `EMAIL_TEMPLATES.md`: convenciones de templates y contenido de correos.
- `run` / `run.sh`: script bash para cargar `.env` y ejecutar backend.
- `pom.xml`: archivo Maven legado (obsoleto en este proyecto).

## 5. Carpeta `gradle/`
- `libs.versions.toml`: catalogo central de versiones y coordenadas de librerias.
- `wrapper/gradle-wrapper.properties`: version fija de Gradle (8.14.3).
- `wrapper/gradle-wrapper.jar`: bootstrap del wrapper.

## 6. Carpeta `src/main/java/com/domu/`

### 6.1 Archivo raiz del paquete
- `Main.java`: entrypoint; resuelve puerto, obtiene `WebServer` desde Guice y levanta la aplicacion.

### 6.2 `config/`
- `AppConfig.java`: record de configuracion (DB, JWT, mail, storage, server) y `CustomJsonMapper` de Javalin/Jackson.
- `DependencyInjectionModule.java`: composicion DI (bindings de repos/services), lectura de `application.properties` + `.env`, creacion de `DataSource`, `JwtProvider`, `EmailService`, y ejecucion de migraciones SQL de compatibilidad en startup.

### 6.3 `web/`
- `WebServer.java`: configura Javalin, handlers globales, rutas REST/HTML, WebSocket, parseo/validacion de requests y conversion a respuestas.
- `ChatWebSocketHandler.java`: canal WS de chat (`/ws/chat`), autenticacion por token, presencia online/offline y entrega de mensajes a participantes.
- `UserMapper.java`: transforma `User` de dominio a `UserResponse`, incluyendo resolucion de URLs firmadas GCS.

### 6.4 `security/`
- `AuthenticationHandler.java`: middleware de autenticacion Bearer JWT para rutas protegidas.
- `JwtProvider.java`: emite y valida JWT (issuer, subject, expiracion y claims).
- `PasswordHasher.java`: contrato de hash de password.
- `BCryptPasswordHasher.java`: implementacion BCrypt (salt aleatorio + verificacion).

### 6.5 `email/`
- `EmailService.java`: interfaz para envio de correos HTML (con y sin adjunto).
- `SmtpEmailService.java`: implementacion SMTP via Jakarta Mail.
- `ApprovalEmailTemplate.java`: renderer de plantilla de aprobacion de solicitudes.
- `BuildingApprovedEmailTemplate.java`: renderer de plantilla de solicitud aprobada.
- `BuildingRejectedEmailTemplate.java`: renderer de plantilla de solicitud rechazada.
- `UserConfirmationEmailTemplate.java`: renderer de plantilla de confirmacion de usuario.

### 6.6 `database/` (persistencia JDBC)
Responsabilidad general: encapsular SQL y transacciones por modulo.

- `DataSourceFactory.java`: construccion de pool Hikari.
- `RepositoryException.java`: excepcion base de repositorios.
- `UserRepository.java`: usuarios, perfil, password, avatar, vecinos para chat.
- `UserBuildingRepository.java`: relacion usuario-edificio y validacion de acceso.
- `UserConfirmationRepository.java`: tokens de confirmacion de cuenta.
- `BuildingRepository.java`: flujo de solicitudes de comunidad y aprobaciones.
- `HousingUnitRepository.java`: CRUD de unidades y residentes por unidad.
- `CommonExpenseRepository.java`: periodos GGCC, cargos, pagos, boletas y revisiones.
- `VisitRepository.java`: autorizaciones de visitas, check-in y logs de acceso.
- `VisitContactRepository.java`: agenda de contactos frecuentes para visitas.
- `IncidentRepository.java`: incidentes, estado y asignaciones.
- `PollRepository.java`: votaciones, opciones, votos y cierre.
- `AmenityRepository.java`: amenities, bloques horarios y reservas.
- `MarketRepository.java`: marketplace, imagenes y acceso por edificio.
- `ChatRepository.java`: salas, participantes, mensajes y ocultamiento de sala.
- `ChatRequestRepository.java`: solicitudes de chat y cambios de estado.
- `ParcelRepository.java`: paqueteria por unidad/edificio.
- `ForumRepository.java`: foros, hilos y publicaciones.
- `TaskRepository.java`: tareas y asignaciones a staff.
- `StaffRepository.java`: personal del edificio.
- `LibraryRepository.java`: documentos de biblioteca por edificio.

### 6.7 `service/` (logica de negocio)
Responsabilidad general: casos de uso, validaciones y autorizaciones.

- `UserService.java`: registro/login, administracion de usuarios, perfil, password y confirmacion.
- `BuildingService.java`: solicitudes de comunidad, aprobacion/rechazo e invitacion de administrador.
- `CommonExpenseService.java`: ciclo GGCC (periodos, cargos, pagos, boletas).
- `CommonExpenseReceiptStorageService.java`: upload/download de boletas GGCC.
- `CommonExpensePdfService.java`: generacion de PDF de gastos comunes.
- `PaymentReceiptPdfService.java`: comprobante PDF de pago.
- `ChargeReceiptPdfService.java`: comprobante PDF de cargo.
- `HousingUnitService.java`: gestion de unidades y vinculacion de residentes.
- `VisitService.java`: visitas, QR y check-in.
- `VisitContactService.java`: contactos frecuentes y registro rapido.
- `IncidentService.java`: incidentes y cambios de estado/asignacion.
- `PollService.java`: creacion/listado/voto/cierre/export de votaciones.
- `AmenityService.java`: alta de amenities, disponibilidad y reservas.
- `MarketService.java`: publicaciones de marketplace e imagenes.
- `ChatService.java`: conversaciones y mensajes.
- `ChatRequestService.java`: flujo de solicitud/aprobacion de chat.
- `ForumService.java`: operacion de foros comunitarios.
- `ParcelService.java`: gestion de paqueteria.
- `TaskService.java`: gestion de tareas.
- `StaffService.java`: gestion de personal.
- `LibraryService.java`: gestion de documentos compartidos.
- `GcsStorageService.java`: operaciones base en GCS (upload/download/delete/signedUrl).
- `MarketplaceStorageService.java`: convenciones de paths para media de marketplace/chat/perfil.
- `CommunityRegistrationStorageService.java`: almacenamiento de documentos de solicitud comunitaria.
- `ImageOptimizer.java`: optimizacion y validacion de archivos multimedia.
- `ValidationException.java`: error de validacion de negocio.
- `InvalidCredentialsException.java`: error de autenticacion.
- `UserAlreadyExistsException.java`: error de duplicidad de usuario.

### 6.8 `domain/`
Responsabilidad general: modelo de dominio.

- Subpaquetes activos por bounded context: `core`, `finance`, `access`, `community`, `facility`, `staff`, `ticket`, `vendor`, `voting`.
- Predominan `record` inmutables para transferencia interna.
- Existen clases legacy en la raiz de `domain/` (POJOs mutables) por compatibilidad historica.
- Modelos actualmente usados de forma directa en capas activas: `domain.core.*`, `domain.finance.*`, `domain.BuildingRequest`, `domain.LibraryDocument`.

### 6.9 `dto/`
Responsabilidad general: contratos de API y serializacion.

- `*Request`: entrada de endpoints (validacion de payload).
- `*Response`: salida estandarizada por modulo.
- DTOs auxiliares sin sufijo request/response (ej.: `ForumThreadDto`, `BoxUploadResult`) para transporte interno entre capas web/service/repository.

## 7. Carpeta `src/main/resources/`

### 7.1 Configuracion y logging
- `application.properties`: propiedades base con placeholders a variables de entorno.
- `log4j2.xml`: configuracion principal de logging.
- `simplelogger.properties`: configuracion alternativa de logging simple.

### 7.2 Plantillas de correo (`templates/`)
- `building-request-approval.html`: correo al aprobador.
- `building-request-approved.html`: notificacion de solicitud aprobada.
- `building-request-rejected.html`: notificacion de solicitud rechazada.
- `building-request-user-confirmation.html`: confirmacion de recepcion de solicitud.
- `LogotipoDOMU.svg`: asset visual para correos.

### 7.3 Migraciones SQL (`migrations/`)
Secuencia funcional:
- `001`-`002`: base de autenticacion y modelo inicial.
- `003`: gastos comunes.
- `004`: solicitudes de edificio/comunidad.
- `005`-`007`: visitas, incidentes y contactos.
- `008`: usuario con multiples edificios.
- `009`-`012`: metadatos de solicitud y enlaces de aprobacion/invitacion.
- `013`-`014`: votaciones y amenities.
- `015`-`017`: auditoria de unidades, mejoras GGCC e incidentes.
- `018`: confirmacion de usuarios.
- `019`-`022`: marketplace, chat y privacidad de perfil.
- `023`-`024`: foro y tareas.
- `026`-`027`: staff y asignacion de tareas.
- `028`-`030`: migracion a GCS, ocultamiento de chats y biblioteca documental.

Notas:
- `migrations/rollback/` existe pero esta vacia.
- `db/migration/V1__create_usuario_table.sql` es un script legacy (esquema `usuario`), no es el flujo principal actual.

## 8. Carpeta `src/test/java/com/domu/`
- `security/BCryptPasswordHasherTest.java`: valida hashing y verificacion de contrasenas.
- `service/PollServiceTest.java`: valida reglas de negocio de votaciones (roles/opciones).

## 9. Artefactos generados vs fuente
- Fuente mantenida: `src/`, `gradle/`, archivos `.kts`, scripts, docs y configs.
- Generado/no versionable funcionalmente: `.gradle/`, `build/`, `bin/`, `target/`.

## 10. Observaciones de arquitectura
- El backend sigue una arquitectura por capas clara (`web -> service -> repository`).
- `WebServer.java` concentra gran cantidad de rutas; candidato natural para modularizar en controllers por dominio.
- Coexisten modelos de dominio nuevos (`domain/*` en records) y legacy (`domain/` raiz), lo que sugiere deuda tecnica de consolidacion.
- `pom.xml` convive con Gradle, pero el build real es Gradle; conviene mantener una sola via oficial para evitar drift.
