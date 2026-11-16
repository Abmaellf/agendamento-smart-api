# Contratos de pacotes de serviço

`ServicePackageRequest` recebe o formulário administrativo: nome, descrição opcional, IDs de unidade e serviço, total de sessões positivo, preço decimal não negativo, ao menos um dia da semana e situação. A validação HTTP rejeita campos obrigatórios ausentes; o serviço complementa a verificação de dias repetidos e do escopo.

`ServicePackageResponse` devolve a definição completa, incluindo nomes de unidade/serviço e `weeklyFrequency`, derivada da coleção de dias. `ServicePackageStatusRequest` limita a alteração de situação ao booleano `active`.
