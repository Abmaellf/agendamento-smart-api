package com.agendamento.smart.controller.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public DomainException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static DomainException tenantResourceNotFound() {
        return new DomainException(
                "TENANT_RESOURCE_NOT_FOUND",
                "Cadastro não encontrado.",
                HttpStatus.NOT_FOUND);
    }
}
