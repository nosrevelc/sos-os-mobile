# Plano de Refatoracao - OS Mobile

Objetivo: reduzir o risco e o custo de evolucao do codigo sem reescrita, com passos pequenos, cada um validado por build e testes.

## Principios

1. Nenhuma mudanca de comportamento visivel ao usuario durante a refatoracao.
2. Cada etapa e um commit separado, validado com `./gradlew test assembleDebug` (no Windows: `.\gradlew.bat test assembleDebug`).
3. Testes primeiro nos caminhos criticos: sao a rede de seguranca das etapas seguintes.
4. Proibido executar `assembleRelease`, `bundleRelease` ou qualquer procedimento de release durante a refatoracao (ver `BUILD_DEBUG_PENDENTE_WINDOWS.md`).
5. Refatorar antes dos modulos novos (Financeiro, Estoque), porque eles vao tocar nos mesmos arquivos.

## Diagnostico atual

| Arquivo | Linhas | Problema |
| --- | --- | --- |
| `feature/workorders/WorkOrderScreen.kt` | 2196 | Composable monolitico com 44 funcoes: formulario, lista, pickup, assinatura, anexos, Drive, mensagens |
| `feature/workorders/WorkOrderViewModel.kt` | 1022 | God ViewModel com 65 funcoes: formulario, fotos, assinatura, checklist, garantia, pagamentos, impressao, Drive |
| `feature/settings/SettingsScreen.kt` | 1073 | Varias secoes em um arquivo |
| `data/drive/DriveSyncRepository.kt` | 845 | 33 funcoes: sincronizacao, importacao de design, relatorio de debug |
| `data/backup/BackupRepository.kt` | 747 | Exportacao e restauracao acopladas |
| `core/di/AppContainer.kt` | 349 | Migracoes SQL inline dentro do DI |
| Testes | 7 arquivos | Apenas validadores/renderizadores; nada de repositorio, conversao ou backup |

Duplicacao confirmada entre `QuoteViewModel` e `WorkOrderViewModel`: `selectCustomer`, `selectServiceProduct`, `onQuantityChanged`, `onUnitPriceChanged`, `onDiscountChanged`, `addSelectedItem`, `removeItem` e a logica de renderizacao de mensagem (`renderWorkOrderMessage` na Screen, tokens no ViewModel).

## Fase 0 - Rede de seguranca (testes dos caminhos criticos)

Prioridade maxima. Antes de mover qualquer codigo.

**Status: CONCLUIDA.** Ambiente de build Linux configurado (JDK 17 + SDK 35 em `~/tools`); `./gradlew test assembleDebug` verde localmente.

| # | Tarefa | Risco |
| --- | --- | --- |
| 0.1 | Adicionar dependencias de teste: `kotlinx-coroutines-test` e Robolectric (para Room in-memory sem aparelho) | Baixo |
| 0.2 | Teste de conversao orcamento -> OS (`QuoteConversionRepository`): itens, total, bloqueio do orcamento convertido, registro de auditoria | Medio |
| 0.3 | Teste de backup/restauracao (`BackupRepository`): roundtrip export -> restore preservando IDs, datas e vinculos; configuracoes/auditoria preservadas | Medio |
| 0.4 | Teste de repositorio de OS: criacao, mudanca de status com historico `origem -> destino`, data de conclusao | Medio |
| 0.5 | Teste de calculo de totais com controle de estoque (`stockControlledTotals`) apos extracao para classe pura | Baixo |

Validacao da fase: `./gradlew test` verde.

## Fase 1 - Extracoes de baixo risco

Nao muda API publica de nenhuma classe. Ganho imediato de legibilidade.

| # | Tarefa | De | Para |
| --- | --- | --- | --- |
| 1.1 | Migracoes Room 1->2 ... N | `AppContainer.kt` | `core/database/DatabaseMigrations.kt` (lista unica `ALL_MIGRATIONS`) |
| 1.2 | Formatadores duplicados (`formatCurrency`, `formatDate`, `formatQuantity`, `formatFileSize`) | Screens | `core/format/Formatters.kt` |
| 1.3 | Renderizacao de mensagem OS (tokens + texto) | `WorkOrderScreen.kt` + `WorkOrderViewModel.kt` | `data/message/WorkOrderMessageRenderer.kt` (classe pura, testavel) |
| 1.4 | Helpers de anexo (`attachmentOriginalName`, `isDocumentAttachment`, `isDesignAttachment`) | `WorkOrderScreen.kt` | `data/local/AttachmentNames.kt` |
| 1.5 | Status/texto/icone/cor do Drive (`driveStatusText`, `driveStatusIcon`, `driveStatusColor`) | `WorkOrderScreen.kt` | `ui/components/DriveSyncStatus.kt` |

## Fase 2 - Divisao do WorkOrderViewModel (1022 linhas)

