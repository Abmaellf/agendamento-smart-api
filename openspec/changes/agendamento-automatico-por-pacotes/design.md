# Design: Agendamento automático por pacotes

## Context

A SPEC 01 fornece pacotes ativos por unidade, serviço, sessões, preço e dias da
semana. A agenda manual já recebe um instante explícito e concentra as regras
de criação em `AppointmentService`.

## Decisions

1. `PACKAGE_CONTRACT` representa a contratação do paciente e guarda os dados
   comerciais e dias efetivos como snapshot. Uma tabela filha normalizada evita
   duplicidade de dia.
2. `APPOINTMENT` recebe relação anulável para a contratação; legados continuam
   sem vínculo.
3. A prévia calcula datas sem persistir. A confirmação recomputa o fingerprint,
   valida disponibilidade sob lock e cria contrato e appointments numa transação.
4. O horário informado pelo fluxo manual é reutilizado em todas as sessões.
   Nenhum horário, profissional ou dia alternativo é inventado.
5. Não consultar nem bloquear por feriados, funcionamento ou bloqueios de
   calendário nesta change; essa regra será objeto de futura SPEC.

## API shape

- `POST /api/package-appointments/preview`
- `POST /api/package-appointments/confirm` com `Idempotency-Key`
- `GET /api/package-appointments/{id}`

## Risks

- Prévia obsoleta: fingerprint e revalidação na confirmação.
- Falha no lote: transação única sem persistência parcial.
- Alteração futura do pacote: snapshot permanece como fonte da contratação.
