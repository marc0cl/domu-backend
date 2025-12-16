# Plantillas de correo para solicitudes de comunidad

## Qué se envía
- **Correo al aprobador**: `building-request-approval.html` (incluye el adjunto del archivo de registro). Se genera en `BuildingService.sendApprovalPreview` y se envía de forma asíncrona.
- **Correo de confirmación al solicitante**: `building-request-user-confirmation.html`. Se renderiza en `sendUserConfirmation` y se envía de forma asíncrona para no bloquear la creación de la solicitud.
- **Correo de aprobación al solicitante**: `building-request-approved.html`. Se envía desde `sendApprovedNotification` cuando la solicitud pasa a estado aprobado (tanto por panel como por enlace de correo).
- **Correo de rechazo al solicitante**: `building-request-rejected.html`. Se envía desde `sendRejectedNotification` al registrar un rechazo con motivo.
- El correo de aprobación incluye un botón para crear la cuenta de administrador mediante un enlace seguro (`/registrar-admin?code=...`).

## Ubicación de plantillas
- `src/main/resources/templates/building-request-approval.html`
- `src/main/resources/templates/building-request-user-confirmation.html`
- `src/main/resources/templates/building-request-approved.html`
- `src/main/resources/templates/building-request-rejected.html`

## Identidad visual (paleta y fuente)
- Fuente: [Roboto Mono](https://fonts.google.com/specimen/Roboto+Mono) cargada vía Google Fonts en ambas plantillas.
- Colores de marca usados en las plantillas:
  - Negro: `#000000`
  - Naranja: `#f16b32`
  - Amarillo: `#f7ce0f`
  - Gris oscuro: `#67696a`
  - Gris claro: `#808282`
  - Turquesa: `#53a497`
- Estados: recepción en naranjo, aprobación en verde, rechazo en rojo.

## Logo
- Por ahora no se incluye el logo en los correos. El archivo `LogotipoDOMU.svg` queda disponible en `templates/` por si se necesita reusarlo en el futuro.

## Estilo de componentes
- Botones modernizados con bordes redondeados, gradiente turquesa en primario y borde claro en secundario; mayor separación entre CTA.
- Tarjetas con bordes suaves, sombra ligera y bloques de información con jerarquía (labels en mayúsculas y valores destacados).

