package com.agendamento.smart.controller.exception;

import java.util.List;
import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        Map<String, List<String>> fieldErrors
) {
}
