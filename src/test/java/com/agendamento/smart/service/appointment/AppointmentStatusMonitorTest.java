package com.agendamento.smart.service.appointment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AppointmentStatusMonitorTest {

    private static final Instant NOW = Instant.parse("2026-11-19T14:00:00Z");

    @Mock
    private AppointmentService appointmentService;

    @Test
    void logsSuccessfulRunWithOperationalCounters(CapturedOutput output) {
        when(appointmentService.countOverdueAppointments(NOW)).thenReturn(3L);
        when(appointmentService.reconcileOverdueAppointments(NOW)).thenReturn(3);

        monitor().reconcileOverdueAppointments();

        assertThat(output.getOut())
                .contains("appointment_status_monitor")
                .contains("candidates=3")
                .contains("updated=3")
                .doesNotContain("appointmentId");
    }

    @Test
    void logsFailureTypeAndAllowsNextRun(CapturedOutput output) {
        when(appointmentService.countOverdueAppointments(NOW))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(0L);
        when(appointmentService.reconcileOverdueAppointments(NOW)).thenReturn(0);

        monitor().reconcileOverdueAppointments();
        monitor().reconcileOverdueAppointments();

        assertThat(output.getOut())
                .contains("errorType=IllegalStateException")
                .contains("candidates=0")
                .contains("updated=0");
    }

    private AppointmentStatusMonitor monitor() {
        return new AppointmentStatusMonitor(
                appointmentService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
