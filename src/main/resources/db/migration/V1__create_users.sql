CREATE TABLE app_users (
    user_id UUID PRIMARY KEY  ,
    email varchar(255) NOT NULL ,
    password_hash varchar(255) NOT NULL,
    role varchar(20) NOT NULL CONSTRAINT chk_app_users_role CHECK ( role IN ('ADMIN', 'RESPONDER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_app_users_email_lower
    ON app_users (LOWER(email));