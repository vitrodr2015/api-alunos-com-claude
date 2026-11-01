CREATE TABLE aluno (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    data_nascimento DATE         NOT NULL,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    -- a collation utf8mb4_unicode_ci não diferencia maiúsculas de minúsculas: e-mail único sem caixa
    CONSTRAINT uk_aluno_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
