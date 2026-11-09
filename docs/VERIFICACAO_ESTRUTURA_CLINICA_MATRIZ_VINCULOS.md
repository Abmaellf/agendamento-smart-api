# Verificação da estrutura de clínica, MATRIZ e vínculos

## Escopo e ambiente

Verificação executada em 8 de novembro de 2026 para a change
`estrutura-clinica-matriz-vinculos`, sem usar dados pessoais ou ambiente
persistente.

- Java 21;
- Spring Boot 3.4.5;
- PostgreSQL 18.6 descartável via Testcontainers 1.21.4;
- Flyway com `ddl-auto=validate`;
- migrations versionadas V001–V010.

## Resultado do fluxo ponta a ponta

Comando:

```bash
./mvnw -Dtest=ClinicStructureEndToEndIntegrationTest test
```

Resultado observado: `BUILD SUCCESS`, 1 teste, nenhuma falha ou erro.

O cenário criou duas clínicas pela API e verificou, no mesmo PostgreSQL:

- clínica e única MATRIZ nascendo `ATIVO`;
- dois profissionais/usuários iniciais por clínica;
- exatamente um `ADMIN` e uma MATRIZ por clínica;
- cadastro posterior de `USER` com profissional exclusivo da MATRIZ;
- paciente associado à unidade da própria clínica;
- um appointment por clínica, um com profissional e outro sem;
- listagem da primeira clínica com e sem `unitId` retornando somente seu
  appointment;
- filtro por unidade e detalhe por appointment da segunda clínica respondendo
  `404/TENANT_RESOURCE_NOT_FOUND` para o primeiro usuário.

## Resultado da suíte completa

Comando:

```bash
./mvnw clean verify
```

Resultado observado: `BUILD SUCCESS` em 45,757 segundos, com 34 testes, zero
falhas, zero erros e zero ignorados. O build aplicou e validou V001–V010 em
schema vazio, inicializou o Hibernate em modo de validação e gerou
`target/agendamento-smart-api-1.0.5.jar`.

| Suíte | Testes | Resultado |
|---|---:|---|
| `ClinicStructureEndToEndIntegrationTest` | 1 | passou |
| `ClinicOnboardingServiceIntegrationTest` | 2 | passou |
| `ClinicRegistrationControllerIntegrationTest` | 2 | passou |
| `UserRegistrationControllerIntegrationTest` | 5 | passou |
| `PatientRegistrationControllerIntegrationTest` | 5 | passou |
| `AppointmentCreationStructureRulesIntegrationTest` | 7 | passou |
| `AppointmentReadStatusIntegrationTest` | 12 | passou |

## Validação OpenSpec

Comando:

```bash
/home/abmael/.nvm/versions/node/v22.14.0/bin/openspec validate estrutura-clinica-matriz-vinculos --type change --strict --json --no-interactive
```

Resultado observado: 1 change validada, 1 aprovada, 0 rejeitadas e nenhuma
issue reportada.

## Evidências complementares

- O relatório e o procedimento de pré-migração estão em
  [PREFLIGHT_ESTRUTURA_CLINICA_MATRIZ_VINCULOS.md](PREFLIGHT_ESTRUTURA_CLINICA_MATRIZ_VINCULOS.md).
- A verificação SQL das constraints da V010 está em
  [verify-v010-clinic-structure.sql](sql/verify-v010-clinic-structure.sql).
- Os contratos exercitados estão em [API de cadastros](API_CADASTROS.md) e
  [API de appointments](API_APPOINTMENTS.md).
