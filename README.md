# Domu Backend

Base del backend del proyecto Domu organizada siguiendo principios de arquitectura
limpia y una distribución moderna de carpetas.

## Requisitos

- Python 3.11+
- Gestor de dependencias compatible con `pyproject.toml` (``pip`` estándar funciona).

## Instalación

```bash
pip install -e .[dev]
```

## Ejecución

```bash
uvicorn app.main:app --reload
```

## Tests

```bash
pytest
```

## Estructura de carpetas

```
main/
├── src/
│   └── app/
│       ├── api/
│       │   └── routes/
│       ├── core/
│       ├── domain/
│       │   └── models/
│       ├── infrastructure/
│       └── services/
└── tests/
```

- **api**: routers y definiciones HTTP.
- **core**: configuración y componentes transversales.
- **domain**: entidades y modelos del negocio.
- **services**: casos de uso y lógica de aplicación.
- **infrastructure**: adaptadores a tecnologías externas (logging, bases de datos, etc.).
- **tests**: pruebas automatizadas.

## Variables de entorno

Configura un archivo `.env` en la raíz para sobreescribir valores por defecto:

```
APP_NAME=Domu Backend
ENVIRONMENT=production
ENABLE_DOCS=false
```
