CREATE TABLE monitored_service(
    service_id UUID PRIMARY KEY,
    name varchar(100) NOT NULL,
    description varchar(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_monitored_services_name ON monitored_service(lower(name));

CREATE TABLE http_monitor(
    monitor_id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    target_url varchar(2048) NOT NULL,
    http_method varchar(10) NOT NULL CHECK ( http_method in ('GET', 'HEAD') ),
    expected_status INT NOT NULL,
    enabled  BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_http_monitor_service
        FOREIGN KEY (service_id) REFERENCES monitored_service (service_id)
    ON DELETE CASCADE
);

CREATE INDEX idx_http_monitor_service_id ON http_monitor(service_id);