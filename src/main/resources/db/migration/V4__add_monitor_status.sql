ALTER TABLE http_monitor
ADD status VARCHAR(20) NOT NULL DEFAULT 'UNCHECKED'
    CONSTRAINT chk_http_monitor_status CHECK ( status IN ('OPERATIONAL', 'DEGRADED', 'UNCHECKED') ),
ADD consecutive_failures INT NOT NULL DEFAULT 0;

