package com.agendamento.smart.controller.appointment;

import com.agendamento.smart.PostgresIntegrationTestSupport;
import com.agendamento.smart.dtos.appointment.AppointmentRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationRequest;
import com.agendamento.smart.dtos.clinic.ClinicRegistrationResponse;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.clinic.StatusClinic;
import com.agendamento.smart.model.patient.Patient;
import com.agendamento.smart.model.professional.Professional;
import com.agendamento.smart.model.serviceoffering.ServiceOffering;
import com.agendamento.smart.model.unit.ClinicUnit;
import com.agendamento.smart.model.unit.StatusUnit;
import com.agendamento.smart.model.unit.TypeUnit;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.AppointmentRepository;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.PatientRepository;
import com.agendamento.smart.repository.ProfessionalRepository;
import com.agendamento.smart.repository.ServiceOfferingRepository;
import com.agendamento.smart.repository.UnitRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.ClinicOnboardingService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentCreationStructureRulesIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClinicOnboardingService onboardingService;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @ParameterizedTest
    @EnumSource(value = StatusClinic.class, names = {"INATIVO", "BLOQUEADA"})
    void rejectsCreationWhenClinicIsNotActive(StatusClinic statusClinic) throws Exception {
        Context context = context("clinic-" + statusClinic.name().toLowerCase());
        context.clinic().setStatus(statusClinic);
        clinicRepository.saveAndFlush(context.clinic());

        performCreate(context, request(context, context.patient().getId(), null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLINIC_APPOINTMENTS_DISABLED"));

        assertThat(appointmentRepository.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = StatusUnit.class, names = {"INATIVO", "BLOQUEADA"})
    void rejectsCreationWhenUnitIsNotActive(StatusUnit statusUnit) throws Exception {
        Context context = context("unit-" + statusUnit.name().toLowerCase());
        context.matrix().setStatus(statusUnit);
        unitRepository.saveAndFlush(context.matrix());

        performCreate(context, request(context, context.patient().getId(), null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNIT_APPOINTMENTS_DISABLED"));

        assertThat(appointmentRepository.count()).isZero();
    }

    @Test
    void rejectsPatientThatIsNotLinkedToSelectedUnit() throws Exception {
        Context context = context("patient-unit");
        ClinicUnit filial = filial(context, "Filial do paciente");
        Patient patientFromFilial = patient(context, "Paciente da filial", filial);

        performCreate(context, request(context, patientFromFilial.getId(), null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PATIENT_UNIT_MISMATCH"));

        assertThat(appointmentRepository.count()).isZero();
    }

    @Test
    void rejectsProfessionalThatIsLinkedToAnotherUnit() throws Exception {
        Context context = context("professional-unit");
        ClinicUnit filial = filial(context, "Filial do profissional");
        Professional professional = professional(context, filial);

        performCreate(context, request(context, context.patient().getId(), professional.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_UNIT_MISMATCH"));

        assertThat(appointmentRepository.count()).isZero();
    }

    @Test
    void createsAppointmentWithoutProfessionalWhenPatientBelongsToUnit() throws Exception {
        Context context = context("without-professional");

        performCreate(context, request(context, context.patient().getId(), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitId").value(context.matrix().getId().toString()))
                .andExpect(jsonPath("$.patient.id").value(context.patient().getId().toString()))
                .andExpect(jsonPath("$.professional").doesNotExist());

        assertThat(appointmentRepository.findAll())
                .singleElement()
                .satisfies(appointment -> assertThat(appointment.getProfessional()).isNull());
    }

    @Test
    void allowsSameProfessionalForDifferentPatientsInCollectiveService() throws Exception {
        Context context = context("collective-professional");
        Professional professional = professional(context, context.matrix());
        Patient secondPatient = patient(context, "Segundo paciente", context.matrix());
        Instant startsAt = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        performCreate(context, request(context, context.patient().getId(), professional.getId(), startsAt))
                .andExpect(status().isCreated());
        performCreate(context, request(context, secondPatient.getId(), professional.getId(), startsAt))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsSameProfessionalForDifferentPatientsInIndividualService() throws Exception {
        Context context = context("individual-professional");
        context.service().setName("Atendimento individual");
        context.service().setCapacity(2);
        serviceRepository.saveAndFlush(context.service());
        Professional professional = professional(context, context.matrix());
        Patient secondPatient = patient(context, "Segundo paciente", context.matrix());
        Instant startsAt = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        performCreate(context, request(context, context.patient().getId(), professional.getId(), startsAt))
                .andExpect(status().isCreated());
        performCreate(context, request(context, secondPatient.getId(), professional.getId(), startsAt))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_APPOINTMENT_CONFLICT"));
    }

    @Test
    void rejectsCollectiveServiceWhenCapacityIsExhausted() throws Exception {
        Context context = context("collective-capacity");
        context.service().setCapacity(1);
        serviceRepository.saveAndFlush(context.service());
        Professional professional = professional(context, context.matrix());
        Patient secondPatient = patient(context, "Segundo paciente", context.matrix());
        Instant startsAt = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        performCreate(context, request(context, context.patient().getId(), professional.getId(), startsAt))
                .andExpect(status().isCreated());
        performCreate(context, request(context, secondPatient.getId(), professional.getId(), startsAt))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SERVICE_CAPACITY_EXCEEDED"));
    }

    @Test
    void rejectsSamePatientEvenInCollectiveService() throws Exception {
        Context context = context("collective-patient-conflict");
        Professional professional = professional(context, context.matrix());
        Instant startsAt = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        performCreate(context, request(context, context.patient().getId(), professional.getId(), startsAt))
                .andExpect(status().isCreated());
        performCreate(context, request(context, context.patient().getId(), professional.getId(), startsAt))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PATIENT_APPOINTMENT_CONFLICT"));
    }

    private org.springframework.test.web.servlet.ResultActions performCreate(
            Context context,
            AppointmentRequest request) throws Exception {
        return mockMvc.perform(post("/api/appointments")
                .header("Authorization", bearer(context.admin()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }

    private AppointmentRequest request(Context context, UUID patientId, UUID professionalId) {
        return request(context, patientId, professionalId,
                Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS));
    }

    private AppointmentRequest request(
            Context context,
            UUID patientId,
            UUID professionalId,
            Instant startsAt) {
        return new AppointmentRequest(
                patientId,
                context.matrix().getId(),
                context.service().getId(),
                professionalId,
                startsAt,
                context.service().getDurationMinutes(),
                context.service().getPrice());
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
                                "Administrative@123")));
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
        service.setCapacity(3);
        service = serviceRepository.saveAndFlush(service);

        return new Context(clinic, matrix, patient, service, admin);
    }

    private Patient patient(Context context, String name, ClinicUnit unit) {
        return patient(context.clinic(), name, unit);
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

    private Professional professional(Context context, ClinicUnit unit) {
        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setClinic(context.clinic());
        professional.setName("Profissional da filial");
        professional.setAvailableWeekDaysValue("1,2,3,4,5,6,7");
        professional.setUnit(unit);
        professional.setServices(new LinkedHashSet<>(Set.of(context.service())));
        return professionalRepository.saveAndFlush(professional);
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
