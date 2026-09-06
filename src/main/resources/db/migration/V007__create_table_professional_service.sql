CREATE TABLE PROFESSIONAL_SERVICE (
    professional_id UUID NOT NULL,
    service_id UUID NOT NULL,

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
        ON DELETE RESTRICT
);

CREATE INDEX idx_professional_service_service
    ON PROFESSIONAL_SERVICE (service_id);
