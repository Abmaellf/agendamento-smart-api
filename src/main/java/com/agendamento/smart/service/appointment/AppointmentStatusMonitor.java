package com.agendamento.smart.service.appointment;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "appointments.status-monitor.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AppointmentStatusMonitor {

    private static final Logger log = LoggerFactory.getLogger(AppointmentStatusMonitor.class);

    private final AppointmentService appointmentService;
    private final Clock clock;

    @Scheduled(
            fixedDelayString = "${appointments.status-monitor.fixed-delay-ms:60000}",
            initialDelayString = "${appointments.status-monitor.initial-delay-ms:10000}")
    public void reconcileOverdueAppointments() {
        String runId = UUID.randomUUID().toString();
        Instant startedAt = clock.instant();
        long startedNanos = System.nanoTime();

        try {
            long candidates = appointmentService.countOverdueAppointments(startedAt);
            int updated = appointmentService.reconcileOverdueAppointments(startedAt);
            log.info(
                    "appointment_status_monitor runId={} startedAt={} durationMs={} candidates={} updated={}",
                    runId,
                    startedAt,
                    (System.nanoTime() - startedNanos) / 1_000_000,
                    candidates,
                    updated);
        } catch (Exception exception) {
            log.error(
                    "appointment_status_monitor runId={} startedAt={} durationMs={} errorType={}",
                    runId,
                    startedAt,
                    (System.nanoTime() - startedNanos) / 1_000_000,
                    exception.getClass().getSimpleName());
        }
    }
}
