CREATE TABLE SERVICE_OFFERING (
    id BINARY(16) NOT NULL,
    clinic_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    capacity INT NOT NULL,

    CONSTRAINT pk_service_offering PRIMARY KEY (id),
    CONSTRAINT uk_service_offering_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT uk_service_offering_clinic_name UNIQUE (clinic_id, name),
    CONSTRAINT fk_service_offering_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT chk_service_offering_duration
        CHECK (duration_minutes BETWEEN 1 AND 480),
    CONSTRAINT chk_service_offering_price
        CHECK (price >= 0),
    CONSTRAINT chk_service_offering_capacity
        CHECK (capacity >= 1)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

