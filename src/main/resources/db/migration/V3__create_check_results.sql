    CREATE TABLE check_result(
        check_result_id UUID PRIMARY KEY,
        monitor_id UUID NOT NULL,
        success boolean NOT NULL,
        response_status int ,
        latency_ms int NOT NULL,
        error_message TEXT ,
        checked_at TIMESTAMPTZ NOT NULL,

        CONSTRAINT fk_check_result_monitor
            FOREIGN KEY (monitor_id) REFERENCES http_monitor (monitor_id)
                ON DELETE CASCADE
    );

    CREATE INDEX idx_monitor_id_checked_at ON check_result(monitor_id,checked_at);