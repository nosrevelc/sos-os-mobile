# Entrega e Retirada

Implementacao inicial em 14/07/2026.

## Objetivo

Controlar se uma OS sera retirada no local ou entregue ao cliente, mantendo informacoes suficientes para mensagem, acompanhamento e historico operacional.

## Campos na OS

- Tipo de entrega:
  - Retirada no local
  - Entrega propria
  - Correios/transportadora
  - Motoboy
- Status de entrega:
  - Aguardando retirada
  - Saiu para entrega
  - Pedido enviado
  - Entregue
  - Nao entregue
- Endereco de entrega
- Taxa de entrega
- Codigo de rastreio
- Observacoes de entrega

## Tokens de mensagem

- `{tipo_entrega}`
- `{status_entrega}`
- `{endereco_entrega}`
- `{taxa_entrega}`
- `{codigo_rastreio}`

## Proximos passos

- Criar templates especificos:
  - Pedido enviado
  - Saiu para entrega
  - Entregue
  - Nao entregue
- Criar filtros/listas por status de entrega.
- Adicionar comprovante/foto de entrega.
- Avaliar se taxa de entrega deve entrar no total financeiro da OS.
