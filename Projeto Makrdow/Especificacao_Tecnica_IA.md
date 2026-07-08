# Documento 1 - Especificação Técnica para IA

## Objetivo

Este documento define os requisitos obrigatórios para implementação do
aplicativo Android de Ordem de Serviço.

## Premissas

-   Aplicação Android nativa.
-   Funcionamento offline-first.
-   Banco de dados local como fonte oficial.
-   Sincronização opcional com agenda Android.
-   Arquitetura modular.
-   Todas as funcionalidades opcionais devem ser controladas pela tela
    de Configurações.

## Módulos

### Clientes

-   Cadastro local.
-   Sincronizar contato para agenda Android (campos suportados).
-   Manter ID do contato sincronizado.
-   CPF armazenado apenas no banco interno.
-   Pesquisa por nome, telefone, CPF e documento.

### Configurações

Permitir habilitar/desabilitar: - Orçamento - Fotos - Assinatura -
Checklist - Garantia - Financeiro (futuro)

CPF: - Não utilizar - Opcional - Obrigatório

### Serviços

Tabela de preços editável. Cada item: - Código - Nome - Categoria -
Valor padrão - Ativo

### Orçamento

Numeração OR+AAMMDD+sequência anual. Status configuráveis. Conversão
automática para OS.

### Ordem de Serviço

Numeração AAMMDD+sequência anual. Itens editáveis no momento da
abertura. Status configuráveis. Mensagens por token. Histórico imutável.

### Mensagens

Templates por evento. Canais: - WhatsApp - SMS - Email

Tokens: {nome} {telefone} {cpf} {os} {orcamento} {valor} {status}
{empresa} {data}

### Impressão

Bluetooth 58 mm. Cada documento possui: - imprimir automaticamente
(sim/não) - quantidade de vias

Documentos: OS Orçamento Recibo Garantia

### Dashboard

Filtros: Data Categoria Status Valor

Indicadores: Quantidade de OS Faturamento Serviços mais vendidos
Clientes recorrentes

## Requisitos não funcionais

-   Offline
-   Backup
-   Log de auditoria
-   Performance
-   Banco preparado para expansão.
