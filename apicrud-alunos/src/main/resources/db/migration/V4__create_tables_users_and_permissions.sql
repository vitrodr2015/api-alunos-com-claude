-- Usuários e permissões (Spring Security). "users" no plural porque "user" é palavra reservada em vários bancos.
-- A collation utf8mb4_unicode_ci não diferencia maiúsculas de minúsculas: "Admin" e "admin" são o mesmo user_name.

CREATE TABLE permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    description VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_description UNIQUE (description)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE users (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    user_name               VARCHAR(100) NOT NULL,
    full_name               VARCHAR(150) NOT NULL,
    password                VARCHAR(255) NOT NULL,
    account_non_expired     BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN      NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN      NOT NULL DEFAULT TRUE,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_user_name UNIQUE (user_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- tabela de ligação (muitos para muitos); apagar o usuário apaga as ligações dele, mas a permissão não pode ser apagada em uso
CREATE TABLE user_permission (
    id_user       BIGINT NOT NULL,
    id_permission BIGINT NOT NULL,
    PRIMARY KEY (id_user, id_permission),
    CONSTRAINT fk_user_permission_user       FOREIGN KEY (id_user)       REFERENCES users (id)      ON DELETE CASCADE,
    CONSTRAINT fk_user_permission_permission FOREIGN KEY (id_permission) REFERENCES permission (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
