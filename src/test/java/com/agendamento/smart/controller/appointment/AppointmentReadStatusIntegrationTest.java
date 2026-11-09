package com.agendamento.smart.controller.appointment;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.AuthenticationDTO;
import com.agendamento.smart.dtos.appointment.AppointmentRequest;
import com.agendamento.smart.dtos.appointment.AppointmentResponse;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.clinic.StatusClinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.ClinicOnboardingService;
import com.agendamento.smart.service.appointment.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentReadStatusIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String ADMIN_PASSWORD = "Administrative@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicOnboardingService onboardingService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @ParameterizedTest
    @EnumSource(StatusClinic.class)
    void permitsLoginForEveryClinicStatus(StatusClinic clinicStatus) throws Exception {
        Context context = context("login-clinic-" + clinicStatus.name().toLowerCase());
        context.clinic().setStatus(clinicStatus);
        clinicRepository.saveAndFlush(context.clinic());

        performLogin(context)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @ParameterizedTest
    @EnumSource(StatusUnit.class)
    void permitsLoginForEveryUnitStatus(StatusUnit unitStatus) throws Exception {
        Context context = context("login-unit-" + unitStatus.name().toLowerCase());
        context.matrix().setStatus(unitStatus);
        unitRepository.saveAndFlush(context.matrix());

        performLogin(context)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void permitsHistoricalReadByIdAndUnitFilterWhenClinicAndUnitAreBlocked() throws Exception {
        Context context = context("blocked-read");
        Instant startsAt = futureBase();
        AppointmentResponse appointment = create(context, context.matrix(), context.patient(), startsAt);
        context.clinic().setStatus(StatusClinic.BLOQUEADA);
        clinicRepository.saveAndFlush(context.clinic());
        context.matrix().setStatus(StatusUnit.BLOQUEADA);
        unitRepository.saveAndFlush(context.matrix());

        mockMvc.perform(get("/api/appointments/{id}", appointment.id())
                        .header("Authorization", bearer(context.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointment.id().toString()));

        performList(context, context.matrix().getId(), startsAt.minusSeconds(1), startsAt.plusSeconds(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(appointment.id().toString()));
    }

    @Test
    void rejectsReadByIdAndClinicListWhenClinicIsInactive() throws Exception {
        Context context = context("inactive-clinic-read");
        Instant startsAt = futureBase();
        AppointmentResponse appointment = create(context, context.matrix(), context.patient(), startsAt);
        context.clinic().setStatus(StatusClinic.INATIVO);
        clinicRepository.saveAndFlush(context.clinic());

        mockMvc.perform(get("/api/appointments/{id}", appointment.id())
                        .header("Authorization", bearer(context.admin())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLINIC_APPOINTMENTS_READ_DISABLED"));

        performList(context, null, startsAt.minusSeconds(1), startsAt.plusSeconds(1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLINIC_APPOINTMENTS_READ_DISABLED"));
    }

    @Test
    void rejectsReadByIdAndExplicitFilterWhenUnitIsInactive() throws Exception {
        Context context = context("inactive-unit-read");
        Instant startsAt = futureBase();
        AppointmentResponse appointment = create(context, context.matrix(), context.patient(), startsAt);
        context.matrix().setStatus(StatusUnit.INATIVO);
        unitRepository.saveAndFlush(context.matrix());

        mockMvc.perform(get("/api/appointments/{id}", appointment.id())
                        .header("Authorization", bearer(context.admin())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNIT_APPOINTMENTS_READ_DISABLED"));

        performList(context, context.matrix().getId(), startsAt.minusSeconds(1), startsAt.plusSeconds(1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNIT_APPOINTMENTS_READ_DISABLED"));
    }

    @Test
    void clinicListIncludesActiveAndBlockedUnitsAndExcludesInactiveAndEndBoundary() throws Exception {
        Context context = context("clinic-list");
        ClinicUnit blocked = filial(context, "Filial bloqueada");
        ClinicUnit inactive = filial(context, "Filial inativa");
        Patient blockedPatient = patient(context.clinic(), "Paciente bloqueada", blocked);
        Patient inactivePatient = patient(context.clinic(), "Paciente inativa", inactive);
        Patient endBoundaryPatient = patient(context.clinic(), "Paciente limite final", context.matrix());
        Instant from = futureBase();
        Instant to = from.plus(3, ChronoUnit.HOURS);

        AppointmentResponse activeAppointment = create(
                context, context.matrix(), context.patient(), from);
        AppointmentResponse blockedAppointment = create(
                context, blocked, blockedPatient, from.plus(1, ChronoUnit.HOURS));
        create(context, inactive, inactivePatient, from.plus(2, ChronoUnit.HOURS));
        create(context, context.matrix(), endBoundaryPatient, to);
        blocked.setStatus(StatusUnit.BLOQUEADA);
        unitRepository.saveAndFlush(blocked);
        inactive.setStatus(StatusUnit.INATIVO);
        unitRepository.saveAndFlush(inactive);

        performList(context, null, from, to)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(activeAppointment.id().toString()))
                .andExpect(jsonPath("$.content[1].id").value(blockedAppointment.id().toString()));

        performList(context, context.matrix().getId(), from, to)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(activeAppointment.id().toString()));
    }

    @Test
    void clinicListAndDetailRemainIsolatedFromAnotherClinic() throws Exception {
        Context first = context("tenant-first");
        Context second = context("tenant-second");
        Instant startsAt = futureBase();
        AppointmentResponse firstAppointment = create(first, first.matrix(), first.patient(), startsAt);
        AppointmentResponse secondAppointment = create(second, second.matrix(), second.patient(), startsAt);

        performList(first, null, startsAt.minusSeconds(1), startsAt.plusSeconds(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstAppointment.id().toString()));

        performList(first, second.matrix().getId(), startsAt.minusSeconds(1), startsAt.plusSeconds(1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/appointments/{id}", secondAppointment.id())
                        .header("Authorization", bearer(first.admin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_RESOURCE_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidOrMissingRangeAndReturnsEmptyContentForValidRange() throws Exception {
        Context context = context("invalid-range");
        Instant instant = futureBase();

        performList(context, null, instant, instant)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_APPOINTMENT_RANGE"));

        performList(context, null, instant.plusSeconds(60), instant)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_APPOINTMENT_RANGE"));

        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", bearer(context.admin()))
                        .param("to", instant.plusSeconds(60).toString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/appointments")
                        .header("Authorization", bearer(context.admin()))
                        .param("from", instant.toString()))
                .andExpect(status().isBadRequest());

        performList(context, null, instant, instant.plusSeconds(60))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(Context context) throws Exception {
        AuthenticationDTO request = new AuthenticationDTO(context.admin().getLogin(), ADMIN_PASSWORD);
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }

    private org.springframework.test.web.servlet.ResultActions performList(
            Context context,
            UUID unitId,
            Instant from,
            Instant to) throws Exception {
        var request = get("/api/appointments")
                .header("Authorization", bearer(context.admin()))
                .param("from", from.toString())
                .param("to", to.toString());
        if (unitId != null) request.param("unitId", unitId.toString());
        return mockMvc.perform(request);
    }

    private AppointmentResponse create(
            Context context,
            ClinicUnit unit,
            Patient patient,
            Instant startsAt) {
        return appointmentService.create(
                new AppointmentRequest(
                        patient.getId(),
                        unit.getId(),
                        context.service().getId(),
                        null,
                        startsAt,
                        context.service().getDurationMinutes(),
                        context.service().getPrice()),
                context.admin(),
                null);
    }

    private Context context(String suffix) {
        ClinicRegistrationResponse registration = onboardingService.register(
                new ClinicRegistrationRequest(
                        new ClinicRegistrationRequest.ClinicData("Clínica " + suffix),
                        new ClinicRegistrationRequest.MatrixUnitData("Matriz " + suffix, "America/Cuiaba"),
                        new ClinicRegistrationRequest.InitialProfessionalData(
                                "Atendente " + suffix,
                                "attendant-" + suffix + "@example.test",
                                "Attendant@123"),
                        new ClinicRegistrationRequest.InitialProfessionalData(
                                "Administrativo " + suffix,
                                "admin-" + suffix + "@example.test",
                                ADMIN_PASSWORD)));
        User admin = userRepository.findById(registration.administrative().userId()).orElseThrow();
        Clinic clinic = clinicRepository.findById(registration.clinicId()).orElseThrow();
        ClinicUnit matrix = unitRepository.findById(registration.unitId()).orElseThrow();
        Patient patient = patient(clinic, "Paciente " + suffix, matrix);

        ServiceOffering service = new ServiceOffering();
        service.setId(UUID.randomUUID());
        service.setClinic(clinic);
        service.setName("Serviço " + suffix);
        service.setDurationMinutes(30);
        service.setPrice(new BigDecimal("125.00"));
        service.setCapacity(10);
        service = serviceRepository.saveAndFlush(service);

        return new Context(clinic, matrix, patient, service, admin);
    }

    private Patient patient(Clinic clinic, String name, ClinicUnit unit) {
        Patient patient = new Patient();
        patient.setName(name);
        patient.setClinic(clinic);
        patient.setUnits(new LinkedHashSet<>(Set.of(unit)));
        return patientRepository.saveAndFlush(patient);
    }

    private ClinicUnit filial(Context context, String name) {
        ClinicUnit filial = new ClinicUnit();
        filial.setId(UUID.randomUUID());
        filial.setClinic(context.clinic());
        filial.setTenantId(context.clinic().getId());
        filial.setName(name);
        filial.setTimeZone("America/Cuiaba");
        filial.setStatus(StatusUnit.ATIVO);
        filial.setType(TypeUnit.FILIAL);
        return unitRepository.saveAndFlush(filial);
    }

    private Instant futureBase() {
        return Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.generateToken(user);
    }

    private record Context(
            Clinic clinic,
            ClinicUnit matrix,
            Patient patient,
            ServiceOffering service,
            User admin) {
    }
}
