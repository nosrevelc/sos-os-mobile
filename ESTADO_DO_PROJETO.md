# Estado do Projeto - OS Mobile

## Repositorio

- GitHub: `https://github.com/nosrevelc/sos-os-mobile`
- Branch principal: `master`

## Situacao atual

Projeto Android nativo em Kotlin/Jetpack Compose, offline-first, usando Room/SQLite.

Builds validados:

```bash
./gradlew test assembleDebug
./gradlew test assembleRelease
./gradlew bundleRelease
```

Resultado atual esperado: `BUILD SUCCESSFUL`.

APK preferencial para teste no celular:

```text
app/build/outputs/apk/release/app-release.apk
```

AAB para Play Console/Teste interno:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Stack

- Kotlin
- Jetpack Compose
- Room/SQLite
- MVVM
- Repositories
- Navigation Compose
- Gradle Wrapper

## Modulos implementados

### Painel

- Dashboard com indicadores.
- Busca global por cliente, telefone, OS, orcamento e status.
- Resultados clicaveis:
  - Cliente abre detalhe do cliente.
  - OS abre detalhe da OS.
  - Orcamento abre detalhe do orcamento.

### Clientes

- Cadastro.
- Edicao.
- Busca.
- Arquivamento logico.
- Validacao CPF/CNPJ conforme configuracao.
- Tratamento de CPF/CNPJ duplicado.
- Detalhe do cliente com OS e orcamentos vinculados.
- Vínculos clicaveis para detalhe de OS/orcamento.
- Sincronizacao manual de cliente para agenda Android/Google.
- Ao cadastrar cliente, verifica se o contato ja existe; se nao existir, pergunta se deseja adicionar.
- Se o contato ja existir, avisa o operador e evita duplicidade.
- Permissao de contatos solicitada somente ao usar a acao de agenda.
- Configuracoes buscam as agendas/contas de contatos disponiveis no aparelho.

### Servicos/Produtos

- Cadastro.
- Edicao.
- Busca.
- Arquivamento logico.
- Codigo automatico no formato `SP-0001`.
- Tratamento de codigo duplicado.

### Orcamentos

- Criacao com cliente, itens, status, observacoes e total.
- Edicao de cliente, itens, status e observacoes.
- Remocao de itens durante criacao/edicao.
- Orcamento convertido fica bloqueado para edicao.
- Cliente em seletor pesquisavel.
- Servico/produto em seletor pesquisavel com resultados abaixo da busca.
- Status: Pendente, Aprovado, Rejeitado, Convertido.
- Alteracao de status.
- Conversao de orcamento aprovado em OS.
- Documento em texto.
- Compartilhamento de texto e arquivo `.txt`.
- Compartilhamento de PDF basico.
- Mensagem por token.
- Template de mensagem configuravel em Configuracoes.
- WhatsApp direto usando telefone do cliente.
- Botoes de mensagem na tela: WhatsApp, SMS e Email.
- Historico por registro.
- Detalhe do orcamento com itens e historico.

### Ordens de Servico

- Criacao com cliente, itens, status, observacoes e total.
- Edicao de cliente, itens, status e observacoes.
- Remocao de itens durante criacao/edicao.
- OS abre em tela de edicao propria ao clicar em uma lista ou resultado de busca.
- OS concluida/cancelada tambem pode ser reaberta/editada se o usuario alterar o status.
- Cliente em seletor pesquisavel.
- Servico/produto em seletor pesquisavel com resultados abaixo da busca.
- Status: Aberta, Em andamento, Concluida, Cancelada.
- Status no formulario em botoes compactos, com o selecionado destacado.
- Alteracao de status.
- Ao alterar status de uma OS em edicao, o app pergunta se deseja avisar o cliente por WhatsApp, SMS, Email ou cancelar.
- Data de conclusao quando status vira Concluida.
- Documento em texto.
- Compartilhamento de texto e arquivo `.txt`.
- Compartilhamento de PDF basico.
- Mensagem por token.
- Template de mensagem configuravel em Configuracoes.
- WhatsApp direto usando telefone do cliente.
- Botoes de mensagem na tela: WhatsApp, SMS e Email.
- Historico por registro.
- Detalhe da OS com itens e historico.
- Historico aparece abaixo do formulario ao editar uma OS.
- Alteracao de status registra origem e destino, exemplo: `Aberta -> Concluida`.

### Auditoria

