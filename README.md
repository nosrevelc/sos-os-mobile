# OS Mobile

Aplicativo Android nativo para gestao offline-first de ordens de servico, orcamentos e clientes, voltado para prestadores de servico e pequenas empresas (ex.: topografia, estampa, assistencia tecnica).

- **Pacote:** `br.com.sos.osmobile`
- **Versao atual:** `0.1.0` (versionCode 1)
- **Status:** MVP operacional, em validacao em aparelho real

## Funcionalidades

### Painel
- Dashboard com indicadores.
- Busca global por cliente, telefone, OS, orcamento e status, com resultados clicaveis.

### Clientes
- Cadastro, edicao, busca e arquivamento logico.
- Validacao de CPF/CNPJ conforme configuracao (nao usar / opcional / obrigatorio) com tratamento de duplicidade.
- Detalhe do cliente com OS e orcamentos vinculados (clicaveis).
- Sincronizacao manual do cliente para a agenda Android/Google, com deteccao de contato existente.
- Permissao de contatos solicitada somente ao usar a acao de agenda.

### Servicos/Produtos
- Cadastro, edicao, busca e arquivamento logico.
- Codigo automatico no formato `SP-0001`, com tratamento de duplicado.

### Orcamentos
- Criacao/edicao com cliente, itens, status, observacoes e total.
- Status: Pendente, Aprovado, Rejeitado, Convertido.
- Conversao de orcamento aprovado em OS (orcamento convertido fica bloqueado).
- Seletores pesquisaveis de cliente e de servico/produto.
- Documento em texto e PDF basico; compartilhamento como texto ou arquivo `.txt`.
- Mensagens por WhatsApp, SMS e Email com template configuravel por tokens.
- Historico por registro e detalhe completo.

### Ordens de Servico
- Criacao/edicao com cliente, itens, status, observacoes e total.
- Status: Aberta, Em andamento, Concluida, Cancelada (com reabertura).
- Ao alterar status, o app pergunta se deseja avisar o cliente por WhatsApp, SMS ou Email.
- Data de conclusao automatica quando status vira `Concluida`.
- Historico de mudancas com origem e destino (ex.: `Aberta -> Concluida`).
- Documento em texto/PDF, compartilhamento e impressao nativa Android.
- Impressao de etiqueta da OS ao criar a ordem (manual ou automatica).

### Agenda
- Modulo de agendamentos integrado ao fluxo de OS.

### Auditoria
- Tela geral de auditoria e historico por orcamento/OS.
- Registros de criacao, mudanca de status, conversao e arquivamentos.

### Backup
- Exportacao JSON completa (IDs, datas, observacoes e vinculos).
- Compartilhamento como texto ou arquivo `os-mobile-backup.json`.
- Restauracao por JSON colado na tela de Backup (preserva configuracoes/auditoria).
- Integracao com Google Drive (documentada em `Projeto Makrdow/21-Backup-Google-Drive.md`).

### Configuracoes
- Ativar/desativar modulos.
- Politica de CPF/CNPJ.
- Nome da empresa e conta/agenda Android para contatos.
- Templates editaveis de mensagem com tokens: `{nome}`, `{telefone}`, `{cpf}`, `{os}`, `{orcamento}`, `{valor}`, `{status}`, `{empresa}`, `{data}`.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Room/SQLite (fonte oficial local, offline-first)
- MVVM com repositories
- Navigation Compose
- KSP, Gradle Wrapper, JDK 17
- ZXing (etiquetas/codigo de barras)

## Requisitos

- JDK 17
- Android SDK (compileSdk 35, minSdk 26, targetSdk 35)
- Android Studio ou `ANDROID_HOME` configurado

## Como buildar

```bash
# Testes + APK debug
./gradlew test assembleDebug

# APK release assinado (requer keystore.properties + keystore/)
./gradlew assembleRelease

# AAB para Play Console
./gradlew bundleRelease
```

Artefatos gerados:

