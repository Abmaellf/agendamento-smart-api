CREATE TABLE USERS (
    id UUID NOT NULL,
    login VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    clinic_id UUID NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER')),
    CONSTRAINT fk_users_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

-- O MySQL anterior usava collation case-insensitive para login.
CREATE UNIQUE INDEX uk_users_login ON USERS (LOWER(login));
CREATE INDEX idx_users_clinic ON USERS (clinic_id);
