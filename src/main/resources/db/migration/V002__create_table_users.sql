CREATE TABLE USERS (
    id BINARY(16) NOT NULL,
    login VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    clinic_id BINARY(16) NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_login UNIQUE (login),
    CONSTRAINT uk_users_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT fk_users_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    INDEX idx_users_clinic (clinic_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

