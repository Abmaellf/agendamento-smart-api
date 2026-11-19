package com.agendamento.smart.controller.appointment;

import com.agendamento.smart.controller.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AppointmentNoShowDeadlineException extends DomainException {

    public AppointmentNoShowDeadlineException() {
        super(
                "APPOINTMENT_NO_SHOW_DEADLINE",
                "O prazo para iniciar ou cancelar o agendamento expirou; o status foi alterado para falta.",
                HttpStatus.CONFLICT);
    }
}
