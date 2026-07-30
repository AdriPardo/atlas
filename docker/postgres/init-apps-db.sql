-- Dedicated customer-data database (ADR-0015). Control plane stays on "atlas".
-- Runs only on first Postgres volume init.
CREATE DATABASE apps;
