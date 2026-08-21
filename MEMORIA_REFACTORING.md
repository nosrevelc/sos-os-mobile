# Memoria de Sessao - Refatoracao OS Mobile

> Arquivo de memoria para continuidade entre sessoes. Atualize a cada etapa concluida.
> Ultima atualizacao: pos-refatoracao, 91 testes (CsvSupport, Formatters e controllers de OS
> Form/Print/Message/Attachment). Faltam: WorkOrderDriveController (SAF) e UI Compose.
> Correcoes de producao dos testes anteriores: parseCsv desescapa `\"`/`\\`, cabecalho BOM+espaco,
> blankToNull trima, decodeCsvEscapedLines normaliza CR cru, dateTimeShort padrao fixo `dd/MM/yy HH:mm`.

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
| Fase 4 - Dedup Orcamento x OS | CONCLUIDA | ver git log |
| Fase 5.1 SettingsScreen dividida | CONCLUIDA (1073->550 linhas) | ver git log |
| Fase 5.2 DriveSafClient extraido | CONCLUIDA (845->659 + 205) | ver git log |
| Fase 5.3 CsvSupport + DriveBackupStorage | CONCLUIDA (747->577 + 97 + 92) | ver git log |
| Fase 6 - CI GitHub Actions (.github/workflows/android.yml) | CONCLUIDA | `844770a` |
| CI: fix java home do Windows no workflow | CONCLUIDA (run #2 verde) | `b1c81e6` |
| APK debug gerado e validado no celular | CONCLUIDA (uninstall -> install -> restore Drive OK) | - |

Build/testes: VERDE (`test assembleDebug`) apos cada etapa. CI remoto: VERDE.

## Validacao em dispositivo (ago/2026)

- APK debug (`app/build/outputs/apk/debug/app-debug.apk`, 18 MB) instalado pelo proprietario.
- Assinatura debug desta maquina difere da do Windows: install por cima falha mesmo entre debugs.
  Caminho que funcionou: desinstalar app antigo -> instalar novo -> restaurar backup do Google Drive.
- Refatoracao NAO alterou schema do banco nem formato de backup; restore compativel.
- Proprietario confirmou funcionamento apos restauracao.

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

Testes: `app/src/test/...` — 91 no total. Controllers de OS: WorkOrderFormControllerTest(11),
WorkOrderAttachmentControllerTest(6), WorkOrderPrintControllerTest(3), WorkOrderMessageControllerTest(2).
CsvSupportTest(13), FormattersTest(5), repos/backup/renderizadores/validadores. Todos verdes.

## Pos-refatoracao: novos testes e correcoes (ago/2026)

- CsvSupportTest + FormattersTest criados (18 testes); 5 falhas revelaram bugs reais de producao.
- parseCsv: agora desescapa `\"` e `\\` dentro de campos citados (round-trip com `esc()` fechado).
- Cabecalho CSV: `removePrefix(BOM)` antes do `trim()` — BOM seguido de espaco quebrava a chave.
- blankToNull trima antes de decidir nulo (chamadores ja recebem valores trimados; sem mudanca visivel).
- decodeCsvEscapedLines normaliza tambem CR/CRLF crus, nao so sequencias escapadas.
- Formatters.dateTimeShort usava DateFormat.getDateTimeInstance (dependia do locale da JVM;
  nesta maquina pt_PT sai `14/11/23, 19:13`, no CI en_US sai com AM/PM). Agora padrao fixo
  `dd/MM/yy HH:mm`, igual ao estilo de `dateTime()`. Teste de data/hora depende de locale = armadilha nova.

### Armadilha nova: controllers + Room em teste

- Controllers lancam corrotinas em `session.scope`; com executor padrao do Room o teste
  asserta antes do termino (e corrotinas orfas viram `UncaughtExceptionsBeforeTest` no teste seguinte).
- Solucao em `TestFixtures.inMemoryDatabase()`: `setQueryExecutor { it.run() }` +
  `setTransactionExecutor { it.run() }` (executor direto) — launches rodam sincronos com
  `CoroutineScope(UnconfinedTestDispatcher())`.
- `saveWorkOrderThen(onSaved: () -> Unit)` nao passa id; usar `session.formState.editingId` apos salvar.

## Proximos passos (opcionais)

- Limpeza de imports nao usados nos arquivos extraidos das Fases 3-5 (so warnings hoje).
- Gerar `assembleRelease` quando o proprietario solicitar (keystore em `keystore/osmobile-release.jks` + `keystore.properties`).
- Considerar remover `org.gradle.java.home` do `gradle.properties` se um dia compilar fora do Windows.

## Armadilhas aprendidas (nao repetir)

- Moeda pt-BR usa NBSP (U+00A0): normalizar com `.replace('\u00A0', ' ')` em asserts de teste.
- Extrair codigo Kotlin via script: NUNCA balancear chaves (templates `"{}"` e funcoes expression-body `= ...`
  quebram a contagem). Preferir ancoras coluna-0 (`^fun`, `^@Composable`) + contagem de parenteses da assinatura.
- Apos mover declaracoes `private`, torná-las `internal` (mesmo pacote) ou o arquivo origem nao compila.
- `ClientMessage` virou `internal data class` em WorkOrderSignature.kt (usada pelo Screen).
- Commits em pt sem acentos, um commit por tarefa, push ao final de cada fase.

## Pendencias de git

- Nenhuma. Tudo commitado e enviado a `origin/master` (ultimo push: `b1c81e6`).
