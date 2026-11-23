CREATE TABLE AUTH_SESSION (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT pk_auth_session PRIMARY KEY (id),
    CONSTRAINT fk_auth_session_user
        FOREIGN KEY (user_id)
        REFERENCES USERS (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE
);

CREATE INDEX idx_auth_session_user ON AUTH_SESSION (user_id);
CREATE INDEX idx_auth_session_active ON AUTH_SESSION (id, revoked_at, expires_at);

CREATE TABLE AUTH_REFRESH_TOKEN (
    id UUID NOT NULL,
    session_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT pk_auth_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_auth_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_refresh_token_session
        FOREIGN KEY (session_id)
        REFERENCES AUTH_SESSION (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE
);

CREATE INDEX idx_auth_refresh_token_session ON AUTH_REFRESH_TOKEN (session_id);
CREATE INDEX idx_auth_refresh_token_lookup ON AUTH_REFRESH_TOKEN (token_hash, expires_at);
