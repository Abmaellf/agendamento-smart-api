# Contratos de pacotes de serviço

`ServicePackageRequest` recebe o formulário administrativo: nome, descrição opcional, IDs de unidade e serviço, total de sessões positivo, preço decimal não negativo, ao menos um dia da semana e situação. A validação HTTP rejeita campos obrigatórios ausentes; o serviço complementa a verificação de dias repetidos e do escopo.

`ServicePackageResponse` devolve a definição completa, incluindo nomes de unidade/serviço e `weeklyFrequency`, derivada da coleção de dias. `ServicePackageStatusRequest` limita a alteração de situação ao booleano `active`.

Para contratar um pacote, `PackageAppointmentRequest` exige `packageId`, `patientId`, `unitId`, data inicial, horário e dias efetivos. O `unitId` é obrigatório para que a API valide o escopo do usuário, a unidade do pacote e o vínculo do paciente antes de gerar a prévia ou persistir a contratação.
