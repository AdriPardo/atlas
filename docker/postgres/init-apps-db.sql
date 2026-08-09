-- Dedicated customer-data database (ADR-0015). Control plane stays on "atlas".
-- Runs only on first Postgres volume init.
CREATE DATABASE apps;

-- Provisioner needs CREATEROLE to create per-project roles. On the official image
-- POSTGRES_USER is already superuser, so this is a no-op; on shared/prod Postgres
-- (non-superuser app role) ops must run the equivalent as a superuser once:
--   ALTER ROLE <ATLAS_APP_DB_USERNAME> WITH CREATEROLE;
-- Do not point ATLAS_APP_DB_URL at database "atlas".
