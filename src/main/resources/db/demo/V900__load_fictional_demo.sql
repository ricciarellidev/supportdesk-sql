-- Portfolio-only dataset. Every identity is fictional. The normal profile never loads this migration.
INSERT INTO departments (id, name) VALUES (1, 'Tecnologia'), (2, 'Operações'), (3, 'Financeiro');
INSERT INTO app_users (id, name, email, role, department_id, active) VALUES
    (1, 'Marina Costa', 'marina@example.com', 'REQUESTER', 2, TRUE),
    (2, 'Rafael Lima', 'rafael@example.com', 'REQUESTER', 3, TRUE),
    (3, 'Ana Souza', 'ana@example.com', 'AGENT', 1, TRUE),
    (4, 'Lucas Dias', 'lucas@example.com', 'AGENT', 1, TRUE),
    (5, 'Bianca Alves', 'bianca@example.com', 'AGENT', 1, TRUE),
    (6, 'Conta Inativa', 'inactive@example.com', 'AGENT', 1, FALSE);
INSERT INTO categories (id, name, department_id) VALUES
    (1, 'Acesso a sistemas', 1), (2, 'Banco de dados', 1), (3, 'Rede e conectividade', 1), (4, 'Equipamentos', 1);

INSERT INTO tickets (id, title, description, requester_id, category_id, assigned_agent_id, priority, status,
                     created_at, updated_at, due_at, resolved_at, closed_at) VALUES
    (1, 'Falha no acesso ao portal', 'Usuário não consegue acessar o portal interno de demonstração.', 1, 1, 3, 'HIGH', 'OPEN',
     '2026-10-01 08:00+00', '2026-10-01 08:05+00', '2026-10-01 16:00+00', NULL, NULL),
    (2, 'Consulta de estoque lenta', 'Investigar consulta SQL utilizada no relatório de estoque fictício.', 1, 2, 4, 'MEDIUM', 'IN_PROGRESS',
     '2026-10-01 09:00+00', '2026-10-01 10:00+00', '2026-10-02 09:00+00', NULL, NULL),
    (3, 'Rede indisponível', 'Conectividade interrompida no ambiente de treinamento.', 2, 3, 3, 'CRITICAL', 'RESOLVED',
     '2026-10-01 10:00+00', '2026-10-01 12:00+00', '2026-10-01 14:00+00', '2026-10-01 12:00+00', NULL),
    (4, 'Ajustar perfil de acesso', 'Validar permissões do usuário em sistema de demonstração.', 2, 1, 3, 'HIGH', 'CLOSED',
     '2026-10-01 10:00+00', '2026-10-01 20:00+00', '2026-10-01 18:00+00', '2026-10-01 19:00+00', '2026-10-01 20:00+00'),
    (5, 'Extrair relatório mensal', 'Preparar relatório de atendimentos com agrupamentos SQL.', 1, 2, 4, 'MEDIUM', 'RESOLVED',
     '2026-10-01 11:00+00', '2026-10-01 17:00+00', '2026-10-02 11:00+00', '2026-10-01 17:00+00', NULL),
    (6, 'Configurar impressora', 'Configuração inicial de equipamento do laboratório.', 2, 4, NULL, 'LOW', 'OPEN',
     '2026-10-01 12:00+00', '2026-10-01 12:00+00', '2026-10-04 12:00+00', NULL, NULL),
    (7, 'Validar cadastro duplicado', 'Verificar integridade e duplicidade de registros fictícios.', 1, 2, 4, 'HIGH', 'CLOSED',
     '2026-10-01 12:00+00', '2026-10-01 16:00+00', '2026-10-01 20:00+00', '2026-10-01 15:00+00', '2026-10-01 16:00+00'),
    (8, 'Revisar conexão VPN', 'Conexão intermitente no ambiente local de demonstração.', 2, 3, 4, 'MEDIUM', 'IN_PROGRESS',
     '2026-10-01 13:00+00', '2026-10-01 14:00+00', '2026-10-02 13:00+00', NULL, NULL);

-- Import markers identify snapshots, rather than inventing a complete historical audit trail.
INSERT INTO ticket_events (ticket_id, actor_id, event_type, message, created_at)
SELECT id, requester_id, 'CREATED', 'Registro importado do conjunto fictício de demonstração.', created_at FROM tickets;
SELECT setval(pg_get_serial_sequence('departments', 'id'), (SELECT MAX(id) FROM departments));
SELECT setval(pg_get_serial_sequence('app_users', 'id'), (SELECT MAX(id) FROM app_users));
SELECT setval(pg_get_serial_sequence('categories', 'id'), (SELECT MAX(id) FROM categories));
SELECT setval(pg_get_serial_sequence('tickets', 'id'), (SELECT MAX(id) FROM tickets));
