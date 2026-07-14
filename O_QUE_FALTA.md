# O que Falta no Projeto - OS Mobile

Analise feita a partir dos documentos em `Projeto Makrdow/`, do `ESTADO_DO_PROJETO.md` e do codigo atual.

## Resumo

O MVP operacional esta bem avancado: cadastro de clientes, servicos/produtos, orcamentos, OS, busca, auditoria, backup, PDF, impressao Android e APK/AAB ja existem.

O que ainda falta esta concentrado em integracoes, configuracoes avancadas, qualidade de documento/impressao e validacao real em celular.

## Pendencias Criticas

### 1. Agenda Android / Google Contatos

Status: implementado no MVP.

Implementado:

- Permissao `WRITE_CONTACTS`.
- Permissao `READ_CONTACTS` para buscar agendas do aparelho.
- Configuracoes com botao para buscar agendas Google disponiveis.
- Lista de selecao da agenda Google destino.
- Botao `Agenda` na lista de clientes.
- Sincronizacao manual do cliente para agenda Android.
- Nome, telefone, email, endereco e observacoes enviados para a agenda.
- CPF/CNPJ permanece apenas no banco interno.
- ID do contato bruto salvo em configuracoes por cliente.

Falta:

- Validar em aparelho real com conta Google configurada.
- Confirmar comportamento em aparelhos sem conta Google.
- Melhorar tratamento quando o Android rejeita a conta informada.
- Opcional: sincronizar automaticamente apos salvar cliente, se configurado.
- Tratar aparelhos sem conta Google ou sem sincronizacao de contatos.

Observacao: o banco local deve continuar sendo a fonte oficial. Google Contatos deve ser apenas sincronizacao.

### 2. Impressao Bluetooth 58 mm

Status: parcialmente implementado.

Implementado:

- Geracao de documento em texto.
- PDF basico.
- Botao de impressao nativa do Android.

Falta:

- Impressao direta em impressora Bluetooth 58 mm.
- Configurar impressora padrao.
- Configurar impressao automatica sim/nao.
- Configurar quantidade de vias.
- Layout especifico para bobina 58 mm.
- Documentos adicionais previstos: recibo e garantia.

### 3. Templates de Mensagens

Status: parcialmente implementado.

Implementado:

- Templates fixos no codigo para OS e orcamento.
- Tela para editar templates em Configuracoes.
- Tokens: `{nome}`, `{telefone}`, `{cpf}`, `{os}`, `{orcamento}`, `{valor}`, `{status}`, `{empresa}`, `{data}`.
- Campo de empresa para preencher `{empresa}`.
- Compartilhamento, WhatsApp, SMS e Email.
- Ao alterar status de OS em edicao, o app pergunta se deseja avisar por WhatsApp, SMS, Email ou cancelar.

Falta:

- Templates por evento.

### 4. Configuracoes Modulares Reais

Status: parcialmente implementado.

Implementado:

- Tela de configuracoes com switches para Orcamento, Fotos, Assinatura, Checklist, Garantia e Financeiro.
- Politica CPF/CNPJ: nao usar, opcional, obrigatorio.

Falta:

- Fazer os switches ocultarem/bloquearem telas e fluxos correspondentes.
- Implementar os modulos Fotos, Assinatura, Checklist e Garantia.
- Definir regras para modulo Financeiro futuro.
- Adicionar configuracoes de impressao, empresa, conta Google e templates.

### 5. Busca e Filtros Avancados

Status: parcialmente implementado.

Implementado:

- Busca global no Painel.
- Busca de clientes.
- Busca de servicos/produtos.

Falta:

- Filtro de clientes por ativo/inativo na UI.
- Filtro de orcamentos por data.
- Filtro de orcamentos por status na UI.
- Filtros de dashboard por data, categoria, status e valor.
- Indicadores previstos: servicos mais vendidos e clientes recorrentes.

### 6. Auditoria Completa

Status: parcialmente implementado.

Implementado:

- Auditoria para criacao, status, conversao, edicoes principais e arquivamentos.
- Tela geral de auditoria.
- Historico por OS/orcamento.

Falta:

- Garantir auditoria em todas as alteracoes de clientes e servicos/produtos com detalhes de antes/depois.
- Registrar usuario quando houver suporte a usuarios.
- Testes especificos para imutabilidade e cobertura de auditoria.

### 7. PDF e Documentos Profissionais

Status: parcialmente implementado.

Implementado:

- PDF basico com texto.
- Compartilhamento de PDF.
- Impressao nativa Android.

