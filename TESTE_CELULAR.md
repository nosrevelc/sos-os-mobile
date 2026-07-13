# Teste rapido no celular

Atualizado em: 13/07/2026

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
11. Ao alterar o status, confirmar se aparece a pergunta para enviar mensagem por WhatsApp, SMS, Email ou cancelar.
12. Testar WhatsApp e confirmar que abre no numero do cliente.
13. Confirmar que o historico aparece abaixo do formulario e mostra a mudanca de status.
14. Tocar em `Documento` e depois `Compartilhar documento`.
15. Usar os botoes `WhatsApp`, `SMS` e `Email` na tela de OS.
16. Usar os botoes `WhatsApp`, `SMS` e `Email` na tela de Orcamento.
17. Ir em `Backup`, gerar e compartilhar o JSON.
18. Ir em `Configuracoes`, tocar em `Buscar agendas do aparelho` e escolher a agenda/conta desejada.
19. Em `Configuracoes`, alterar os templates de OS/orcamento e salvar.
20. Criar/editar OS e orcamento e confirmar se a mensagem usa o template alterado.
21. Ir em `Clientes`, tocar em `Agenda` e permitir acesso aos contatos.
22. Abrir o app Contatos do Android e confirmar se o cliente foi criado.
23. Em `Configuracoes`, ativar `Fotos`, `Assinatura`, `Checklist`, `Garantia` e `Financeiro`.
24. Abrir uma OS salva e adicionar uma foto.
25. Abrir a foto adicionada e depois remover a foto.
26. Desenhar uma assinatura, salvar, abrir e remover.
27. Adicionar itens no checklist, marcar/desmarcar e remover.
28. Salvar uma garantia com prazo e termos.
29. Imprimir OS, etiqueta, recibo e garantia se a impressora Bluetooth estiver configurada e vias > 0.
30. Abrir um orcamento salvo e testar `Imprimir orcamento` se a impressora Bluetooth estiver configurada e vias > 0.
31. Registrar pagamento em Financeiro, confirmar total pago/saldo e remover pagamento.
32. Gerar backup completo e confirmar que fotos/assinaturas entram no JSON.
33. Restaurar backup completo e conferir dados, fotos, assinatura, checklist, garantia e pagamentos.

Pontos a observar:

- Se o app abre sem crash.
- Se os dados permanecem apos fechar e abrir o app.
- Se a conversao de orcamento para OS funciona.
- Se a `Lista de OS` abre a OS em modo edicao.
- Se a busca por cliente no Painel mostra as OS do cliente em ordem decrescente.
- Se a alteracao de status aparece no historico da OS.
- Se ao alterar status da OS aparece a pergunta de envio WhatsApp/SMS/Email.
- Se os botoes WhatsApp/SMS/Email aparecem em OS e Orcamento.
- Se os templates configurados aparecem nas mensagens.
- Se compartilhamento abre a tela nativa do Android.
- Se o backup JSON aparece com os dados cadastrados.
- Se o botao `Agenda` cria ou atualiza o contato corretamente.
- Se ao cadastrar cliente novo o app pergunta se deseja adicionar na agenda.
- Se contato ja existente gera aviso e nao duplica.
- Se os modulos ativados aparecem na tela de OS.
- Se fotos e assinaturas continuam abrindo apos fechar e abrir o app.
- Se o backup restaurado recupera arquivos de fotos e assinaturas.
- Se as impressoes Bluetooth saem direto na impressora selecionada, sem tela de PDF.
- Se o financeiro calcula pago e saldo corretamente.
