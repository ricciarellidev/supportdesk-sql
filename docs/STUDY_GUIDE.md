# Guia de estudo e apresentação

Um projeto de portfólio é evidência de prática. Execute e entenda os exemplos antes de descrever as competências no perfil.

## Roteiro

1. Suba a demonstração e consulte o chamado 1 no painel.
2. Leia a migração V1 e identifique as cinco tabelas e suas chaves.
3. Execute `01_backlog_join.sql` e compare com o painel.
4. Abra um chamado pela API e encontre os registros em `tickets` e `ticket_events`.
5. Atribua um agente, inicie, resolva e encerre.
6. Leia o teste de rollback e explique por que status e histórico precisam ser salvos juntos.
7. Execute o caderno na ordem e compare com os resultados em `sql/README.md`.
8. Modifique uma consulta no ambiente de estudo e explique a mudança com suas palavras.

## Perguntas de entrevista

**Qual problema o projeto resolve?**

Organiza chamados de suporte e transforma os registros em indicadores. A demonstração conecta suporte a sistemas à prática de Java e SQL.

**Chave primária e estrangeira?**

A primária identifica a linha. A estrangeira exige que o ID relacionado exista; `tickets.requester_id` aponta para `app_users.id`.

**Por que JOIN?**

O chamado armazena IDs. JOIN reúne os nomes do solicitante, categoria e departamento sem copiá-los para cada chamado.

**Quando LEFT JOIN?**

Quando a linha principal precisa aparecer sem correspondência: um agente sem chamados continua no relatório com zero.

**Por que COUNT(t.id), e não COUNT(\*)?**

LEFT JOIN preserva uma linha sem chamado. `COUNT(*)` contaria essa linha; `COUNT(t.id)` ignora o identificador nulo.

**WHERE e HAVING?**

WHERE filtra linhas antes da agregação. HAVING filtra grupos; o exemplo 07 mantém categorias com pelo menos dois chamados.

**O que é CTE?**

Um resultado nomeado com `WITH` para organizar a consulta. No exemplo 05, uma etapa calcula volume diário e outra seleciona o resultado.

**Por que parâmetros?**

O valor é enviado como dado, sem virar sintaxe SQL. A busca por `' OR 1=1 --` não retorna todos os chamados.

**O que a transação garante?**

As alterações confirmam juntas ou são desfeitas juntas. O teste rejeita o evento de histórico e confirma a permanência do status anterior.

**Por que bloquear a linha?**

Duas requisições podem tentar alterar o mesmo chamado. `FOR UPDATE` faz a segunda aguardar e verificar o estado atualizado. Não resolve automaticamente todos os problemas de concorrência.

**Como medir SLA?**

Prazo = abertura + janela da prioridade. A taxa usa somente resolvidos/encerrados: quantos foram resolvidos até o prazo dividido pelo total de resoluções. Na demonstração são três entre quatro: 75%.

**Por que taxa nula sem resoluções?**

Não existe denominador. Zero por cento comunicaria que houve resoluções e todas atrasaram, o que seria diferente.

**Índice sempre acelera?**

Não. Ocupa espaço e custa nas escritas. Com poucas linhas, o planejador pode preferir varredura sequencial. `EXPLAIN ANALYZE` mostra o plano executado, sem comprovar ganho por si só.

**O projeto autentica o usuário?**

Não. Valida papéis do catálogo fictício, mas não há login nem identidade verificada do autor.

## Apresentação

Em três minutos, mostre o painel, um JOIN e o teste de rollback. Explique esses pontos com suas palavras. Depois de praticar, descreva no perfil modelagem relacional, consultas SQL, integração com Java e testes de transações, deixando claro que são evidências de um projeto educacional.
