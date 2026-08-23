CREATE TABLE PROFESSIONAL (
    id BINARY(16) NOT NULL,
    clinic_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    available_week_days VARCHAR(30) NOT NULL,

    CONSTRAINT pk_professional PRIMARY KEY (id),
    CONSTRAINT uk_professional_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT fk_professional_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT chk_professional_available_week_days
        CHECK (
            REGEXP_LIKE(
                available_week_days,
                '^[[:space:]]*[1-7]([[:space:]]*,[[:space:]]*[1-7])*[[:space:]]*$'
            )
        ),

    INDEX idx_professional_clinic_name (clinic_id, name)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
