CREATE TABLE IF NOT EXISTS USERS (
    id BINARY(16) NOT NULL,
    login VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    clinic_id BINARY(16) NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT fk_users_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
);

-- Evita usuários com login duplicado
CREATE UNIQUE INDEX uk_users_login ON USERS (login);
