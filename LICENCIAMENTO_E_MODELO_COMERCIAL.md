# Licenciamento e Modelo Comercial

Pesquisa baseada na documentacao oficial Android/Google Play em 14/07/2026.

## Modelos possiveis

### 1. Demo

Modo para demonstracao, sem compromisso comercial.

Recomendado:

- dados ficticios;
- limite de registros, por exemplo 5 clientes e 5 OS;
- bloquear backup/restauracao real;
- mostrar aviso discreto "Modo demonstracao";
- permitir testar fluxo completo sem valor juridico/comercial.

Nao recomendado:

- misturar dados demo com dados reais;
- deixar o usuario trabalhar por muito tempo no demo e depois perder tudo sem aviso.

### 2. Trial

Uso real por prazo limitado, por exemplo 7, 14 ou 30 dias.

Recomendado:

- liberar o sistema completo no trial;
- registrar data de ativacao;
- validar trial em servidor quando possivel;
- permitir periodo de tolerancia offline;
- avisar dias restantes.

Nao recomendado:

- controlar trial apenas pelo relogio local do celular;
- bloquear o usuario sem permitir backup/exportacao;
- prometer "gratis" se depois exigir pagamento sem aviso claro.

### 3. Licenca por aparelho

O cliente compra uma licenca e ativa em um ou mais aparelhos.

Recomendado:

- licenca vinculada a uma empresa/cliente;
- limite de aparelhos por licenca;
- cada aparelho recebe uma ativacao propria;
- permitir desativar aparelho antigo para trocar de celular;
- usar servidor para validar ativacoes.

Nao recomendado:

- usar IMEI, MAC, serial ou identificadores sensiveis;
- confiar apenas em uma chave local dentro do APK;
- impedir troca legitima de aparelho.

### 4. Assinatura

Modelo mensal/anual.

Recomendado:

- se distribuir pela Play Store, usar Google Play Billing para assinatura;
- ter plano mensal e anual;
- manter periodo de tolerancia para falha temporaria de pagamento;
- validar assinatura em servidor quando possivel.

### 5. Compra unica

Modelo simples para primeira venda.

Recomendado:

- pagamento unico por empresa;
- limite de aparelhos;
- suporte/atualizacoes por periodo definido;
- opcao futura de migrar para assinatura.

## Estrategia recomendada para OS Mobile

Fase 1:

- modo Trial de 15 dias;
- chave de ativacao manual;
- limite de aparelhos por chave;
- tela "Licenca" em Configuracoes;
- cache offline da licenca por alguns dias.

Fase 2:

- servidor simples de licencas;
- painel para gerar chaves;
- ativar/desativar aparelhos;
- historico de ativacoes.

Fase 3:

- Google Play Billing para assinatura;
- Play Integrity API para verificar app original e reduzir copia pirata;
- licenca por empresa sincronizada com backend.

## Identificacao de aparelho

O Android restringe identificadores permanentes. O correto e evitar IMEI, MAC e serial.

Opcao aceitavel:

- gerar um ID local do app;
- combinar com Android ID apenas em hash;
- enviar para servidor como "fingerprint" aproximado;
- aceitar que troca de aparelho, reset de fabrica ou mudanca de assinatura do app pode alterar o identificador.

Nunca depender 100% de identificador local para seguranca.

## Antipirataria

O que ajuda:

- validar licenca em servidor;
- assinar resposta da licenca;
- verificar integridade do app;
- ofuscar codigo;
- cache offline com expiracao;
- Play Integrity API quando estiver na Play Store.

O que nao resolve sozinho:

- chave fixa dentro do app;
- senha escondida no APK;
- bloquear por IMEI;
- trial baseado so em data local;
- APK com todas as regras de licenca sem servidor.

## Tela sugerida

Configuracoes > Licenca

Campos:

- Status: Trial / Ativado / Expirado / Demo
- Empresa licenciada
- Chave de ativacao
- ID deste aparelho
- Dias restantes
- Ultima validacao

Botoes:

- Ativar licenca
- Verificar licenca
- Desativar este aparelho
- Entrar em modo demo

## Fontes oficiais consultadas

- Google Play App Licensing: https://developer.android.com/google/play/licensing
- Licensing Overview: https://developer.android.com/google/play/licensing/overview
- Play Billing Subscriptions: https://developer.android.com/google/play/billing/subscriptions
- Test Play Billing: https://developer.android.com/google/play/billing/test
- Play Integrity API: https://developer.android.com/google/play/integrity/overview
- Android ID changes: https://android-developers.googleblog.com/2017/04/changes-to-device-identifiers-in.html
