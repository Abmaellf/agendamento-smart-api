CREATE TABLE PROFESSIONAL_SERVICE (
    professional_id BINARY(16) NOT NULL,
    service_id BINARY(16) NOT NULL,

    CONSTRAINT pk_professional_service
        PRIMARY KEY (professional_id, service_id),
    CONSTRAINT fk_professional_service_professional
        FOREIGN KEY (professional_id)
        REFERENCES PROFESSIONAL (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_professional_service_service
        FOREIGN KEY (service_id)
        REFERENCES SERVICE_OFFERING (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    INDEX idx_professional_service_service (service_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

