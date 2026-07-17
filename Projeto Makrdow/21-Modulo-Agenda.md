# Modulo Agenda

## Objetivo

Atender negocios que trabalham com hora marcada, como manicure, atelie, assistencia, retirada, prova, entrega ou retorno.

## Fluxo

1. O cliente e cadastrado normalmente.
2. O operador cria um agendamento vinculado ao cliente.
3. O app salva o agendamento localmente primeiro.
4. Se uma agenda Android/Google estiver selecionada, o app cria/atualiza o evento no calendario.
5. Ao iniciar o atendimento, o operador pode criar uma OS a partir do agendamento.
6. A OS passa a controlar servicos, pagamento, comprovantes, fotos, assinatura, garantia e documentos.

## Status do agendamento

- Agendado
- Confirmado
- Compareceu
- Nao compareceu
- Remarcado
- Cancelado
- Concluido

## Mensagens

Templates configuraveis:

- Agendamento criado
- Lembrete 2 dias antes
- Lembrete 1 dia antes
- Lembrete no dia

Tokens:

- `{nome}`
- `{telefone}`
- `{empresa}`
- `{os}`
- `{agendamento}`
- `{agendamento_tipo}`
- `{agendamento_status}`
- `{agendamento_data}`
- `{agendamento_hora}`

## Regra de OS

Agendamento nao vira OS automaticamente. A OS deve ser criada quando o atendimento realmente iniciar, para evitar gerar OS de clientes que faltaram, cancelaram ou remarcaram.
