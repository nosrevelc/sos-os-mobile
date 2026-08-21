# Build debug pendente no Windows

## Instrucao para o proximo agente

Assim que este repositorio estiver aberto em um ambiente Windows com Java e Android SDK disponiveis, validar a alteracao do commit:

```text
9863e21 Imprime etiqueta ao criar ordem de servico
```

Executar, na raiz do repositorio:

```powershell
.\gradlew.bat test assembleDebug
```

Confirmar que o APK foi gerado em:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Validar no aplicativo que, ao criar uma nova OS:

1. A pergunta exibida seja `Imprimir etiqueta da OS?`.
2. O botao `Imprimir etiqueta` imprima a etiqueta, e nao o documento completo da OS.
3. Com impressao automatica ativada, seja impressa automaticamente a etiqueta.
4. A impressao manual do documento completo da OS continue disponivel durante a edicao.

## Restricao obrigatoria

Nao executar `assembleRelease`, `bundleRelease`, assinatura, publicacao ou qualquer outro procedimento de release sem solicitacao explicita do proprietario do projeto.