Falta:

- Layout profissional do PDF.
- Cabecalho com dados da empresa.
- Dados completos do cliente.
- Tabela de itens mais clara.
- Rodape/termos/assinatura.
- Recibo.
- Garantia.
- Opcao de visualizacao antes de compartilhar/imprimir.

### 8. Segurança e Dados Sensíveis

Status: pendente.

Falta:

- Avaliar criptografia/protecao local para CPF/CNPJ.
- Documentar politica de permissao para contatos, Bluetooth e armazenamento.
- Solicitar permissoes apenas quando a funcionalidade for usada.

### 9. Testes e Validacao em Aparelho Real

Status: parcial.

Implementado:

- Testes unitarios existentes.
- Build debug/release validado.
- APK e AAB gerados.

Falta:

- Teste manual completo em celular real.
- Testes Room/repository.
- Testes de conversao orcamento -> OS.
- Testes de backup/restauracao.
- Testes de impressao Android.
- Testes de instalacao limpa e atualizacao de versao.

### 10. Release / Play Console

Status: parcial.

Implementado:

- APK release assinado localmente.
- AAB release gerado.

Falta:

- Subir AAB no Google Play Console em Teste interno.
- Definir versionamento para proximas versoes.
- Validar Play Protect via instalacao pelo canal oficial.
- Guardar keystore com politica de backup segura.

## Itens Fora do MVP, mas Previstos

Segundo os documentos de escopo, ficam para fases futuras:

- Sincronizacao em nuvem avancada.
- Multiplos usuarios e permissoes.
- Modulo financeiro completo.
- Modulo de estoque completo:
  - cadastro de produtos/insumos;
  - entrada, saida e ajuste de estoque;
  - baixa por OS/venda;
  - saldo atual, estoque minimo e historico de movimentacoes.
- Importacao CSV de servicos/produtos:
  - botao para baixar modelo CSV;
  - colunas padrao: codigo, nome, tipo, categoria, descricao, valor, estoque_minimo, ncm, cfop, unidade, cst_csosn;
  - validacao antes de importar;
  - relatorio de linhas importadas e linhas com erro.
- Modulo fiscal / nota eletronica:
  - status fiscal na OS/venda;
  - configuracao fiscal da empresa;
  - ambiente de homologacao/producao;
  - integracao via API fiscal externa;
  - armazenamento de chave, protocolo, XML/PDF/DANFE e rejeicoes.
- Relatorios gerenciais avancados.
- Notificacoes e lembretes.
- Integracao com pagamentos.
- Campos e layouts personalizados avancados.

## Diretriz para NF-e

Nao integrar direto com SEFAZ no primeiro ciclo. A recomendacao e usar uma API fiscal especializada, como Focus NFe, TecnoSpeed, eNotas, PlugNotas ou Nuvem Fiscal.

O app deve preparar os dados comerciais e fiscais, mas a transmissao da nota deve passar por um provedor fiscal. Isso reduz risco, manutencao e complexidade legal.

Ordem recomendada:

1. Concluir estoque local.
2. Criar venda/OS com status fiscal.
3. Criar configuracoes fiscais da empresa.
4. Integrar primeiro em homologacao via API fiscal.
5. Liberar producao somente depois de testes reais.

## Sobre CSS / Template Visual

Este projeto nao usa CSS porque nao e um app web. Ele e um app Android nativo com Kotlin e Jetpack Compose.

O equivalente a CSS/template visual esta em:

- `app/src/main/java/br/com/sos/osmobile/ui/theme/Theme.kt`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/colors.xml`
- Componentes Compose dentro de `app/src/main/java/br/com/sos/osmobile/ui/components/`

Hoje existe um tema Compose simples com cores claras/escuras, mas ainda nao existe um design system completo.

Falta criar um padrao visual reutilizavel para:

- botoes primarios/secundarios;
- botoes de acao em listas;
- cards;
- campos de formulario;
- estados vazios;
- mensagens de erro/sucesso;
- cabecalhos de tela;
- documentos/PDF.

Em Compose, isso deve ser feito criando componentes Kotlin reutilizaveis, nao arquivos CSS.

## Proxima Ordem Recomendada

1. Implementar agenda Android/Google Contatos, porque foi uma expectativa funcional citada e ainda nao existe.
2. Criar configuracoes de conta Google, empresa, impressao e templates.
3. Melhorar PDF/documentos.
4. Implementar impressao Bluetooth 58 mm.
5. Fechar testes em aparelho real.
6. Subir AAB no Play Console em Teste interno.
