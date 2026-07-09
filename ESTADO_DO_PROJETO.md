# Estado do Projeto - OS Mobile

## Situacao atual

Projeto Android nativo em Kotlin/Jetpack Compose, offline-first, usando Room/SQLite.

Build validado:

```powershell
.\gradlew.bat test assembleDebug
```

Resultado atual esperado: `BUILD SUCCESSFUL`.

APK para teste:

```text
C:\SOS\app\build\outputs\apk\debug\app-debug.apk
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
- Cliente em dropdown.
- Servico/produto em dropdown.
- Status: Pendente, Aprovado, Rejeitado, Convertido.
- Alteracao de status.
- Conversao de orcamento aprovado em OS.
- Documento em texto.
- Compartilhamento de texto e arquivo `.txt`.
- Compartilhamento de PDF basico.
- Mensagem por token.
- WhatsApp direto usando telefone do cliente.
- Historico por registro.
- Detalhe do orcamento com itens e historico.

### Ordens de Servico

- Criacao com cliente, itens, status, observacoes e total.
- Edicao de cliente, itens, status e observacoes.
- Remocao de itens durante criacao/edicao.
- OS concluida ou cancelada fica bloqueada para edicao.
- Cliente em dropdown.
- Servico/produto em dropdown.
- Status: Aberta, Em andamento, Concluida, Cancelada.
- Alteracao de status.
- Data de conclusao quando status vira Concluida.
- Documento em texto.
- Compartilhamento de texto e arquivo `.txt`.
- Compartilhamento de PDF basico.
- Mensagem por token.
- WhatsApp direto usando telefone do cliente.
- Historico por registro.
- Detalhe da OS com itens e historico.

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

### Navegacao/UI

- Menu lateral via drawer.
- Icone proprio simples.
- Barra inferior removida.

## Pontos pendentes principais

1. UI/UX:
   - melhorar botoes;
   - reduzir poluicao visual;
   - telas de detalhe mais completas;
   - estados vazios melhores;
   - possivel uso de icones.

2. Release:
   - gerar APK/AAB assinado;
   - configurar versionamento;
   - testar instalacao limpa em aparelho real.

3. Testes:
   - testes Room/repository;
   - testes de conversao orcamento -> OS;
   - testes de backup/exportacao/restauracao.

4. PDF:
   - melhorar layout visual do PDF;
   - avaliar impressao direta/Bluetooth.

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
8. Gerar documento.
9. Gerar mensagem.
10. Abrir WhatsApp.
11. Gerar backup JSON.

## Proximo passo recomendado

Fazer polimento final de UI/UX e teste em aparelho real com o APK debug.