- Tela geral de auditoria.
- Historico por orcamento e OS.
- Registros para criacao, status, conversao e arquivamentos principais.

### Backup

- Exportacao JSON.
- Exportacao JSON com IDs, datas, observacoes e vinculos.
- Compartilhamento como texto.
- Compartilhamento como arquivo `os-mobile-backup.json`.
- Restauracao por JSON colado na tela de Backup.
- Restauracao substitui dados operacionais e preserva configuracoes/auditoria.

### Configuracoes

- Ativar/desativar modulos.
- CPF/CNPJ: nao usar, opcional ou obrigatorio.
- Conta/agenda Android para salvar contatos sincronizados.
- Lista de agendas/contas de contatos disponiveis no aparelho para selecao.
- Nome da empresa.
- Templates editaveis para mensagens de OS e orcamento.
- Tokens disponiveis: `{nome}`, `{telefone}`, `{cpf}`, `{os}`, `{orcamento}`, `{valor}`, `{status}`, `{empresa}`, `{data}`.

### Navegacao/UI

- Menu lateral via drawer.
- Menu possui `Nova OS` e `Lista de OS` separados.
- `Nova OS` nao lista mais todas as OS no rodape.
- `Lista de OS` mostra OS em ordem decrescente com opcao `Ver mais`.
- Icone proprio simples.
- Barra inferior removida.

### Release

- APK release assinado localmente.
- AAB release assinado localmente.
- Assinatura configurada por `keystore.properties`.
- Keystore local em `keystore/osmobile-release.jks`.

## Pontos pendentes principais

0. Refatoracao incremental: ver `PLANO_REFACTORING.md` (testes criticos primeiro, depois divisao dos arquivos grandes de OS).

1. UI/UX:
   - melhorar botoes;
   - reduzir poluicao visual;
   - telas de detalhe mais completas;
   - estados vazios melhores;
   - possivel uso de icones.

2. Release:
   - subir AAB no Play Console em Teste interno;
   - configurar versionamento para proximas versoes;
   - testar instalacao limpa em aparelho real.

3. Testes:
   - testes Room/repository;
   - testes de conversao orcamento -> OS;
   - testes de backup/exportacao/restauracao.

4. PDF:
   - melhorar layout visual do PDF;
   - avaliar impressao direta/Bluetooth.

5. Agenda/Contatos:
   - validar em aparelho real com conta Google configurada;
   - melhorar mensagens de erro quando a conta nao aceitar insercao.

## Arquivos-chave

- `app/src/main/java/br/com/sos/osmobile/ui/OSMobileApp.kt`
- `app/src/main/java/br/com/sos/osmobile/core/database/AppDatabase.kt`
- `app/src/main/java/br/com/sos/osmobile/core/di/AppContainer.kt`
- `app/src/main/java/br/com/sos/osmobile/feature/dashboard/`
- `app/src/main/java/br/com/sos/osmobile/feature/customers/`
- `app/src/main/java/br/com/sos/osmobile/feature/services/`
- `app/src/main/java/br/com/sos/osmobile/feature/quotes/`
- `app/src/main/java/br/com/sos/osmobile/feature/workorders/`
- `app/src/main/java/br/com/sos/osmobile/feature/details/`
- `app/src/main/java/br/com/sos/osmobile/feature/backup/`
- `app/src/main/java/br/com/sos/osmobile/data/repository/`
- `app/src/main/java/br/com/sos/osmobile/data/local/dao/`
- `app/src/main/java/br/com/sos/osmobile/data/local/entity/`

## Roteiro de teste no celular

Ver tambem:

```text
TESTE_CELULAR.md
```

Fluxo minimo:

1. Cadastrar cliente.
2. Cadastrar servico/produto.
3. Criar orcamento aprovado.
4. Converter orcamento em OS.
5. Buscar cliente no Painel.
6. Abrir detalhe do cliente.
7. Abrir OS vinculada.
8. Confirmar que a OS abre em tela de edicao.
9. Alterar status e salvar.
10. Confirmar historico abaixo do formulario.
11. Gerar documento.
12. Gerar mensagem.
13. Abrir WhatsApp/SMS/Email.
14. Gerar backup JSON.
15. Em Clientes, tocar em Agenda e validar contato criado no app Contatos.

## Proximo passo recomendado

Testar em aparelho real com o APK release assinado e, em seguida, subir o AAB no Play Console em Teste interno.
