-- Permissões do sistema. ADMIN pode cadastrar usuários (/auth/register); USER é a permissão padrão de quem se cadastra.
INSERT INTO permission (description) VALUES ('ADMIN'), ('USER');

-- Primeiro administrador, para o sistema ter por onde começar (sem ele ninguém consegue chamar /auth/register).
-- ATENÇÃO (repositório público): aqui está só o HASH da senha, nunca a senha. A senha é aleatória e longa (24 caracteres),
-- então o hash é inviável de quebrar; ela foi entregue uma única vez ao responsável e NÃO está em nenhum arquivo.
-- Em estudo é aceitável semear usuário em migration; em produção real não é (crie o admin fora do repositório).
-- Para trocar a senha: gere um novo hash {bcrypt}... e rode um UPDATE, ou crie uma migration nova. Nunca edite esta.
INSERT INTO users (user_name, full_name, password)
VALUES ('admin', 'Administrador', '{bcrypt}$2a$10$lJUPTDytsniixGj2P.mYzus5zKHoIUlYsTaTQDPN/rrr45wtpQAna');

INSERT INTO user_permission (id_user, id_permission)
SELECT u.id, p.id
FROM users u
JOIN permission p ON p.description IN ('ADMIN', 'USER')
WHERE u.user_name = 'admin';
