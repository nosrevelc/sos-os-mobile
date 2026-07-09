# Teste rapido no celular

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Fluxo minimo:

1. Instalar o APK no Android.
2. Abrir o app em `Painel`.
3. Ir em `Clientes` e cadastrar um cliente.
4. Ir em `Servicos` e cadastrar um servico/produto.
5. Ir em `Orcamentos`, criar um orcamento com status `Aprovado`.
6. Na lista de orcamentos, tocar em `Converter em OS`.
7. Ir em `OS` e confirmar que a OS foi criada.
8. Tocar em `Documento` e depois `Compartilhar documento`.
9. Tocar em `Mensagem` e depois `Compartilhar mensagem`.
10. Ir em `Backup`, gerar e compartilhar o JSON.

Pontos a observar:

- Se o app abre sem crash.
- Se os dados permanecem apos fechar e abrir o app.
- Se a conversao de orcamento para OS funciona.
- Se compartilhamento abre a tela nativa do Android.
- Se o backup JSON aparece com os dados cadastrados.
