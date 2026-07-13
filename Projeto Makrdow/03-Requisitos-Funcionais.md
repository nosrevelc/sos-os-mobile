# 03 - Requisitos Funcionais

**Projeto:** OS Mobile - Sistema de Gestão de Ordens de Serviço para Android

**Documento:** 03 - Requisitos Funcionais

**Versão:** 1.0

**Status:** Em elaboração

## 1. Introdução

Este documento detalha os requisitos funcionais do sistema OS Mobile para a fase de Produto Mínimo Viável (MVP). Os requisitos funcionais descrevem as funções que o sistema deve executar para atender às necessidades dos usuários e do negócio, conforme definido no documento de Escopo do Projeto.

## 2. Requisitos Funcionais por Módulo

### 2.1. Módulo de Clientes

| ID Requisito | Descrição | Prioridade | Critérios de Aceitação |
|---|---|---|---|
| RF001 | O sistema deve permitir o cadastro de novos clientes. | Alta | O usuário deve conseguir inserir nome, endereço, telefone, e-mail e CPF/CNPJ do cliente. |
| RF002 | O sistema deve permitir a edição de informações de clientes existentes. | Alta | O usuário deve conseguir modificar qualquer campo de um cliente já cadastrado. |
| RF003 | O sistema deve permitir a exclusão lógica de clientes. | Média | Um cliente excluído logicamente não deve aparecer nas listas ativas, mas seus dados devem ser preservados para histórico. |
| RF004 | O sistema deve exibir uma lista de clientes com opções de busca e filtro. | Alta | O usuário deve conseguir buscar clientes por nome, telefone ou CPF/CNPJ e filtrar por status (ativo/inativo). |
| RF005 | O sistema deve associar ordens de serviço e orçamentos a clientes. | Alta | Ao criar uma OS ou orçamento, o usuário deve selecionar um cliente existente. |

### 2.2. Módulo de Serviços e Produtos

| ID Requisito | Descrição | Prioridade | Critérios de Aceitação |
|---|---|---|---|
| RF006 | O sistema deve permitir o cadastro de novos serviços ou produtos. | Alta | O usuário deve conseguir inserir nome, descrição e preço unitário de um serviço/produto. |
| RF007 | O sistema deve permitir a edição de serviços ou produtos existentes. | Alta | O usuário deve conseguir modificar qualquer campo de um serviço/produto já cadastrado. |
| RF008 | O sistema deve permitir a exclusão lógica de serviços ou produtos. | Média | Um serviço/produto excluído logicamente não deve aparecer nas listas ativas, mas seus dados devem ser preservados para histórico. |
| RF009 | O sistema deve exibir uma lista de serviços e produtos com opções de busca. | Alta | O usuário deve conseguir buscar serviços/produtos por nome ou descrição. |
| RF009A | O cadastro deve diferenciar serviço de produto/insumo. | Alta | Produto/insumo deve poder controlar estoque; serviço deve ser usado para mão de obra e pode, futuramente, ter insumos vinculados. |
| RF009B | Produtos fiscais devem guardar campos necessários para emissão de nota. | Média | O produto deve prever campos como NCM, CFOP, unidade, origem, CST/CSOSN e alíquotas quando o módulo fiscal estiver ativo. |

### 2.3. Módulo de Ordens de Serviço (OS)

| ID Requisito | Descrição | Prioridade | Critérios de Aceitação |
|---|---|---|---|
| RF010 | O sistema deve permitir a criação de novas ordens de serviço. | Alta | O usuário deve conseguir criar uma OS, associá-la a um cliente, adicionar serviços/produtos e definir um status inicial. |
| RF011 | O sistema deve permitir a edição de informações de uma ordem de serviço. | Alta | O usuário deve conseguir modificar os detalhes de uma OS, como serviços/produtos, observações e status. |
| RF012 | O sistema deve permitir a atualização do status de uma ordem de serviço. | Alta | O usuário deve conseguir alterar o status da OS (ex: Aberta, Em Andamento, Concluída, Cancelada). |
| RF013 | O sistema deve registrar automaticamente o histórico de alterações de uma OS. | Alta | Cada alteração significativa em uma OS (status, itens, etc.) deve gerar um registro no histórico. |
| RF014 | O sistema deve gerar um documento de Ordem de Serviço para impressão. | Alta | O sistema deve gerar um PDF ou outro formato imprimível contendo os detalhes da OS. |

