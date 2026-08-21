# Backup no Google Drive

## Objetivo

Permitir restaurar o sistema quando o cliente perder ou trocar o telefone.

## Estrutura

```text
Pasta escolhida no Drive
└── Backups
    └── backup_completo_YYYYMMDD_HHMMSS.json
```

## Como gerar

1. Abra `Configuracoes > Google Drive`.
2. Selecione uma pasta dentro do Drive, preferencialmente `OS Mobile`.
3. Abra `Backup`.
4. Toque em `Gerar backup completo no Drive`.
5. Aguarde a mensagem de sucesso.

## Como restaurar em outro celular

1. Instale o app no novo celular.
2. Configure a mesma conta Google no aparelho.
3. Abra `Configuracoes > Google Drive`.
4. Selecione a mesma pasta usada antes.
5. Abra `Backup`.
6. Toque em `Buscar backups no Drive`.
7. Selecione o backup desejado.
8. Digite `RESTAURAR`.
9. Toque em `Restaurar backup selecionado do Drive`.

## Observacao tecnica

O backup completo inclui dados, configuracoes portaveis, imagens/documentos e assinaturas em Base64. Chaves locais que dependem do aparelho, como URI da pasta do Drive e ids locais de contatos, nao sao restauradas para evitar referencias quebradas no novo telefone.
