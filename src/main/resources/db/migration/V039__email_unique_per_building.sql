-- Multi-tenant: permitir mismo email en diferentes comunidades.
-- La unicidad de email se valida por edificio en la aplicación.
ALTER TABLE users DROP INDEX email;