### 2.4. Módulo de Orçamentos

| ID Requisito | Descrição | Prioridade | Critérios de Aceitação |
|---|---|---|---|
| RF015 | O sistema deve permitir a criação de novos orçamentos. | Alta | O usuário deve conseguir criar um orçamento, associá-lo a um cliente e adicionar serviços/produtos. |
| RF016 | O sistema deve permitir a edição de orçamentos existentes. | Alta | O usuário deve conseguir modificar os detalhes de um orçamento, como serviços/produtos e observações. |
| RF017 | O sistema deve permitir a conversão de um orçamento aprovado em ordem de serviço. | Alta | Ao aprovar um orçamento, o sistema deve criar automaticamente uma nova OS com base nos itens do orçamento. |
| RF018 | O sistema deve exibir uma lista de orçamentos com opções de busca e filtro. | Alta | O usuário deve conseguir buscar orçamentos por cliente, status (aprovado/pendente/rejeitado) e filtrar por data. |

### 2.5. Módulo de Auditoria

| ID Requisito | Descrição | Prioridade | Critérios de Aceitação |
|---|---|---|---|
| RF019 | O sistema deve registrar todas as ações relevantes do usuário. | Alta | Cada criação, edição ou exclusão (lógica) de clientes, serviços, produtos, OS e orçamentos deve ser registrada. |
| RF020 | O registro de auditoria deve incluir data, hora, usuário, módulo, ação e ID do registro afetado. | Alta | O histórico deve conter todas as informações necessárias para rastrear a ação. |
| RF021 | O histórico de auditoria deve ser imutável. | Alta | Nenhuma entrada no histórico de auditoria pode ser alterada ou excluída pelo usuário. |

### 2.6. Módulo de Estoque

| ID Requisito | Descrição | Prioridade | Critérios de Aceitação |
|---|---|---|---|
| RF022 | O sistema deve controlar estoque de produtos e insumos. | Alta | O usuário deve visualizar saldo atual, entradas, saídas e ajustes de cada item. |
| RF023 | O sistema deve registrar movimentações de estoque. | Alta | Toda entrada, saída, baixa por OS/venda ou ajuste manual deve gerar histórico com data, quantidade, motivo e vínculo quando existir. |
| RF024 | O sistema deve baixar estoque a partir de OS ou venda. | Alta | Ao usar produto/insumo em OS ou venda, o saldo deve ser reduzido conforme regra configurada. |
| RF025 | O sistema deve alertar estoque baixo. | Média | Itens abaixo do estoque mínimo devem aparecer em alerta ou relatório. |

### 2.7. Módulo Fiscal / Nota Eletrônica

| ID Requisito | Descrição | Prioridade | Critérios de Aceitação |
|---|---|---|---|
| RF026 | O sistema deve preparar vendas/OS para emissão fiscal futura. | Alta | A OS/venda deve possuir cliente, itens, valores, pagamentos e status fiscal. |
| RF027 | O sistema deve integrar com API fiscal externa para emissão de NF-e/NFC-e/NFS-e. | Alta | A emissão deve ocorrer via provedor fiscal, evitando integração direta inicial com SEFAZ. |
| RF028 | O sistema deve separar ambiente fiscal de homologação e produção. | Alta | O usuário deve escolher o ambiente nas configurações fiscais. |
| RF029 | O sistema deve armazenar dados fiscais da empresa. | Alta | Deve haver configuração de CNPJ, inscrição estadual/municipal, regime tributário, certificado/token da API fiscal e série/numeração quando aplicável. |
| RF030 | O sistema deve controlar status da nota. | Alta | Status mínimos: não emitida, em emissão, autorizada, rejeitada, cancelada. |
| RF031 | O sistema deve registrar retorno fiscal. | Alta | Deve salvar número, chave de acesso, protocolo, XML/PDF/DANFE quando retornados pela API fiscal. |

## 3. Considerações Finais

Os requisitos funcionais aqui descritos são a base para o desenvolvimento do MVP do OS Mobile. Eles serão complementados pelos requisitos não funcionais e detalhados em especificações técnicas futuras. Qualquer alteração ou adição a estes requisitos deverá seguir o processo de gestão de mudanças do projeto.
