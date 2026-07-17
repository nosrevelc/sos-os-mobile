# Sincronizacao Google Drive

## Objetivo

O sistema deve continuar funcionando localmente no Android mesmo sem internet. A criacao da OS, anexos, fotos, comprovantes, assinatura e documentos ocorre primeiro no banco/armazenamento local. Quando houver internet e uma pasta do Drive estiver configurada, o app tenta sincronizar.

## Estrutura no Drive

O usuario seleciona uma pasta raiz pelo seletor nativo do Android, preferencialmente dentro do Google Drive.

Estrutura criada pelo app:

```text
Pasta escolhida
└── Nome do cliente
    └── OS-000001
        ├── Fotos
        ├── Comprovantes
        ├── Assinaturas
        └── Documentos
```

## Regra principal

1. A OS e salva localmente primeiro.
2. Se Drive estiver configurado e houver internet, o app cria a pasta da OS.
3. Se nao houver internet/configuracao, a OS fica pendente.
4. Ao anexar foto ou comprovante, o app salva localmente e tenta sincronizar no mesmo momento.
5. Falhas nao bloqueiam o atendimento.

## Status de sincronizacao

- `Pendente`: aguardando internet ou nova tentativa.
- `Sincronizado`: pasta/arquivo criado no Drive.
- `Erro`: houve falha ao criar pasta/arquivo.
- `Sem configuracao`: usuario ainda nao escolheu a pasta do Drive.

## Implementacao atual

A primeira versao usa Storage Access Framework do Android. Isso evita OAuth e API Google direta: o usuario escolhe uma pasta do Drive pelo seletor do sistema, o app grava nela, e o provedor do Google Drive faz a sincronizacao.

Para uma versao comercial multi-dispositivo, o modulo pode evoluir para Google Drive API oficial com OAuth, escopo `drive.file`, controle de conta, fila robusta e reprocessamento em segundo plano.
