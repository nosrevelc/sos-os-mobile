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
8. Ir em `Lista de OS` e tocar na OS criada.
9. Confirmar que abriu a tela de edicao da OS.
10. Alterar o status usando os botoes e salvar.
11. Confirmar que o historico aparece abaixo do formulario e mostra a mudanca de status.
12. Tocar em `Documento` e depois `Compartilhar documento`.
13. Tocar em `Mensagem` e depois `Compartilhar mensagem`.
14. Ir em `Backup`, gerar e compartilhar o JSON.
15. Ir em `Configuracoes`, tocar em `Buscar agendas do aparelho` e escolher a agenda Google.
16. Ir em `Clientes`, tocar em `Agenda` e permitir acesso aos contatos.
17. Abrir o app Contatos do Android e confirmar se o cliente foi criado.

Pontos a observar:

- Se o app abre sem crash.
- Se os dados permanecem apos fechar e abrir o app.
- Se a conversao de orcamento para OS funciona.
- Se a `Lista de OS` abre a OS em modo edicao.
- Se a busca por cliente no Painel mostra as OS do cliente em ordem decrescente.
- Se a alteracao de status aparece no historico da OS.
- Se compartilhamento abre a tela nativa do Android.
- Se o backup JSON aparece com os dados cadastrados.
- Se o botao `Agenda` cria ou atualiza o contato corretamente.
