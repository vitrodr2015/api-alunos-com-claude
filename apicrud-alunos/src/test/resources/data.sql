-- Carga dos testes (H2): os mesmos 8 alunos da migration V2 do MySQL.
-- Sem id explícito: o H2 gera 1 a 8 nesta ordem, e os alunos criados nos testes seguem a sequência.
INSERT INTO aluno (nome, email, data_nascimento, ativo) VALUES
    ('Ana Souza',     'ana.souza@escola.com',     '2000-05-10', TRUE),
    ('Bruno Lima',    'bruno.lima@escola.com',    '1999-03-21', TRUE),
    ('Carla Dias',    'carla.dias@escola.com',    '2001-11-02', TRUE),
    ('Diego Martins', 'diego.martins@escola.com', '1998-07-15', TRUE),
    ('Eduarda Costa', 'eduarda.costa@escola.com', '2002-01-30', TRUE),
    ('Felipe Rocha',  'felipe.rocha@escola.com',  '1997-09-08', TRUE),
    ('Gustavo Alves', 'gustavo.alves@escola.com', '2003-04-17', TRUE),
    ('Helena Barros', 'helena.barros@escola.com', '2000-12-03', TRUE);
