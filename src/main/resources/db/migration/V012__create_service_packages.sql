CREATE TABLE SERVICE_PACKAGE (
    id UUID NOT NULL,
    clinic_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    service_id UUID NOT NULL,
    created_by UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NULL,
    total_sessions INT NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_service_package PRIMARY KEY (id),
    CONSTRAINT uk_service_package_clinic_id UNIQUE (clinic_id, id),
    CONSTRAINT fk_service_package_clinic FOREIGN KEY (clinic_id) REFERENCES CLINIC (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_service_package_unit_tenant FOREIGN KEY (clinic_id, unit_id) REFERENCES CLINIC_UNIT (clinic_id, id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_service_package_service_tenant FOREIGN KEY (clinic_id, service_id) REFERENCES SERVICE_OFFERING (clinic_id, id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_service_package_created_by_tenant FOREIGN KEY (clinic_id, created_by) REFERENCES USERS (clinic_id, id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_service_package_sessions CHECK (total_sessions > 0),
    CONSTRAINT chk_service_package_price CHECK (total_price >= 0)
);

CREATE INDEX idx_service_package_clinic_unit ON SERVICE_PACKAGE (clinic_id, unit_id);
CREATE INDEX idx_service_package_clinic_service ON SERVICE_PACKAGE (clinic_id, service_id);
CREATE INDEX idx_service_package_clinic_active ON SERVICE_PACKAGE (clinic_id, active);

CREATE TABLE SERVICE_PACKAGE_WEEK_DAY (
    service_package_id UUID NOT NULL,
    week_day VARCHAR(16) NOT NULL,
    CONSTRAINT pk_service_package_week_day PRIMARY KEY (service_package_id, week_day),
    CONSTRAINT fk_service_package_week_day_package FOREIGN KEY (service_package_id) REFERENCES SERVICE_PACKAGE (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_service_package_week_day CHECK (week_day IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);
