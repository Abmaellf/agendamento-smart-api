CREATE TABLE PROFESSIONAL (
    id UUID NOT NULL,
    clinic_id UUID NOT NULL,
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
            available_week_days ~
                '^[[:space:]]*[1-7]([[:space:]]*,[[:space:]]*[1-7])*[[:space:]]*$'
        )
);

CREATE INDEX idx_professional_clinic_name ON PROFESSIONAL (clinic_id, name);
