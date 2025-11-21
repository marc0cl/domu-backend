# Pruebas locales

Se verificó la conexión a MySQL y el flujo de autenticación local.

## Preparación
1. Se instaló y levantó MySQL 8.0 localmente.
2. Base de datos y usuario creados:
   ```sql
   CREATE DATABASE domu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'domu'@'%' IDENTIFIED BY 'domu';
   GRANT ALL PRIVILEGES ON domu.* TO 'domu'@'%';
   FLUSH PRIVILEGES;
   ```
3. Migraciones aplicadas:
   ```bash
   mysql -udomu -pdomu domu < src/main/resources/migrations/001_create_auth_schema.sql
   mysql -udomu -pdomu domu < src/main/resources/migrations/002_update_auth_schema.sql
   ```

## Ejecución del servidor
Servidor iniciado con conexión local a MySQL:
```bash
DB_URI="jdbc:mysql://127.0.0.1:3306/domu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" DB_USER=domu DB_PASSWORD=domu ./gradlew run
```

## Casos probados
- **Registro** de usuaria nueva:
  ```bash
  curl -s -X POST http://localhost:7000/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"unitId": null, "roleId": null, "firstName": "Maria", "lastName": "Lopez", "birthDate": "1992-05-10", "email": "maria.lopez@example.com", "phone": "+56 9 1111 2222", "documentNumber": "98.765.432-1", "resident": false, "password": "ClaveSegura456!"}'
  ```
  Respuesta `201` con los datos creados.

- **Login** con la usuaria registrada:
  ```bash
  curl -s -X POST http://localhost:7000/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email": "maria.lopez@example.com", "password": "ClaveSegura456!"}'
  ```
  Respuesta `200` con token JWT y payload del usuario.
