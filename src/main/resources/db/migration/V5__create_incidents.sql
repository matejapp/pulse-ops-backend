CREATE TABLE incident(
    incident_id UUID PRIMARY KEY ,
    service_id UUID NOT NULL ,
    title varchar(200) NOT NULL ,
    current_status varchar(20) NOT NULL DEFAULT 'INVESTIGATING'
                     CONSTRAINT chk_current_status_status CHECK ( current_status IN ('INVESTIGATING', 'MONITORING', 'RESOLVED') ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,

    CONSTRAINT fk_incident_service
                     FOREIGN KEY (service_id) REFERENCES monitored_service(service_id)
                     ON DELETE CASCADE
);

CREATE TABLE incident_update(
    incident_update_id UUID PRIMARY KEY ,
    incident_id UUID NOT NULL ,
    status varchar(20) NOT NULL,
        CONSTRAINT chk_status_status CHECK ( status IN ('INVESTIGATING', 'MONITORING', 'RESOLVED') ),
    message TEXT NOT NULL ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_incident_update_incident
                            FOREIGN KEY (incident_id) REFERENCES incident(incident_id)
                            ON DELETE CASCADE
);


CREATE INDEX idx_incident_update_incident_id_created_at ON incident_update(incident_id,created_at);