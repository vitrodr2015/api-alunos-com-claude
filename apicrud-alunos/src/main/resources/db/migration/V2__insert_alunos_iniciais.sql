-- Os 8 alunos que antes ficavam fixos na lista em memória do AlunoService.
-- Sem id explícito: numa tabela vazia o AUTO_INCREMENT gera 1 a 8 nesta ordem e os próximos começam no 9.
INSERT INTO aluno (nome, email, data_nascimento) VALUES
    ('Ana Souza',     'ana.souza@escola.com',     '2000-05-10'),
    ('Bruno Lima',    'bruno.lima@escola.com',    '1999-03-21'),
    ('Carla Dias',    'carla.dias@escola.com',    '2001-11-02'),
    ('Diego Martins', 'diego.martins@escola.com', '1998-07-15'),
    ('Eduarda Costa', 'eduarda.costa@escola.com', '2002-01-30'),
    ('Felipe Rocha',  'felipe.rocha@escola.com',  '1997-09-08'),
    ('Gustavo Alves', 'gustavo.alves@escola.com', '2003-04-17'),
    ('Helena Barros', 'helena.barros@escola.com', '2000-12-03');
