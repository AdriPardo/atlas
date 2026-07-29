CREATE TABLE usage_records (
    id              UUID PRIMARY KEY,
    meter           VARCHAR(64)  NOT NULL,
    quantity        NUMERIC(20, 4) NOT NULL,
    period_start    TIMESTAMPTZ  NOT NULL,
    period_end      TIMESTAMPTZ  NOT NULL,
    dimensions      TEXT         NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_usage_records_period ON usage_records (period_start DESC, period_end DESC);
CREATE INDEX idx_usage_records_meter_period ON usage_records (meter, period_start DESC);
CREATE INDEX idx_usage_records_created_at ON usage_records (created_at DESC);
