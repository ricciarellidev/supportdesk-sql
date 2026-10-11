-- Educational CRUD lab. Run the whole file; ROLLBACK discards the inserted/updated/deleted rows.
-- PostgreSQL sequence values are not rolled back, so gaps in generated IDs are normal.
BEGIN;
INSERT INTO departments (name) VALUES ('Laboratório SQL');
INSERT INTO app_users (name, email, role, department_id)
SELECT 'Pessoa do laboratório', 'sql-lab@example.invalid', 'REQUESTER', id
FROM departments WHERE name = 'Laboratório SQL';
UPDATE app_users SET name = 'Pessoa atualizada' WHERE email = 'sql-lab@example.invalid';
SELECT u.name, u.email, d.name AS department
FROM app_users u JOIN departments d ON d.id = u.department_id
WHERE u.email = 'sql-lab@example.invalid';
DELETE FROM app_users WHERE email = 'sql-lab@example.invalid';
DELETE FROM departments WHERE name = 'Laboratório SQL';
ROLLBACK;
