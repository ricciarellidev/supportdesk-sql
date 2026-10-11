# Verificação da entrega

Data: **11/10/2026**.

## Resultado local

| Verificação                        | Resultado                             |
| ---------------------------------- | ------------------------------------- |
| Compilação Java 17 e empacotamento | Concluídos                            |
| Testes unitários                   | 24 aprovados                          |
| Testes HTTP e banco                | 35 aprovados                          |
| Falhas, erros e testes ignorados   | 0                                     |
| Caderno SQL                        | 12 arquivos executados nos testes     |
| Contrato OpenAPI 3.1               | Validado por `openapi-spec-validator` |

Os números foram extraídos dos relatórios Surefire/Failsafe; o [resumo JSON](verification/test-summary.json) guarda as contagens por suíte. A execução incluiu `clean verify`.

JaCoCo mediu **419 de 425 linhas cobertas (98,59%)** e **62 de 70 ramificações (88,57%)**. Não foram configuradas exclusões de classes do projeto. Cobertura não comprova ausência de falhas nem substitui testes do ambiente final.

## Painel e revisão visual

O painel foi conferido em Chromium com a API e o mecanismo de banco embutido em execução. A revisão cobriu tela de computador (1440 × 1050) e celular (390 × 844), consulta de detalhes, paginação, busca, filtros, estado vazio e acesso ao contrato OpenAPI. Não houve erros de JavaScript no console nem transbordamento horizontal do documento no celular. A tabela usa sua própria rolagem horizontal. O [resumo da interface](verification/ui-summary.json) registra esses checks.

## Ambiente e limites

Java 17.0.20, Spring Boot 4.1.1 e o mecanismo PostgreSQL 18.3 fornecido por PGlite 0.5.8. PGlite executa PostgreSQL compilado para WebAssembly; não é H2 nem um mock de respostas SQL.

O ambiente não oferece Docker e mapeia somente o usuário root, impedindo a inicialização normal do PostgreSQL nativo. A verificação alternativa carregou o SQL de `V1` e `V2` diretamente no mecanismo, usou JDBC pelo protocolo simples e uma conexão. Isso permitiu executar os testes de consultas, restrições, HTTP e rollback.

**A inicialização pelo Flyway foi desativada somente nessa execução alternativa.** Seu gerenciamento de migrações, a imagem PostgreSQL 17.11, o Compose e o fluxo GitHub Actions não foram executados nesta entrega. A configuração distribuída mantém o Flyway ativo e os testes de integração obrigatórios.

Essa diferença de versão e execução precisa ser respeitada: o resultado local não certifica PostgreSQL 17 nativo, concorrência entre múltiplas conexões, desempenho em produção ou uma execução remota de CI.

## Validação no ambiente previsto

Na raiz do projeto:

```bash
docker compose up --build -d
docker compose --profile test run --rm tests
```

Esse caminho usa PostgreSQL 17.11 nativo em Docker e aplica as migrações pelo Flyway. O GitHub Actions configura o mesmo banco dedicado e executa `clean verify`.

Na publicação, confira o resultado real do fluxo e registre o link da execução. Um arquivo de workflow ou um teste local aprovado não equivale a CI remoto aprovado.

## Integridade do pacote

`FILES.sha256` lista o SHA-256 de todos os arquivos entregues, exceto o próprio manifesto. O ZIP preserva as pastas ocultas de configuração e o modo executável de `mvnw`.
