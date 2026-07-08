# 02 - Escopo do Projeto

**Projeto:** OS Mobile - Sistema de Gestão de Ordens de Serviço para Android

**Documento:** 02 - Escopo do Projeto

**Versão:** 1.0

**Status:** Em elaboração

## 1. Introdução

Este documento detalha o escopo do projeto OS Mobile, definindo os limites e as funcionalidades que serão desenvolvidas na primeira fase (MVP - Produto Mínimo Viável) e o que será considerado para futuras iterações. O objetivo é garantir que todas as partes interessadas tenham um entendimento claro do que será entregue.

## 2. Escopo do Produto (MVP)

O MVP do OS Mobile focará em fornecer uma solução robusta para a gestão básica de ordens de serviço, com ênfase na operação offline e na configurabilidade. As funcionalidades incluídas são:

### 2.1. Gestão de Clientes

*   Cadastro, edição e exclusão de clientes.
*   Visualização de lista de clientes com opções de busca e filtro.
*   Associação de ordens de serviço e orçamentos a clientes.

### 2.2. Gestão de Serviços e Produtos

*   Cadastro, edição e exclusão de serviços e produtos oferecidos.
*   Definição de preços e descrições para cada item.
*   Utilização de serviços/produtos em orçamentos e ordens de serviço.

### 2.3. Gestão de Ordens de Serviço (OS)

*   Criação de novas ordens de serviço, associadas a um cliente e contendo serviços/produtos.
*   Definição e atualização de status da OS (ex: Aberta, Em Andamento, Concluída, Cancelada).
*   Registro de informações detalhadas da OS, como data de abertura, previsão de conclusão, observações.
*   Histórico de alterações da OS.
*   Geração de documentos de OS para impressão.

### 2.4. Gestão de Orçamentos

*   Criação de orçamentos detalhados, incluindo serviços, produtos e valores.
*   Conversão de orçamentos aprovados em ordens de serviço.
*   Visualização e acompanhamento do status dos orçamentos.

### 2.5. Módulo de Auditoria

*   Registro automático de todas as ações relevantes no sistema (criação, edição, exclusão de registros).
*   Armazenamento de data, hora, usuário (se aplicável), módulo, ação e identificação do registro afetado.
*   Histórico imutável para garantir a rastreabilidade.

### 2.6. Operação Offline

*   Todas as funcionalidades críticas (cadastro, edição, consulta de clientes, serviços, OS e orçamentos) devem ser totalmente operacionais sem conexão com a internet.
*   A sincronização de dados com a nuvem (se implementada em futuras versões) será um recurso opcional e não essencial para o funcionamento básico.

## 3. Funcionalidades Excluídas do MVP (para futuras versões)

As seguintes funcionalidades estão fora do escopo do MVP, mas são consideradas para futuras fases do projeto:

*   **Sincronização Avançada:** Sincronização automática e bidirecional com serviços de nuvem para múltiplos dispositivos.
*   **Integrações:** Conexão com sistemas de pagamento, agenda do Android, Google Contatos, etc., além da sincronização básica.
*   **Módulos Financeiros:** Controle de contas a pagar/receber, fluxo de caixa, relatórios financeiros detalhados.
*   **Relatórios e Dashboards Avançados:** Ferramentas de análise de dados e visualizações personalizáveis.
*   **Gestão de Usuários e Permissões:** Suporte a múltiplos usuários com diferentes níveis de acesso e permissões.
*   **Notificações:** Alertas e lembretes para prazos de OS, agendamentos, etc.
*   **Customização Avançada:** Temas, layouts e campos personalizados além das configurações básicas.

## 4. Critérios de Aceitação do Escopo

O escopo definido neste documento será considerado aceito quando:

*   Todas as funcionalidades listadas no 
MVP estiverem implementadas e testadas com sucesso.
*   A documentação do escopo for revisada e aprovada por todas as partes interessadas.
*   Os princípios fundamentais do sistema, conforme definidos no documento `00-Principios-do-Sistema.md`, forem respeitados em todas as funcionalidades do MVP.

## 5. Próximos Passos

Com o escopo do MVP definido, os próximos documentos detalharão os requisitos funcionais e não funcionais, a arquitetura do sistema e o modelo de banco de dados, seguindo a metodologia estabelecida.
