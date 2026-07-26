ALTER TABLE hosts
    ADD COLUMN connection_type VARCHAR(16) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN ssh_user VARCHAR(128),
    ADD COLUMN ssh_port INT NOT NULL DEFAULT 22,
    ADD COLUMN ssh_private_key_secret_id UUID;

ALTER TABLE hosts
    ADD CONSTRAINT chk_hosts_connection_type CHECK (connection_type IN ('LOCAL', 'SSH')),
    ADD CONSTRAINT chk_hosts_ssh_port CHECK (ssh_port > 0 AND ssh_port <= 65535),
    ADD CONSTRAINT fk_hosts_ssh_secret
        FOREIGN KEY (ssh_private_key_secret_id) REFERENCES secrets (id) ON DELETE SET NULL;

CREATE INDEX idx_hosts_connection_type ON hosts (connection_type);