Estrategia: extrair delegues por responsabilidade mantendo a fachada `WorkOrderViewModel` estavel, para nao obrigar mudanca grande na Screen na mesma etapa.

```text
feature/workorders/
├── WorkOrderViewModel.kt          # fachada: expoe estado e delega (fica ~200 linhas)
└── WorkOrderControllers.kt ou pacote controllers/
    ├── WorkOrderFormController        # rascunho, itens, save/edit/status
    ├── WorkOrderAttachmentController  # fotos, assinatura, checklist, garantia, pagamentos
    ├── WorkOrderPrintController       # documento, etiqueta, recibo, garantia termica
    └── WorkOrderDriveController       # sync, importacao de design, prompt, debug report
```

Regras:

- Cada controller recebe apenas os repositorios que usa.
- Migrar um controller por commit, rodando `test assembleDebug` a cada um.
- Ordem sugerida: Print -> Attachment -> Drive -> Form (o Form por ultimo, e o mais arriscado).

## Fase 3 - Divisao do WorkOrderScreen (2196 linhas)

Depois da Fase 2, dividir por secao visual. Cada arquivo novo e movimentacao de codigo existente, sem redesenho de UI.

```text
feature/workorders/
├── WorkOrderScreen.kt             # orquestracao (~400 linhas)
├── WorkOrderListScreen.kt         # lista + rows + pickup (ja semi-separado)
├── components/
│   ├── WorkOrderFormSections.kt   # itens, status, entrega, pagamento
│   ├── SignatureCapture.kt        # capture + pad + bitmap (~180 linhas coesas)
│   ├── AttachmentsSection.kt      # fotos/documentos/designs
│   └── SelectionButton.kt         # compartilhado com Orcamentos
└── ...
```

Meta: nenhum arquivo de UI acima de ~500 linhas.

## Fase 4 - Deduplicacao Orcamento x OS

So depois das Fases 2 e 3, quando os dois lados estiverem legiveis.

| # | Tarefa |
| --- | --- |
| 4.1 | Extrair `ItemDraftEditor` (composable de item: quantidade, preco, desconto, adicionar/remover) usado por Orcamento e OS |
| 4.2 | Extrair `ItemDraftState`/controller compartilhado para a logica de rascunho de itens |
| 4.3 | Unificar botoes de mensagem (WhatsApp/SMS/Email) em `ui/components/MessageActionsRow.kt` |
| 4.4 | Unificar `StatusSelector` compacto (botoes com selecionado destacado) |

## Fase 5 - Outros pontos quentes

| # | Tarefa | Meta |
| --- | --- | --- |
| 5.1 | Dividir `SettingsScreen.kt` por secao (Geral, Empresa, Mensagens, Agenda, Impressao, Backup) | < 300 linhas/arquivo |
| 5.2 | Dividir `DriveSyncRepository.kt`: engine de sync, importador de design, relatorio de debug | 3 classes coesas |
| 5.3 | Dividir `BackupRepository.kt`: exportador vs restaurador, com modelo de backup versionado | prepara backup na nuvem |
| 5.4 | Padronizar tratamento de erros dos repositorios (resultado explicito em vez de try/catch espalhado) | opcional |

## Fase 6 - CI no GitHub (opcional, recomendado)

O repositorio agora esta no GitHub; um workflow simples elimina a dependencia do Windows para validar cada passo:

```yaml
# .github/workflows/android.yml
# jobs: test + assembleDebug em ubuntu-latest com JDK 17 e SDK 35
```

Gatilho: push e pull_request em `master`.

## Fora de escopo por enquanto

Nao fazer nesta rodada:

- Hilt/Koin: o `AppContainer` manual ainda atende; reavaliar se o numero de repositorios dobrar.
- Multi-modulo Gradle: ganho baixo para o tamanho atual.
- Migracao de versoes de libs/Compose: separada da refatoracao.
- Redesign de UI/UX: refatoracao nao muda tela alguma.

## Ordem e tamanho das etapas

| Fase | Conteudo | Tamanho | Dependencia |
| --- | --- | --- | --- |
| 0 | Testes criticos | M | - |
| 1 | Extracoes simples | P | 0 |
| 2 | Split do WorkOrderViewModel | G | 0 |
| 3 | Split do WorkOrderScreen | M | 2 |
| 4 | Deduplicacao Orcamento/OS | M | 2, 3 |
| 5 | Settings, Drive, Backup | M | 0 |
| 6 | CI no GitHub | P | - |

Legenda: P = meio periodo, M = 1-2 dias, G = 2-4 dias (estimativa para o proprietario + agente).

## Critério de pronto por etapa

1. `./gradlew test assembleDebug` verde.
2. Fluxo minimo do `TESTE_CELULAR.md` continua funcionando (validacao manual no fim das Fases 2, 3 e 4).
3. Commit unico por tarefa, mensagem no padrao do repositorio.