| Artefato | Caminho |
| --- | --- |
| APK debug | `app/build/outputs/apk/debug/app-debug.apk` |
| APK release | `app/build/outputs/apk/release/app-release.apk` |
| AAB release | `app/build/outputs/bundle/release/app-release.aab` |

A assinatura release usa `keystore.properties` (nao versionado) apontando para `keystore/osmobile-release.jks` (nao versionado). Veja `keystore.properties.example` se disponivel ou crie:

```properties
storeFile=keystore/osmobile-release.jks
storePassword=***
keyAlias=***
keyPassword=***
```

## Estrutura do projeto

```text
app/src/main/java/br/com/sos/osmobile/
├── core/
│   ├── database/        # AppDatabase (Room)
│   ├── di/              # AppContainer (injecao manual)
│   └── time/
├── data/
│   ├── backup/          # Exportacao/restauracao JSON
│   ├── document/        # Geracao de documentos
│   ├── drive/           # Integracao Google Drive
│   ├── local/dao/       # DAOs Room
│   ├── local/entity/    # Entidades Room
│   ├── message/         # Templates e mensagens
│   ├── model/           # Modelos de dominio
│   ├── print/           # Impressao
│   └── repository/      # Repositorios
├── feature/
│   ├── appointments/    # Agenda
│   ├── audit/           # Auditoria
│   ├── backup/          # Tela de backup
│   ├── customers/       # Clientes
│   ├── dashboard/       # Painel
│   ├── details/         # Telas de detalhe
│   ├── finance/         # Financeiro (em evolucao)
│   ├── messages/        # Mensagens
│   ├── quotes/          # Orcamentos
│   ├── reports/         # Relatorios
│   ├── sales/           # Vendas
│   ├── services/        # Servicos/produtos
│   ├── settings/        # Configuracoes
│   └── workorders/      # Ordens de servico
└── ui/                  # Shell do app, navegacao e tema
```

## Documentacao

| Documento | Conteudo |
| --- | --- |
| [`ESTADO_DO_PROJETO.md`](ESTADO_DO_PROJETO.md) | Estado atual, modulos implementados e proximos passos |
| [`O_QUE_FALTA.md`](O_QUE_FALTA.md) | Pendencias detalhadas e escopo futuro |
| [`TESTE_CELULAR.md`](TESTE_CELULAR.md) | Roteiro de teste manual em aparelho real |
| [`ENTREGA_RETIRADA.md`](ENTREGA_RETIRADA.md) | Especificacao do modulo entrega/retirada |
| [`LICENCIAMENTO_E_MODELO_COMERCIAL.md`](LICENCIAMENTO_E_MODELO_COMERCIAL.md) | Licenciamento e modelo comercial |
| [`BUILD_DEBUG_PENDENTE_WINDOWS.md`](BUILD_DEBUG_PENDENTE_WINDOWS.md) | Pendencia de build debug em ambiente Windows |
| [`Projeto Makrdow/`](Projeto%20Makrdow/) | Especificacoes originais, requisitos e decisoes de arquitetura |

## Modelos CSV

Modelos de importacao na raiz do repositorio:

- `modelo_clientes.csv`
- `modelo_servicos_produtos.csv`
- `modelo_configuracoes.csv`
- `modelo_templates_mensagens.csv`

## Roadmap resumido

1. Validar agenda/contatos e instalacao limpa em aparelho real.
2. Subir AAB no Play Console (Teste interno) e definir versionamento.
3. Melhorar UI/UX (botoes, estados vazios, detalhes) e layout do PDF.
4. Impressao direta Bluetooth 58 mm (recibo/garantia).
5. Testes automatizados: Room/repository, conversao orcamento -> OS, backup/restauracao.

Detalhes completos em [`O_QUE_FALTA.md`](O_QUE_FALTA.md).

## Licenca

Uso comercial controlado pelo proprietario do projeto. Veja [`LICENCIAMENTO_E_MODELO_COMERCIAL.md`](LICENCIAMENTO_E_MODELO_COMERCIAL.md).
