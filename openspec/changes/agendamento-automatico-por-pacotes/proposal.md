# Change: Agendamento automático por pacotes

## Why

Permitir que uma contratação de pacote gere vários agendamentos individuais a
partir da data mínima, dos dias efetivos escolhidos e do horário usado no fluxo
manual de agendamento, preservando snapshot, atomicidade e idempotência.

## What Changes

- Criar contratação de pacote separada da definição administrativa.
- Persistir snapshot imutável e vínculo opcional em `APPOINTMENT`.
- Expor prévia não persistente, confirmação transacional e consulta.
- Reutilizar as regras do `AppointmentService` para horário manual, duração,
  preço, profissional opcional, conflitos e capacidade.
- Calcular datas apenas pelos dias efetivos e data mínima; calendário, feriados
  e bloqueios operacionais ficam para uma especificação futura.

## Impact

- Nova migration, entidades, DTOs, serviços, endpoints e testes na API.
- O `POST /api/appointments` manual permanece compatível.
- Contratos novos exigem coordenação com o frontend e chave de idempotência.
