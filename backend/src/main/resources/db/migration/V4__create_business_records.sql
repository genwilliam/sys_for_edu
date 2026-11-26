CREATE TABLE business_records (
    domain_name VARCHAR(40) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (domain_name, record_id)
);

CREATE INDEX idx_business_records_domain ON business_records (domain_name);
