# OS Mobile

Aplicativo Android nativo para gestao offline-first de ordens de servico.

## Stack inicial

- Kotlin
- Jetpack Compose
- MVVM
- Room/SQLite como fonte oficial local
- Navigation Compose
- Repositorios por modulo

## Modulos do MVP

- Clientes
- Servicos e produtos
- Orcamentos
- Ordens de servico
- Configuracoes
- Auditoria

## Observacao de ambiente

Esta estrutura foi criada manualmente porque `gradle` nao esta instalado no PATH deste ambiente e nao ha Android SDK configurado em `ANDROID_HOME` ou `ANDROID_SDK_ROOT`.
