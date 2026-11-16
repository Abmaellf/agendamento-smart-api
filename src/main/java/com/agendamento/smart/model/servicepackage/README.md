# Modelo de pacotes de serviço

`ServicePackageDefinition` representa a definição administrativa de um pacote pertencente a uma única clínica e uma única unidade. Ele guarda serviço, nome, descrição, quantidade total de sessões, preço decimal, situação e os dias de semana selecionados.

`PackageWeekDay` é o conjunto de dias permitido pela definição. A frequência semanal não é configurada separadamente: é derivada da quantidade de dias distintos. A coleção persiste em `SERVICE_PACKAGE_WEEK_DAY`, cuja chave primária impede repetição.

Nesta SPEC não há profissional, paciente, contrato nem agendamento associado ao pacote. Esses vínculos pertencem à SPEC 02.
