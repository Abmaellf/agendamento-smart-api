CREATE TABLE CLINIC_UNIT (
    id UUID NOT NULL,
    clinic_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    time_zone VARCHAR(80) NOT NULL,

    CONSTRAINT pk_clinic_unit PRIMARY KEY (id),
    CONSTRAINT uk_clinic_unit_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT uk_clinic_unit_clinic_name UNIQUE (clinic_id, name),
    CONSTRAINT fk_clinic_unit_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES CLINIC (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
);

CREATE INDEX idx_clinic_unit_tenant ON CLINIC_UNIT (tenant_id);
