-- Endurecimento (passo 37 do playbook): ninguém pode ter um login conhecido num ambiente publicado.
-- A V5 semeou o usuário 'admin' para o desenvolvimento, com uma senha que foi mostrada ao responsável (portanto
-- conhecida). Esta migration remove esse usuário, mas SÓ se ele ainda tiver exatamente a senha semeada:
-- se alguém já trocou a senha dele por outra, ele é legítimo e fica. As ligações em user_permission são apagadas
-- junto (ON DELETE CASCADE). A V5 não foi editada (nunca se edita uma migration já aplicada).
--
-- O primeiro administrador passa a ser criado por um meio controlado: a variável de ambiente
-- ADMIN_BOOTSTRAP_PASSWORD, numa execução única (veja AdminBootstrapRunner).
DELETE FROM users
WHERE user_name = 'admin'
  AND password = '{bcrypt}$2a$10$lJUPTDytsniixGj2P.mYzus5zKHoIUlYsTaTQDPN/rrr45wtpQAna';
