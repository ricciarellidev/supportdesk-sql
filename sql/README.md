# Caderno SQL

Execute no banco de demonstração `supportdesk` com o perfil `demo` carregado. Os resultados pressupõem o conjunto inicial, antes de abrir ou arquivar chamados.

Na raiz do projeto:

```bash
docker compose exec -T db psql -U supportdesk -d supportdesk -v ON_ERROR_STOP=1 < sql/01_backlog_join.sql
```

No PowerShell:

```powershell
Get-Content sql/01_backlog_join.sql | docker compose exec -T db psql -U supportdesk -d supportdesk -v ON_ERROR_STOP=1
```

Um editor SQL também pode conectar a `localhost:5433`, banco `supportdesk`, com os valores locais de `.env.example`.

| Arquivo                         | Conceito                   | Resultado inicial                             |
| ------------------------------- | -------------------------- | --------------------------------------------- |
| `01_backlog_join.sql`           | JOIN e LEFT JOIN           | 4 chamados na fila                            |
| `02_category_group_by.sql`      | GROUP BY e FILTER          | 4 categorias; Banco de dados = 3 chamados     |
| `03_agents_left_join.sql`       | LEFT JOIN com zero         | 3 agentes ativos; Bianca = 0                  |
| `04_sla_case.sql`               | CASE e porcentagem         | HIGH = 50%; CRITICAL e MEDIUM = 100%          |
| `05_daily_volume_cte.sql`       | CTE e agrupamento por dia  | 8 aberturas em 01/10/2026 UTC                 |
| `06_rank_agents_window.sql`     | DENSE_RANK                 | Lucas = 4, Ana = 3, Bianca = 0                |
| `07_categories_having.sql`      | HAVING                     | 3 categorias com pelo menos 2 chamados        |
| `08_requesters_exists.sql`      | EXISTS                     | 2 solicitantes com chamados na fila           |
| `09_latest_ticket_subquery.sql` | Subconsulta correlacionada | Chamados 7 e 8                                |
| `10_crud_transaction.sql`       | CRUD e ROLLBACK            | Nenhuma linha do laboratório permanece        |
| `11_explain_backlog.sql`        | EXPLAIN ANALYZE            | Plano depende do ambiente e do relógio        |
| `12_department_workload.sql`    | Relações e agregação       | Operações = 4; Financeiro = 4; Tecnologia = 0 |

No arquivo 10, execute tudo, incluindo `ROLLBACK`. Sequências podem avançar mesmo com rollback. No arquivo 11, `ANALYZE` executa a consulta; o exemplo contém somente um SELECT.

Os [relatórios usados pela API](../src/main/resources/sql/reports/) são lidos pela aplicação e testados com resultados esperados, incluindo grupos vazios e ausência de denominador. O caderno acrescenta perguntas didáticas.
