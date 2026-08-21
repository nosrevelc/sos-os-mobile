# Memoria de Sessao - Refatoracao OS Mobile

> Arquivo de memoria para continuidade entre sessoes. Atualize a cada etapa concluida.
> Ultima atualizacao: Fase 3 (split do WorkOrderScreen) concluida.

## Como buildar nesta maquina (Linux)

O `gradle.properties` tem `org.gradle.java.home` apontando para um caminho do Windows.
SEMPRE sobrescrever pela linha de comando:

```bash
export JAVA_HOME=~/tools/jdk17
export ANDROID_HOME=~/tools/android-sdk
./gradlew test assembleDebug -Dorg.gradle.java.home=$HOME/tools/jdk17 --console=plain
```

- JDK 17 Temurin: `~/tools/jdk17`
- Android SDK (platform 35 + build-tools 35.0.0): `~/tools/android-sdk`
- `local.properties`: `sdk.dir=/home/cleverson/tools/android-sdk`
- Proibido: `assembleRelease` / `bundleRelease` sem pedido expresso do proprietario.

## Estado atual

Branch `master`, remote `origin` = github.com/nosrevelc/sos-os-mobile.

| Etapa | Status | Commit |
| --- | --- | --- |
| Docs GitHub (README, ESTADO, O_QUE_FALTA) | CONCLUIDA | `450b6f0` |
| PLANO_REFACTORING.md criado | CONCLUIDA | `5eaa557` |
| Fase 0 - testes criticos (16 testes) | CONCLUIDA | `50b5551`, `181a6ee`, `51d13c2` |
| Fase 1.1 DatabaseMigrations + AppContainer enxuto | CONCLUIDA | local |
| Fase 1.2 Formatters unificados | CONCLUIDA | local |
| Fase 1.3 WorkOrderMessageRenderer | CONCLUIDA | local |
| Fase 1.4 AttachmentNames | CONCLUIDA | local |
| Fase 1.5 DriveSyncStatus em ui/components | CONCLUIDA | local |
| Fase 2 - WorkOrderViewModel em controllers | CONCLUIDA | local |
| Fase 3 - Split do WorkOrderScreen | CONCLUIDA | `694aa84` |
| Memoria de sessao | criada | `715d8e4` |
| Fase 4 - Dedup Orcamento x OS (CustomerSection, DocumentItemsEditor, StatusSelectorCompact, itemTokensOf) | CONCLUIDA | local |

Build/testes: VERDE (`test assembleDebug`) apos cada etapa.

## Estrutura atual (feature/workorders)

```text
WorkOrderViewModel.kt    ~440 linhas  fachada + uiState + factory
WorkOrderModels.kt        ~78         WorkOrderDraftItem/FormState/UiState
WorkOrderSessionState.kt  ~31         estados mutaveis compartilhados + scope
WorkOrderFormController   ~319        campos, itens, save/edit/status/stock
WorkOrderPrintController  ~80         documentos A4 + termicos
WorkOrderMessageControl   ~49         showMessage por status/template
WorkOrderAttachmentCtrl   ~196        fotos/assinatura/checklist/garantia/pagamentos
WorkOrderDriveController  ~120        sync/import design/prompt/debug
WorkOrderScreen.kt       ~1432        orquestracao + WorkOrderForm + dialogs
WorkOrderListScreen.kt    ~377        lista + pickup + rows
WorkOrderFormSections.kt  ~271        selectors (status/pagamento/entrega) + DraftItemRow
WorkOrderSignature.kt     ~379        SignatureCapture/Pad/bitmap + ClientMessage(data class internal)
```

Extras das fases 0-1: `core/database/DatabaseMigrations.kt` (`ALL_MIGRATIONS`), `core/format/Formatters.kt`,
`data/message/WorkOrderMessageRenderer.kt`, `data/local/AttachmentNames.kt`,
`ui/components/DriveSyncStatus.kt`, `WorkOrderStockTotals.kt`.

Testes: `app/src/test/...` — TestFixtures, QuoteConversionRepositoryTest(5), WorkOrderRepositoryTest(8),
BackupRepositoryTest(3), WorkOrderStockTotalsTest(5), WorkOrderMessageRendererTest(5). Todos verdes.

## Proximos passos (ordem)

1. **Fase 4** - Deduplicacao Orcamento x OS:
   - 4.1 `DocumentItemsEditor` (lista de itens + add/remove reutilizavel)
   - 4.2 `CustomerSection` (seletor de cliente)
   - 4.3 Reaproveitar `WorkOrderMessageRenderer.ItemData` no Orcamento
   - 4.4 Unificar `StatusSelector` compacto
2. **Fase 5**: 5.1 dividir SettingsScreen; 5.2 dividir DriveSyncRepository; 5.3 dividir BackupRepository.
3. **Fase 6** - CI GitHub Actions (`.github/workflows/android.yml`: test + assembleDebug, JDK 17, SDK 35).
4. Limpeza final: imports nao usados nos arquivos extraidos da Fase 3 (so warnings hoje).

## Armadilhas aprendidas (nao repetir)

- Moeda pt-BR usa NBSP (U+00A0): normalizar com `.replace('\u00A0', ' ')` em asserts de teste.
- Extrair codigo Kotlin via script: NUNCA balancear chaves (templates `"{}"` e funcoes expression-body `= ...`
  quebram a contagem). Preferir ancoras coluna-0 (`^fun`, `^@Composable`) + contagem de parenteses da assinatura.
- Apos mover declaracoes `private`, torná-las `internal` (mesmo pacote) ou o arquivo origem nao compila.
- `ClientMessage` virou `internal data class` em WorkOrderSignature.kt (usada pelo Screen).
- Commits em pt sem acentos, um commit por tarefa, push ao final de cada fase.

## Pendencias de git

- Fazer PUSH de todos os commits locais (Fases 1, 2 e 3) - ultimo push foi `51d13c2`.
