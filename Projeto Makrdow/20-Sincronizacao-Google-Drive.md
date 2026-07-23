# Sincronizacao Google Drive

## Objetivo

O sistema deve continuar funcionando localmente no Android mesmo sem internet. A criacao da OS, anexos, imagens, documentos e assinatura ocorre primeiro no banco/armazenamento local. Quando houver internet e uma pasta do Drive estiver configurada, o app tenta sincronizar.

## Estrutura no Drive

O usuario seleciona uma pasta raiz pelo seletor nativo do Android, preferencialmente dentro do Google Drive.

Estrutura criada pelo app:

```text
Pasta escolhida
└── Nome do cliente_Telefone
    └── OS-2607170001
        ├── Imagens
        ├── Documentos
        ├── Design
        └── Assinaturas
```

## Regra principal

1. A OS e salva localmente primeiro.
2. Se Drive estiver configurado e houver internet, o app cria a pasta da OS.
3. Se nao houver internet/configuracao, a OS fica pendente.
4. A pasta da OS usa exatamente o numero gerado pelo sistema, por exemplo `OS-2607170001`.
5. A pasta do cliente usa nome e telefone para evitar conflito entre clientes com nomes iguais.
6. Ao anexar imagem ou documento, o app salva localmente e tenta sincronizar no mesmo momento.
7. Ao anexar documento, o app pergunta qual documento se trata antes de abrir o seletor de arquivo.
8. Falhas nao bloqueiam o atendimento.

## Importacao manual de arquivos do Drive para a OS

Alguns arquivos podem ser criados fora do aplicativo, por exemplo artes, layouts e arquivos de design feitos em computador. Para isso a OS possui uma pasta de entrada no Drive:

```text
Pasta escolhida
└── Nome do cliente_Telefone
    └── OS-2607170001
        └── Design
```

Regra da funcionalidade:

1. O app nao baixa arquivos do Drive em segundo plano.
2. Na tela da OS existe o botao `Importar Design do Drive`.
3. Ao tocar no botao, o app cria a pasta `Design` se ela ainda nao existir.
4. Se a pasta estiver vazia, o app informa o usuario para colocar os arquivos nela e tentar novamente.
5. Se houver arquivos, o app baixa para o armazenamento local e registra como documento da OS.
6. Arquivos ja importados nao sao duplicados.
7. O arquivo importado recebe tipo visual `Design` e status `Sincronizado`.
8. Arquivo `Design` nao entra na fila de envio para as pastas `Documentos` ou `Imagens`.
9. O retorno ao usuario deve informar quantos arquivos foram importados e quantos ja existiam.
10. Os arquivos importados passam a aparecer na lista de anexos da OS junto com documentos e imagens, mas sem serem confundidos com eles.

## Status de sincronizacao

- `Pendente`: aguardando internet ou nova tentativa.
- `Sincronizado`: pasta/arquivo criado no Drive.
- `Erro`: houve falha ao criar pasta/arquivo.
- `Sem configuracao`: usuario ainda nao escolheu a pasta do Drive.

## Pasta apagada no Drive

Se uma pasta ou arquivo sincronizado for apagado manualmente no Google Drive, o app deve detectar na proxima verificacao. Na tela da OS existe a acao `Refazer sincronizacao Drive`, que limpa as referencias antigas, marca a OS e anexos como pendentes, recria a estrutura no Drive e reenvia os anexos locais.

Use esta acao quando:

- a pasta do cliente foi apagada no Drive;
- a pasta da OS foi apagada no Drive;
- imagens ou documentos sumiram do Drive;
- o status indica sincronizado, mas o arquivo nao aparece mais no Drive.

## Implementacao atual

A primeira versao usa Storage Access Framework do Android. Isso evita OAuth e API Google direta: o usuario escolhe uma pasta do Drive pelo seletor do sistema, o app grava nela, e o provedor do Google Drive faz a sincronizacao.

Para uma versao comercial multi-dispositivo, o modulo pode evoluir para Google Drive API oficial com OAuth, escopo `drive.file`, controle de conta, fila robusta e reprocessamento em segundo plano.
