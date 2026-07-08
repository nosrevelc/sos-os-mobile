# 00 - Princípios do Sistema

**Projeto:** OS Mobile - Sistema de Gestão de Ordens de Serviço para Android

**Documento:** 00 - Princípios do Sistema

**Versão:** 1.0

**Status:** Concluído (primeira versão)

## 1. Objetivo

Este documento estabelece os princípios fundamentais que regem todo o desenvolvimento do sistema OS Mobile. Todos os módulos, funcionalidades, regras de negócio e futuras evoluções deverão respeitar integralmente estes princípios. Em caso de conflito entre uma funcionalidade e um princípio, o princípio prevalecerá até que a documentação seja revisada.

## 2. Filosofia do Produto

O OS Mobile é um aplicativo Android desenvolvido para gerenciar o ciclo completo de atendimento ao cliente, desde o cadastro até a conclusão de uma Ordem de Serviço, com foco em simplicidade, confiabilidade, operação offline e alta configurabilidade. O sistema deve atender profissionais autônomos, microempresas e pequenas empresas de diferentes segmentos, permitindo que cada negócio utilize apenas os recursos necessários.

## 3. Público-Alvo

O sistema foi projetado para empresas que realizam prestação de serviços, incluindo, mas não se limitando a:

*   Assistência técnica de celulares.
*   Assistência técnica de computadores.
*   Oficinas mecânicas.
*   Assistência de eletrodomésticos.
*   Costureiras.
*   Marcenarias.
*   Serralherias.
*   Eletricistas.
*   Encanadores.
*   Técnicos em refrigeração.
*   Prestadores de serviços em geral.

A arquitetura não deve conter regras específicas para um único segmento. Sempre que possível, as diferenças entre segmentos devem ser resolvidas por configurações.

## 4. Princípios Fundamentais

### Princípio 1 — Offline First

O sistema deve funcionar integralmente sem conexão com a internet. A internet será utilizada apenas para recursos opcionais, como sincronização de contatos, envio de mensagens ou backup em nuvem. A indisponibilidade de internet nunca poderá impedir o uso das funções principais do aplicativo.

### Princípio 2 — Banco de Dados Local como Fonte Oficial

O banco de dados interno do aplicativo é a fonte oficial de todas as informações. Serviços externos, como agenda do Android, Google Contatos ou outros, serão utilizados apenas para sincronização. Nenhuma informação essencial dependerá exclusivamente de um serviço externo.

### Princípio 3 — Modularidade

O sistema será composto por módulos independentes. Cada módulo poderá ser ativado ou desativado pelo usuário nas configurações. A desativação de um módulo deverá ocultar sua interface e impedir seu uso, sem comprometer os demais módulos.

### Princípio 4 — Configurabilidade

Toda funcionalidade que possa variar entre modelos de negócio deve ser configurável. Exemplos:

*   utilização de CPF;
*   utilização de fotos;
*   utilização de assinatura;
*   utilização de checklist;
*   utilização de orçamentos;
*   impressão automática;
*   quantidade de vias impressas.

### Princípio 5 — Auditoria

Toda ação relevante deverá ser registrada no Histórico do Sistema. O histórico deverá registrar, no mínimo:

*   data e hora;
*   usuário responsável (quando aplicável);
*   módulo;
*   ação executada;
*   identificação do registro afetado.

O histórico não poderá ser alterado ou removido pelo usuário.

### Princípio 6 — Integridade dos Dados

O sistema deve preservar a consistência das informações. Sempre que possível:

*   utilizar exclusão lógica para registros importantes;
*   impedir operações que gerem dados órfãos;
*   validar referências entre entidades.

### Princípio 7 — Simplicidade

As operações mais frequentes devem exigir o menor número possível de interações. Como diretriz geral, nenhuma função principal deverá exigir mais de três ações consecutivas para ser iniciada.

### Princípio 8 — Escalabilidade

A arquitetura deverá permitir a inclusão de novos módulos sem necessidade de reestruturação significativa do sistema. Novas funcionalidades deverão reutilizar componentes existentes sempre que possível.

### Princípio 9 — Padronização

Todos os módulos deverão seguir padrões comuns para:

*   telas;
*   botões;
*   mensagens;
*   validações;
*   histórico;
*   impressão;
*   notificações;
*   nomenclatura.

Isso reduz a curva de aprendizado e melhora a experiência do usuário.

### Princípio 10 — IA Friendly

Toda regra de negócio deverá estar documentada. Nenhuma decisão funcional poderá depender exclusivamente da interpretação do desenvolvedor ou da IA responsável pela implementação. Quando houver dúvida, a documentação deverá ser atualizada antes do desenvolvimento.

### Princípio 11 — Segurança

O aplicativo deverá solicitar apenas as permissões necessárias ao seu funcionamento. Permissões como acesso à agenda, SMS, armazenamento ou Bluetooth deverão ser solicitadas apenas quando exigidas pela funcionalidade correspondente.

### Princípio 12 — Desempenho

O sistema deverá manter boa responsividade em dispositivos Android compatíveis. As consultas ao banco de dados e a navegação entre telas devem ser projetadas para minimizar atrasos perceptíveis durante o uso normal.

### Princípio 13 — Evolução Contínua

Toda nova funcionalidade deverá ser incorporada à documentação antes de sua implementação. A documentação será considerada a referência oficial do projeto.

## 5. Convenções Gerais

*   Datas no formato configurável pelo sistema.
*   Valores monetários em Real (R$), com possibilidade de internacionalização futura.
*   Todos os identificadores internos devem ser únicos.
*   Todas as operações críticas devem gerar registro no histórico.
*   Impressões devem respeitar as configurações definidas pelo usuário.
*   Mensagens automáticas devem utilizar modelos configuráveis com tokens documentados.

## 6. Critério de Aceitação deste Documento

Este documento será considerado aprovado quando todos os módulos do sistema puderem ser desenvolvidos respeitando integralmente os princípios aqui definidos.

## Próximo Documento

Agora que definimos a "constituição" do sistema, o próximo será `01 - Introdução e Visão Geral do Produto`. Nele vamos detalhar a missão do sistema, os problemas que ele resolve, o perfil dos usuários, o escopo do projeto, os objetivos funcionais e os limites da primeira versão (MVP) em relação às evoluções futuras. Esse documento servirá como a porta de entrada para qualquer pessoa ou IA que precise compreender o projeto antes de trabalhar em um módulo específico.
