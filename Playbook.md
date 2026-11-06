# Playbook: uma API CRUD de alunos, do zero à produção (Spring Boot 4 + Docker + AWS)

Este playbook é a versão **limpa e ordenada** do que foi feito neste projeto. A fonte de verdade, com todos os detalhes, datas e saídas reais, são os três arquivos de anotação do repositório:

| Arquivo | O que tem |
| --- | --- |
| `PassoAPasso.md` | o registro cronológico, passo a passo (49 seções), com decisões, testes e tropeços |
| `Duvida-CICD.md` | a parte da AWS: telas do console de hoje, diferenças do tutorial original e problemas comuns |
| `memoria.md` | o estado atual, as regras de trabalho e as pendências |

Aqui está o **mapa para reproduzir**: o que fazer, em que ordem, como saber que deu certo e onde costuma dar errado. Onde a explicação completa existe, o texto aponta a seção do `PassoAPasso.md` (`PAP §n`) ou do `Duvida-CICD.md` (`DC §n`).

> 🔓 O repositório é **público**. Nunca escreva em arquivo versionado: senha, chave de acesso, token. Nos textos, use marcadores: `<id-da-conta>`, `<IP-publico>`, `<seu-IP>/32`, `sg-...`.

---

## Como ler (e como usar)

- **Cada passo tem:** o objetivo, como fazer, **Pronto quando** (a prova de que funcionou) e **Armadilhas** (o que deu errado de verdade).
- **A regra de ouro do projeto:** nenhum passo está "feito" sem prova. Prova é teste passando, resposta real de HTTP, linha de log ou estado do banco, e não "eu acho que funciona". Depois da prova, **sabotar de propósito** (quebrar o código de um jeito conhecido e ver o teste falhar), para ter certeza de que o teste não é decorativo.
- **Cadência combinada:** um passo por vez; cada passo termina com testes verdes, anotação nos três arquivos e commit/PR.
- **Stack final:** Java 25, Spring Boot 4.1.2 (Spring Framework 7, Spring Security 7, Spring Data 4, Jackson 3), Maven, MySQL 8.4, Flyway, JWT (java-jwt 4.4.0), Docker, GitHub Actions, AWS (ECR, ECS Fargate, RDS, Secrets Manager), Swagger (springdoc 3.2.0), Testcontainers 2.0.5 e REST Assured 6.0.0.

### Pré-requisitos na máquina

JDK 25, Docker Desktop, AWS CLI v2, GitHub CLI (`gh`), Git, Node.js (só para scripts de apoio) e um cliente de API (Postman). No Windows, usar o Git Bash ou o PowerShell conforme o caso (ver a Fase 12, armadilhas de ambiente).

---

## Mapa das fases

| Fase | Assunto | Seções do `PassoAPasso.md` |
| --- | --- | --- |
| 0 | Regras de trabalho e de segurança do repositório | memoria, PAP 15 |
| 1 | Projeto base: Initializr, `application.yml`, camadas, erros | 1 a 4 |
| 2 | A API de alunos: contrato, endpoints, formatos, serialização | 5 a 10 |
| 3 | Banco de dados: MySQL, JPA, Flyway, H2 nos testes | 17 a 24 |
| 4 | Paginação e HATEOAS | 26 a 28 |
| 5 | Contêiner: Dockerfile e Compose | 12 |
| 6 | AWS: RDS, ECR, IAM, ECS, Secrets Manager | 11, 14 e `DC` 1 a 10, 15 |
| 7 | CI/CD e Git Flow | 13, 15, 16 |
| 8 | Segurança: JWT, autorização, CORS, endurecimento | 30 a 39 |
| 9 | Pirâmide de testes e guardas de configuração | 43 |
| 10 | Documentação e saúde: Postman, Swagger, Actuator | 42, 44, 46 |
| 11 | QA de pré-produção e correção dos defeitos | 47, 48 |
| 12 | Releases e operação (checklist, rollback, lições) | 25, 29, 40, 41, 45, 49 |

---

## Fase 0: regras de trabalho e de segurança

**0.1. Trabalhar com anotações vivas.** A cada passo, atualizar no mesmo PR o `PassoAPasso.md` (sempre), o `Duvida-CICD.md` (quando mexer em AWS/CI) e o `memoria.md` (estado e pendências). Quem lê no futuro precisa reproduzir sem a conversa.

**0.2. Repositório público = nada secreto.** Conferir com `grep` antes de cada commit grande. Valores que já estão no histórico (ID da conta, endpoint do RDS, ARNs) foram aceitos e não se reescreve histórico; senhas e chaves nunca entraram. Nos `.md`, usar marcadores.

**0.3. Git Flow com proteção de branches** (detalhado na Fase 7): `main` produção, `develop` integração, `feature/*`, `release/X.Y.Z`; **nunca** push direto; tudo por PR com o check `build` verde.

**0.4. Confirmar antes de ações irreversíveis:** merge na `main` (dispara deploy), apagar branches remotas, mexer no ambiente da AWS. Merge na `main` só com o "pode" explícito e **com o RDS `Available` confirmado** (ver a Fase 12).

---

## Fase 1: projeto base

### Passo 1.1: gerar o projeto (PAP 1)
- Spring Initializr: Maven, Java 25, Spring Boot 4.1.2, grupo `br.com.vitor`, artefato `apicrud-alunos`; dependências Spring Web (no Boot 4 o starter é `spring-boot-starter-webmvc`) e DevTools.
- O `java.version` do `pom.xml` precisa igualar o JDK instalado (e, depois, o do Dockerfile e o da esteira).
- **Pronto quando:** `./mvnw clean package` passa e gera o jar.

### Passo 1.2: `application.yml` no lugar de `.properties` (PAP 2)
Renomear e escrever `spring.application.name: apicrud-alunos`.

### Passo 1.3: camadas (PAP 3)
Pacote base `br.com.vitor.apicrud_alunos` com: `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `exception`, `config`, `security` (e depois `assembler`). O Git não guarda pastas vazias: elas aparecem com a primeira classe de cada uma.

### Passo 1.4: tratamento de erros centralizado (PAP 4)
- `ErrorResponse` (record: `dataHora`, `status`, `erro`, `mensagem`) é o **único formato de erro** da API.
- `ResourceNotFoundException` (404), `InvalidRequestException` (400), `ConflictException` (409) e `GlobalExceptionHandler` (`@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler`).
- Mensagens em português, inclusive as do próprio Spring; qualquer outro erro vira 500 genérico (o detalhe vai só para o log); a resposta de erro é sempre JSON.
- **Pronto quando:** `GlobalExceptionHandlerTests` (um controller falso dentro do teste) cobre 400, 404, 405, 406, 409, 415 e 500.
- **Armadilha (Boot 4):** `@WebMvcTest` está em `org.springframework.boot.webmvc.test.autoconfigure`.

---

## Fase 2: a API de alunos

### Passo 2.1: escrever o contrato antes do código (PAP 5)
Recurso `aluno`, rota `/alunos`, sem versão na URL. Campos: `id` (gerado), `nome` (3 a 100), `email` (válido, único sem diferenciar maiúsculas), `dataNascimento` (no passado, `yyyy-MM-dd`), `ativo` (`true` na criação). A entidade nunca sai na API: usar `AlunoRequest` e `AlunoResponse`.

| Verbo | Rota | Sucesso | Erros |
| --- | --- | --- | --- |
| POST | `/alunos` | 201 + `Location` | 400, 409 |
| GET | `/alunos` | 200 lista paginada (filtros `nome`, `ativo`) | 400 (`sort` inválido) |
| GET | `/alunos/{id}` | 200 | 400, 404 |
| PUT | `/alunos/{id}` | 200 (substitui, menos `ativo`) | 400, 404, 409 |
| PATCH | `/alunos/{id}/disable` e `/enable` | 200, idempotentes | 400, 404 |
| DELETE | `/alunos/{id}` | 204 | 400, 404 |

Ordem de validação: corpo (400), id (400), existência (404), conflito (409), alteração.

### Passo 2.2: implementar (PAP 6 a 8)
- `spring-boot-starter-validation` (`@Valid`). Mapper escrito à mão (`AlunoMapper`), sem MapStruct. Mensagens de validação fixas em português nas anotações.
- O CRUD começou **em memória** (para validar o contrato) e só depois ganhou banco (Fase 3).
- **Pronto quando:** `AlunoControllerTests` cobre os 7 endpoints e os status 200, 201, 204, 400, 404 e 409.

### Passo 2.3: JSON, XML e YAML (PAP 9)
- Dependências `tools.jackson.dataformat:jackson-dataformat-xml` e `-yaml`. **No Boot 4 o Jackson é o 3** (pacote `tools.jackson`): não seguir receitas de Jackson 2. O conversor de YAML já vem pronto; basta a dependência.
- `WebConfig`: JSON como padrão; o YAML aceita também `application/x-yaml`. Os erros continuam sempre em JSON.

### Passo 2.4: serialização padronizada (PAP 10)
`@JsonPropertyOrder` e `@JsonFormat` nos DTOs: data `yyyy-MM-dd`; `dataHora` do erro em UTC com milissegundos. Anotações de formato continuam em `com.fasterxml.jackson.annotation`.

### Passo 2.5: coleção do Postman (PAP 7)
Pasta `postman/` com a coleção e o ambiente (`baseUrl`). Validada rodando todas as requisições. (A versão final, com autenticação, está na Fase 10.)

---

## Fase 3: banco de dados

### Passo 3.1: MySQL local em Docker (PAP 17)
```bash
docker run -d --name mysql-dev \
  -e MYSQL_ROOT_PASSWORD="$SENHA_DEV" -e MYSQL_DATABASE=crud_alunos \
  -p 3307:3306 -v mysql-dev-data:/var/lib/mysql mysql:8.4 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```
- **Porta 3307**, não 3306: o Windows tinha o serviço `MySQL84` na 3306. A senha de desenvolvimento vem de variável do shell e **nunca** vai para arquivo versionado.
- **Pronto quando:** HeidiSQL/DBeaver em `localhost:3307` mostra o schema `crud_alunos` vazio.

### Passo 3.2: JPA e driver (PAP 18)
`spring-boot-starter-data-jpa`, `com.mysql:mysql-connector-j` (escopo `runtime`) e `com.h2database:h2` (escopo `test`). O `application.yml` **de teste substitui o principal** (mesmo nome em `src/test/resources`): tudo que o teste exigir tem de estar nele também.

### Passo 3.3: datasource sem expor segredo (PAP 19)
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${DB_URL:jdbc:mysql://localhost:3307/crud_alunos?createDatabaseIfNotExist=true}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD}          # sem valor padrão: sem a variável a aplicação não sobe
```
- **Armadilha importante:** o Spring Boot **não falha** se `${DB_PASSWORD}` não existe: envia o texto literal como senha e o erro final é enigmático. Solução: um listener de `ApplicationEnvironmentPreparedEvent` no `main` que lança `IllegalStateException` com mensagem clara. (Um `@Component` com `@Value` roda tarde demais.)
- **Pronto quando:** sem `DB_PASSWORD` a aplicação para na hora com a mensagem clara; com ela e o MySQL ligado, sobe e responde.

### Passo 3.4: perfis e arquivos locais (PAP 20)
`application-local.yml` (ignorado pelo Git) vale só com `SPRING_PROFILES_ACTIVE=local`; o modelo versionado é `application-local.yml.example`.

### Passo 3.5: Flyway (PAP 21)
- **Boot 4:** `spring-boot-starter-flyway` + `org.flywaydb:flyway-mysql` (o `flyway-core` sozinho não autoconfigura).
- `ddl-auto: validate` (o Hibernate só **confere**, nunca altera), `open-in-view: false`. Nos testes: Flyway desligado (H2).
- `V1__create_table_aluno.sql`: `id` BIGINT auto incremento, `nome` 100, `email` 255 **único**, `data_nascimento`, `ativo` (padrão verdadeiro), InnoDB `utf8mb4_unicode_ci` (a collation sem caixa dá a unicidade de e-mail sem caixa).
- **Regras:** nunca editar uma migration já aplicada (criar outra); evitar `ddl-auto: update`.

### Passo 3.6: entidade e repository (PAP 22)
`Aluno` como `@Entity` com nomes de coluna iguais aos da migration; `AlunoRepository extends JpaRepository<Aluno, Long>`. **Prova de que o `validate` confere de verdade:** trocar temporariamente o nome de uma coluna na entidade e ver a aplicação **não subir** (`missing column`).

### Passo 3.7: trocar o mock pelo repository (PAP 23)
- `AlunoService` por construtor, `@Transactional` nas escritas e `readOnly` nas leituras. Mesmas regras e mensagens.
- Carga inicial dos 8 alunos na migration `V2__insert_alunos_iniciais.sql` (sem `id` explícito) e em `src/test/resources/data.sql` para o H2.
- Testes unitários do service com Mockito (`AlunoServiceTests`).

### Passo 3.8: testes rápidos com H2 (PAP 24)
`flyway.enabled: false`, `ddl-auto: create-drop`, `defer-datasource-initialization: true`, `sql.init.mode: always`. `mvn test` passa **sem nenhum banco ligado** (é o que a esteira precisa). **Limite:** as migrations não são exercitadas nesses testes (ver a Fase 9 para a solução).

### Passo 3.9: massa de teste para a paginação (PAP 27)
`V3__insert_400_alunos_para_paginacao.sql` (400 alunos determinísticos, 40 inativos) e o mesmo no `data.sql`. **Decisão consciente:** dados de exemplo não vão para produção em projeto real; aqui foi aceito (e depois só uma migration nova desfaz).

---

## Fase 4: paginação e HATEOAS

### Passo 4.1: paginação com `Pageable` (PAP 26)
- O controller recebe `Pageable`; limites no `application.yml`: `default-page-size: 20`, `max-page-size: 50` (repetir no yml de teste). `size` acima de 50 é **reduzido** (200), `page`/`size` inválidos caem nos padrões.
- `AlunoService.listar` acrescenta `id` como desempate (paginação estável). Consultas derivadas `findByNomeContainingIgnoreCase...` (o Spring Data escapa `%` e `_`).
- **Spring Data 4:** `PropertyReferenceException` está em `org.springframework.data.core`.
- **Armadilha:** ao apagar uma classe, rodar com `clean` (o `.class` antigo esconde erro de compilação).

### Passo 4.2: HATEOAS opcional (PAP 28)
- `spring-boot-starter-hateoas`; HAL **só** na listagem e no `GET /alunos/{id}` (`_embedded.alunos`, `_links`, `page`). XML/YAML nesses `GET` passam a dar 406.
- `AlunoModelAssembler` dá `self`, `alunos` e só a ação que faz sentido (`disable` se ativo, `enable` se inativo).
- **Armadilha:** `methodOn(...)` cria proxy do tipo de retorno e `record` é final (erro 500 em toda listagem); montar os links com `linkTo(...).slash(id)`.

---

## Fase 5: contêiner

### Passo 5.1: Dockerfile em duas fases (PAP 12)
```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- `sed` remove o CRLF do `mvnw` (que impediria rodar no Linux); a camada de dependências é cacheada; roda como usuário comum; Java da imagem = `pom.xml` = esteira. Os testes **não** rodam no build da imagem: rodam antes, na esteira.
- `docker-compose.yml` só dá o nome à imagem (`vitrodr/crud-alunos`, porta 8080).
- **A imagem não tem `curl` nem `wget`:** não dá para usar `healthCheck` dentro do contêiner sem instalá-los.
- **Pronto quando:** `docker build` e `docker run -p 8080:8080` respondem; o processo roda como `app`.

---

## Fase 6: AWS

Cadeia final: **GitHub Actions → imagem no ECR → ECS Fargate (aplicação) → RDS MySQL (banco)**, com a senha no Secrets Manager. Região `us-east-1`. Os passos de tela e as diferenças do tutorial original estão no `Duvida-CICD.md` (DC); aqui vai a ordem e o porquê.

| Ordem | O que criar | Nome usado | Detalhe |
| --- | --- | --- | --- |
| 1 | RDS MySQL (Single-AZ, `db.t4g.micro`, 20 GiB) | `database-crud-alunos` | DC 1 |
| 2 | Repositório ECR privado | `respositorio-crud-alunos` | DC 2 |
| 3 | Usuário IAM da esteira + chave | `GITHUB_ACTIONS_CRUD_ALUNOS` | DC 3 |
| 4 | Cluster ECS Fargate | `cluster-crud-alunos` | DC 4 |
| 5 | Secret da senha + política de leitura | `secrets_crud_alunos/db-password`, `LerSecretDB_CrudAlunos` | DC 5 |
| 6 | Security group da aplicação (8080) e regra 3306 no do RDS | `security-group-crud-alunos` | DC 6 |
| 7 | Task Definition e `task-definition.json` | `crud-alunos-task-definition` | DC 7 |
| 8 | Primeira imagem no ECR | tag `latest` | DC 8 |
| 9 | Service ECS | `service-crud-alunos` | DC 9 |
| 10 | Teste pelo IP público | porta 8080 | DC 10 |

Pontos que custam caro se errados:
- **Custo do RDS:** conferir o *Estimated monthly costs* antes de criar (os padrões davam ~US$ 145/mês); usar `db.t4g.micro` e 20 GiB, **Full configuration**, **Self managed**.
- **A política de leitura do secret** vai na **Task execution role** (`ecsTaskExecutionRole`), com `Resource` terminando em `-*` (a AWS acrescenta um sufixo ao ARN). Na primeira tentativa foi criada na role errada e a tarefa caiu com `AccessDeniedException ... secretsmanager:GetSecretValue`.
- **`DB_PASSWORD` na Task Definition precisa ser `ValueFrom`** (o ARN do secret), nunca `Value`.
- **Variáveis da aplicação** (`DB_URL`, `DB_USER`, `TZ` em `environment`; `DB_PASSWORD` e depois `JWT_SECRET` em `secrets`). O database `crud_alunos` é criado pela própria aplicação (`createDatabaseIfNotExist=true`).
- **No Service:** remover as duas subnets de Local Zone (o Fargate não funciona nelas), trocar o security group `default` pelo da aplicação e ligar **Public IP**.
- **Ordem diferente do tutorial:** enviar a imagem ao ECR **antes** de criar o Service (senão `CannotPullContainerError`).
- **Porta 3306 do banco** só para o grupo da aplicação e para o seu IP (`/32`), nunca `0.0.0.0/0`. A 8080 só para **My IP** enquanto a API for de estudo.
- **O `task-definition.json` fica versionado** (a esteira só enxerga o repositório); tem ID da conta e ARNs, nunca senha. Remover os campos que a AWS gera (`taskDefinitionArn`, `revision`, `status`, `requiresAttributes`, `compatibilities`, `registeredAt`, `registeredBy`).
- **Ligar e desligar (PAP 14, DC 15):** desligar = Service com *Desired tasks* = 0 e RDS **Stop temporarily** (religa sozinho após 7 dias); ligar = RDS Start, **esperar `Available`**, depois *Desired tasks* = 1; o IP muda a cada tarefa. Custo ligado: ~US$ 1,30/dia da tarefa mais o RDS.
- **Perfil da AWS CLI** (`aws configure --profile crud-alunos`): lê ECS, ECR e EC2; **não** lê RDS, IAM, Secrets Manager nem CloudWatch Logs.

---

## Fase 7: CI/CD e Git Flow

### Passo 7.1: a esteira (PAP 13, DC 11)
`.github/workflows/continuous-deployment.yml`:
- Gatilhos: `push` na `main` (build, testes **e deploy**; `paths-ignore: '**.md'`) e `pull_request` para `main`, `develop` e `release/**` (só build e testes).
- `permissions: contents: read` (menor privilégio).
- Etapas: checkout → JDK 25 (Temurin, cache Maven) → `mvn -B verify` → `docker compose build` → **(só em push)** login na AWS e no ECR → push da imagem com a tag do **commit** e `latest` → `amazon-ecs-render-task-definition` (troca a imagem do contêiner `container-crudalunos`) → `amazon-ecs-deploy-task-definition` (registra a revisão e atualiza o Service, esperando estabilizar).
- Secrets do GitHub (Settings → Secrets and variables → Actions): `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY`.
- **A esteira não altera o *Desired tasks*:** com 0 ela termina verde e nada sobe.

### Passo 7.2: proteger as branches (PAP 15, 16)
Rulesets em Settings → Rules: `protect-main`, `protect-develop`, `protect-release` (`release/**`). Regras iguais: PR obrigatório (0 aprovações, projeto individual), check **`build`** verde e atualizado, sem force push; em `main` e `develop` também sem apagar. **Teste de bloqueio:** uma branch com um teste quebrado de propósito deve deixar o PR com `build` vermelho e o Merge bloqueado (nunca mesclar). Em repositório privado de conta gratuita os rulesets não são aplicados; como o repositório é público, valem.

### Passo 7.3: Git Flow (PAP 16)
| Branch | Papel | Sai de | Volta para |
| --- | --- | --- | --- |
| `main` | produção; cada merge dispara o deploy | | |
| `develop` | integração das features | `main` | |
| `feature/*` | uma mudança | `develop` | `develop` |
| `release/X.Y.Z` | fecha a versão | `develop` | `main` e `develop` |
| `hotfix/*` | correção urgente | `main` | `main` e `develop` |

Merge sempre com **merge commit** (nunca squash). `gh` (GitHub CLI) abre os PRs e acompanha o `build`.

---

## Fase 8: segurança (JWT, autorização, CORS, endurecimento)

Ordem importa, e **a segurança ficou numa branch com o build vermelho de propósito** (os testes de contrato quebram por 401 até serem ajustados), indo para a `develop` só quando verde.

### Passo 8.1: dependências (PAP 30)
`spring-boot-starter-security`, `com.auth0:java-jwt:4.4.0`, `spring-security-test` (teste). **O aviso se confirma:** todas as rotas passam a dar 401 e 59 testes quebram.

### Passo 8.2: usuário e permissões (PAP 31)
- Migration `V4`: `permission`, `users` (plural: `user` é reservada) e `user_permission` (chave composta, `ON DELETE CASCADE` do lado do usuário).
- `Permission implements GrantedAuthority`; `User implements UserDetails` com `@ManyToMany(fetch = EAGER)`; `UserRepository.findByUserName` com `Optional`; `UserService implements UserDetailsService`.
- **Pronto quando:** os testes de carga do usuário passam **também contra o MySQL real** (`-Dspring.flyway.enabled=true -Dspring.jpa.hibernate.ddl-auto=validate -Dspring.sql.init.mode=never`).

### Passo 8.3: hash de senha (PAP 32)
`PasswordEncoder` = `DelegatingPasswordEncoder` com **só o BCrypt** registrado (`{bcrypt}$2a$10$...`). Não usar `createDelegatingPasswordEncoder()` (aceita `{noop}`, `md5` etc.). **Limite descoberto:** senha acima de **72 bytes** é recusada (não truncada); validar no cadastro.

### Passo 8.4: provedor de token JWT (PAP 33)
- HS256, access **15 min**, refresh **24 h**; claims `iss`, `sub`, `roles`, `type` (`access`/`refresh`), `iat`, `exp`.
- Valida assinatura, emissor, expiração e **tipo** (um não vale como o outro); exige `exp` e `sub`; **recarrega o usuário do banco** a cada validação e recusa conta desativada/bloqueada/expirada. O banco manda, não o token.
- Segredo: `security.jwt.secret: ${JWT_SECRET}` **sem valor padrão**, mínimo 32 bytes, falha na subida com mensagem clara. Falhas viram `InvalidTokenException` (401) com mensagem genérica.
- **Limites a lembrar:** sem lista de revogação não há logout de verdade; recarregar o usuário custa uma consulta por requisição; o refresh vai no **corpo** de um POST, nunca na URL; renovar não invalida o refresh antigo.

### Passo 8.5: filtro e configuração (PAP 34)
```java
http.csrf(AbstractHttpConfigurer::disable)                       // sem cookie de sessão, nada a proteger
    .cors(Customizer.withDefaults())
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
    .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
    .authorizeHttpRequests(/* ver o passo 8.8 */)
    .addFilterBefore(new JwtTokenFilter(tokenProvider, errorWriter), UsernamePasswordAuthenticationFilter.class);
```
- `JwtTokenFilter` lê `Authorization: Bearer ...` (esquema sem diferenciar caixa); token ruim **não derruba a requisição**: segue sem autenticação e a rota protegida responde 401.
- **Armadilha:** o filtro **não é `@Component`** (se fosse bean, rodaria duas vezes). A `SecurityConfig` o cria com `new`.
- 401 e 403 em JSON no formato `ErrorResponse` por `SecurityErrorWriter` (o `GlobalExceptionHandler` não alcança erros dos filtros).

### Passo 8.6: ajustar os testes (PAP 35)
Testes de **contrato** (`AlunoControllerTests`, `ContentNegotiationTests`, `SerializationTests`) passam a `@AutoConfigureMockMvc(addFilters = false)`, com comentário avisando que **não exercitam segurança**. A segurança é testada à parte, com filtros **ligados** (`SecurityFilterChainTests`). **Pronto quando:** `mvn -B clean verify` verde com `JWT_SECRET` e `DB_PASSWORD` **fora** do ambiente (como na esteira).

### Passo 8.7: endpoints de autenticação (PAP 36)
- `POST /auth/signin` (público) → `{tokenType, accessToken, accessExpiresAt, refreshToken, refreshExpiresAt}` com `Cache-Control: no-store`; `POST /auth/refresh` (público, só aceita refresh); `POST /auth/register` (**só ADMIN**) → `201` sem senha. Só JSON.
- **Regra: não revelar se o usuário existe.** Todo erro de login vira a **mesma** resposta 401 (usuário inexistente, senha errada, conta desativada...), com tempo equivalente (medido: ~95 a 115 ms nos dois casos).
- Cadastro: `username` 3 a 100 só `[A-Za-z0-9._@-]`; `fullName` 3 a 150; senha 8 a 72 **bytes**. Permissão padrão `USER`; o corpo **não escolhe** permissões (mass assignment ignorado). `DataIntegrityViolationException` vira **409** (corrida de dois cadastros iguais).

### Passo 8.8: autorização por rota (PAP 37)
| Rota | Quem pode |
| --- | --- |
| `GET`/`HEAD` em `/alunos`, `/alunos/{id}` | `USER` ou `ADMIN` |
| `POST /alunos`, `PUT`, `PATCH disable/enable` | `USER` ou `ADMIN` |
| `DELETE /alunos/{id}` (irreversível) | só `ADMIN` |
| `POST /auth/register` | só `ADMIN` |
| `POST /auth/signin`, `POST /auth/refresh`, `OPTIONS` | públicos |
| **qualquer outra rota** | **negada** (`anyRequest().denyAll()`) |

- Regras no `SecurityConfig` (uma tabela só, fácil de ler), permissões em constantes (`Authorities`). **A regra final é `denyAll()`, não `authenticated()`:** com `authenticated()` toda rota nova nasceria aberta a qualquer logado.
- O `HEAD` tem regra própria (o Spring Security compara o método exato).
- **Pronto quando:** `AuthorizationTests` (uma matriz de chamadas por perfil) passa; token com `roles` forjado ou ADMIN revogado no banco recebe 403.

### Passo 8.9: CORS (PAP 38)
- Hoje nenhum navegador chama a API: a lista de origens começa **vazia** (`cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:}`). `*` e formatos inválidos impedem a subida.
- `WebConfig.addCorsMappings`: métodos da API, cabeçalhos `Authorization, Content-Type, Accept`, **sem credenciais**, `Max-Age` 3600, `Location` exposto.
- **Aviso do playbook, descoberto na prática:** `.cors()` ligado e `OPTIONS` liberado se cobrem; esquecer **só o `.cors()`** tira o CORS dos 401/403; esquecer **os dois** faz o pré-voo virar 401. CORS **não** protege o servidor (curl ignora), só instrui o navegador.

### Passo 8.10: credenciais iniciais e endurecimento (PAP 39)
- O `admin` semeado pela `V5` tinha senha conhecida: a **`V6`** o remove **só se a senha ainda for a semeada** (`DELETE ... WHERE user_name='admin' AND password='<hash exato>'`). A `V5` não se edita.
- Primeiro admin por meio controlado: `AdminBootstrapRunner` (`ApplicationRunner`), desligado por padrão, ativado por `ADMIN_BOOTSTRAP_PASSWORD` (12 caracteres a 72 bytes, sem espaço/quebra de linha nas pontas). Só cria se **não existir ADMIN**; falha (não promove) se já houver usuário comum com o nome; **nunca loga a senha**. Subir uma vez e **remover a variável**.
- **Guardas na esteira** (`ConfigHygieneTests`, leem os arquivos reais): nenhum segredo com valor padrão; nenhum segredo em `environment` da Task Definition (só em `secrets`, com ARN do Secrets Manager); só a `V5` semeia usuário; hash da `V6` = hash da `V5`; Swagger e CORS desligados por padrão. `SensitivePathsTests`: actuator, `heapdump`, console H2, `.env`, `.git/config`... dão 401 anônimo e **403 até para o ADMIN**; erros nunca vazam pilha nem classes.
- **Roteiro da AWS** (console, antes da primeira release com segurança; sem isso a tarefa não sobe):
  1. Secrets Manager: segredo do JWT (`openssl rand -hex 32`, **não** reaproveitar o local) e, para o 1º deploy, o da senha do admin.
  2. IAM: acrescentar os ARNs novos (com `-*`) na política inline `LerSecretDB_CrudAlunos` da `ecsTaskExecutionRole`.
  3. `task-definition.json`: `JWT_SECRET` (e, **só no 1º deploy**, `ADMIN_BOOTSTRAP_PASSWORD`) em `secrets` com `valueFrom` = ARN **real** (um ARN inventado quebra o deploy).
  4. Ligar o ambiente (RDS `Available`, depois o Service); depois do primeiro start, **tirar** o `ADMIN_BOOTSTRAP_PASSWORD` do arquivo e fazer novo deploy.
  5. Conferir: login com a senha escolhida 200; com a senha antiga 401; `GET /alunos` sem token 401.

---

## Fase 9: pirâmide de testes e guardas

| Nível | Ferramenta | Banco | O que valida |
| --- | --- | --- | --- |
| Unitário (service) | JUnit 5 + Mockito | nenhum | regras de negócio isoladas |
| Web (controller) | `@SpringBootTest` + MockMvc (filtros off nos testes de contrato) | H2 | status, corpo, erros, formatos |
| Segurança | filtros **ligados** | H2 | token válido/vencido/adulterado/tipo errado, 401, 403 |
| Integração | Testcontainers (MySQL 8.4 real) + REST Assured | MySQL em contêiner | fluxo completo, migrations reais, token real |

### Passo 9.1: nível de integração (PAP 43)
- Dependências (teste): `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-mysql` (versão 2.0.5 do BOM) e `io.rest-assured:rest-assured:6.0.0`.
- Classe base `AbstractIntegrationTest` com **um único contêiner** (`@ServiceConnection`, padrão *singleton container*) e o perfil `testcontainers` (Flyway **ligado**, `ddl-auto: validate`, sem `data.sql`). O primeiro admin nasce pelo `AdminBootstrapRunner`, como em produção.
- `FluxoCompletoIntegrationTests`: login, CRUD completo, 400/404/409/401/403, token adulterado/vencido/de outro segredo, e o que só o MySQL real mostra (e-mail repetido em maiúsculas, `V1` a `V6` aplicadas, `admin` semeado removido).
- **Exige Docker ligado**, local e na esteira (o `ubuntu-latest` já tem). Soma ~40 s ao build.

### Passo 9.2: conferir a pirâmide
Cada camada deve ter os casos feliz, 400, 404, 401, 403 e 409, **onde se aplicam** (o 403 não existe no service: a autorização mora no `SecurityConfig`). Lacuna achada e fechada: a camada de segurança não tinha o 409.

### Passo 9.3: sabotar para provar que o teste é real
Depois de cada passo: quebrar o código de um jeito conhecido (liberar uma rota, remover uma regra, deixar a data leniente...) e confirmar que **algum teste falha**; restaurar e conferir que o arquivo ficou idêntico. Isso pegou, por exemplo, que `.cors()` e `OPTIONS` se cobrem.

---

## Fase 10: documentação e saúde

### Passo 10.1: coleção do Postman com autenticação (PAP 42)
Pasta `postman/`: a coleção ganhou a pasta `00 - Autenticação` (login do admin, renovar, registrar usuário comum, login dele) e `3 - Segurança (401 e 403)`; Bearer `{{token}}` **herdado da coleção** (o login guarda o token no ambiente); ambientes `Local` e `AWS`; `adminPassword` vem **vazio** no arquivo. **O campo do token na resposta é `accessToken`** (não `token`). Validada com o `newman` (105 asserções).

### Passo 10.2: Swagger / OpenAPI (PAP 44)
- `springdoc-openapi-starter-webmvc-ui` **3.2.0** (funciona com Boot 4.1.2). Anotações em **interfaces** `controller/docs/*Docs` (`AlunoControllerDocs`, `AuthControllerDocs`) que o controller implementa, deixando o controller só com a lógica; `OpenApiConfig` com título, descrição (como autenticar, quem pode o quê, formatos, paginação), esquema `bearerAuth` e **dois grupos** (`GroupedOpenApi`: Alunos e Autenticação).
- **Decisão:** o Swagger nasce **desligado** (`SWAGGER_ENABLED`, padrão `false`); ligado, as rotas da documentação são públicas e a API continua exigindo o token. Desligado, dão 401 (e 403 até para o ADMIN).
- `signin` e `refresh` ficam sem cadeado (`@SecurityRequirements` vazio). **O customizer global não chega aos grupos**: anexar o `OperationCustomizer` a cada `GroupedOpenApi`.
- **Provado na tela** (Chrome): a página abre, os grupos aparecem, **Authorize** aceita o token e **Execute** devolve 200. Cuidado: `example` num parâmetro de filtro **é enviado como valor** pelo *Try it out*.
- **Regra permanente:** a cada endpoint novo ou mudança, atualizar o Swagger junto (interface `*Docs`, `OpenApiConfig` e `OpenApiDocsTests`).
- Como testar localmente: terminal **fora do VS Code**; `docker start mysql-dev`; em `apicrud-alunos`, definir `SWAGGER_ENABLED=true` e `ADMIN_BOOTSTRAP_PASSWORD` e rodar `./mvnw spring-boot:run`; abrir `http://localhost:8080/swagger-ui/index.html` (login em *Autenticação*, depois **Authorize** em *Alunos*).

### Passo 10.3: health check com Actuator (PAP 46)
```yaml
management:
  endpoints: { web: { exposure: { include: health } } }
  endpoint:  { health: { probes: { enabled: true }, show-details: never, show-components: never } }
```
- Só o `health` é exposto; `GET /actuator/health/**` é **público e só de leitura**; o resto do `/actuator` continua na regra final (401/403). Com os probes: `/actuator/health/liveness` e `/readiness`.
- **O health completo inclui o banco:** com o banco fora ele dá **503 `DOWN`** (e, antes da correção do Passo 11.2, demorava 30 s). **ECS e ALB devem apontar para `/actuator/health/liveness`**, que não depende do banco; senão reiniciariam a tarefa em laço.
- O corpo do health traz também `groups: ["liveness","readiness"]` (só os nomes; o Spring não deixa esconder). Sem `curl` na imagem, não há `healthCheck` no contêiner; com um ALB, o *target group* checa por HTTP e não precisa de `curl`.

---

## Fase 11: QA de pré-produção

### Passo 11.1: testar o artefato que vai para a produção (PAP 47)
Método: construir a **imagem Docker de produção**, subir com um **MySQL real descartável** numa rede Docker isolada, com **as mesmas variáveis da Task Definition** e o admin criado pelo bootstrap, **sem tocar na AWS nem no banco de desenvolvimento**. Dois scripts: uma bateria HTTP (412 verificações, incluindo HTTP bruto por socket para o que o `fetch` não envia: cabeçalhos duplicados, `TRACE`, URLs malformadas, e concorrência) e uma fase de banco/logs/reinício/segredos/queda do banco/desligamento. Ao final, remover contêineres, rede, imagem e arquivos de senha.

O que a bateria cobre: matriz de autorização por perfil; ataques ao token (`alg=none`, outro segredo, payload alterado, vencido, tipo errado, sem `exp`, forjado dizendo ADMIN); cadastro (limites, bytes do BCrypt, *mass assignment*); injeção SQL; limites de todos os campos; ids estranhos (0, negativo, `Long.MAX`, estouro); paginação, ordenação e filtros; negociação de conteúdo; corpo de 5 MB; CORS; erros sem vazar detalhes; concorrência (25 POST iguais, cadastros iguais, `DELETE` do mesmo aluno, PUTs cruzados); persistência e reinício; troca do `JWT_SECRET`; segredos ausentes; banco derrubado e recuperação; desligamento limpo; segredos e tokens **fora dos logs**.

### Passo 11.2: defeitos reais achados e corrigidos (PAP 47 e 48)
| # | Defeito | Correção |
| --- | --- | --- |
| 1 | `DELETE` concorrente do mesmo aluno dava 500 | `apagarPorId` (`@Modifying`): apagar numa instrução atômica; 404 se 0 linhas |
| 2 | Data inexistente corrigida **em silêncio** (`2000-02-30` virava `02-29`) | leitura estrita: `@JsonFormat(pattern="uuuu-MM-dd", lenient=OptBoolean.FALSE)` (`yyyy` não funciona no modo estrito) |
| 3 | Banco fora do ar: 401 enganoso ou 500, depois de **30 s** | Hikari `connection-timeout: 5000`; `FalhaDeInfraestrutura`; **503 com `Retry-After`** no filtro do JWT, no login e no tratador geral |
| 4 | `sort` estranho (`nome;drop table aluno`) dava 500 | `sort` só por `id, nome, email, dataNascimento, ativo`; o resto é 400 |
| 5 | Percentual inválido na URL (`?nome=%ZZ`) dava 500 | tratar `org.apache.tomcat.util.http.InvalidParameterException` como 400 |
| 6 | O login ignorava acentos (`admín` entrava como `admin`; collation sem acento) | conferir o nome de novo com `equalsIgnoreCase` |

Resultado depois das correções (QA refeito na imagem corrigida, com o banco derrubado de verdade): **402 de 412 verificações; respostas 5xx caíram de 11 para zero**; as 10 restantes são validações cosméticas (por exemplo `page=abc` cair no padrão). Banco fora do ar: **503 em 5 s**, e a API volta sozinha quando o banco volta. Cada correção ganhou teste, sabotagem e (quando HTTP) prova no Tomcat e MySQL reais; o Swagger foi atualizado (503 em todas as rotas).

### Passo 11.3: parecer
Sólido para estudo e demonstração; **ainda não para usuários reais**. Faltam, na infraestrutura: **HTTPS** (ALB com certificado e domínio), **usuário próprio do banco** no lugar do `root`, limite de tentativas de login, tokens revogáveis, mais de uma instância, alarmes e conferir os backups do RDS.

---

## Fase 12: releases e operação

### Passo 12.1: o ciclo de uma release (PAP 16, 25, 29, 45)
1. Todas as features já na `develop` (cada uma por PR com `build` verde).
2. `git checkout -b release/X.Y.Z origin/develop` e `git push -u origin release/X.Y.Z`.
3. `gh pr create --base main --head release/X.Y.Z`; esperar o `build` (testes + imagem).
4. **Antes do merge: o usuário confirma o RDS `Available`** (e o Service com *Desired tasks* = 1). Conferir também que a release não muda o `task-definition.json`/migrations sem avisar (`git diff --stat origin/main..origin/release/X.Y.Z -- apicrud-alunos/task-definition.json apicrud-alunos/src/main/resources/db`).
5. `gh pr merge N --merge` (merge commit); a esteira faz o deploy.
6. Esperar o rollout (`aws ecs describe-services ... deployments[].rolloutState` = `COMPLETED`) e **verificar pelo comportamento**: rota nova responde, rotas protegidas dão 401, o Swagger desligado dá 401.
7. Tag anotada no commit da `main`: `git tag -a vX.Y.Z origin/main -m "..."` e `git push origin vX.Y.Z`.
8. Back-merge: branch a partir da `develop`, `git merge origin/main`, PR para a `develop`, merge.
9. Registrar nos três arquivos de anotação.

Versões feitas: 1.0.0 (esteira e primeiro deploy), 1.1.0 (MySQL e Flyway na AWS), 1.2.0 (paginação, HATEOAS, 400 alunos), 1.3.0 (segurança), 1.3.1 (senha do bootstrap sai da Task Definition), 1.4.0 (Swagger desligado, integração com MySQL real, Postman), 1.4.1 (correções do QA e health check).

### Passo 12.2: o deploy falhou? (PAP 49, DC "Problemas")
O caso real (1.4.1): build e testes passaram, mas o passo `Deploy to Amazon ECS` falhou com `not found after stabilization ... rolled back by the deployment circuit breaker`.
1. **Olhar os eventos do Service** (`aws ecs describe-services ... --query 'services[0].events[0:10]'`) e as **tarefas paradas** (`aws ecs list-tasks --desired-status STOPPED` + `describe-tasks`: `stoppedReason`, `exitCode`). Código 1 logo na subida = a aplicação não conseguiu iniciar.
2. **Causa mais comum neste projeto: o RDS parado ou subindo** (a aplicação exige o banco no arranque). Foi exatamente isso; as tarefas da versão **antiga** também já estavam caindo.
3. **O rollback deixa o código ANTIGO no ar** (reusa o digest da implantação anterior, mesmo que o `latest` já aponte para a imagem nova). Confirme pela rota nova (`/actuator/health`: 401 = código antigo).
4. Com o RDS `Available`, **reexecutar só o job que falhou**: `gh run rerun <id> --failed`. Isso também destrava o PR de back-merge, que ficava `BLOCKED` porque o check `build` do **commit da `main`** era o deploy falho.
5. O perfil da CLI não lê o CloudWatch Logs: para o log da tarefa, usar o console (**ECS → tarefa → Logs** ou o log group `/ecs/crud-alunos-task-definition`).

### Passo 12.3: armadilhas de ambiente (Windows) que custaram tempo
- **VS Code:** só lê as variáveis de ambiente do Windows no momento em que abre; criadas depois, o botão Run não as enxerga (`DB_PASSWORD não foi definida`). Fechar por completo e reabrir, ou rodar num terminal novo.
- **Git Bash x PowerShell:** `export VAR=...` no Bash; `$env:VAR="..."` no PowerShell. O `mvnw` fica **dentro** de `apicrud-alunos`.
- **Encerrar a aplicação:** `timeout` do bash mata o *wrapper* mas **não a JVM** no Windows; uma instância esquecida, ligada ao banco real, com o DevTools, aplicou no banco local as migrations de uma sabotagem. Usar um script que mata a JVM e conferir `Get-NetTCPConnection -LocalPort 8080` antes de mexer em `src/main/resources`.
- **Editar arquivos:** `sed` com tabulação quebra código e XML (usar o editor ou scripts por linha); arquivos CRLF exigem cuidado nos cortes por posição; textos longos com apóstrofos no *heredoc* falham (usar o editor). O `java` de um arquivo só e o `node` precisam de caminhos do Windows (`cygpath -w`).
- **Banco local:** o serviço `MySQL84` do Windows ocupa a 3306; o contêiner usa a 3307.

---

## Apêndice A: variáveis de ambiente

| Variável | Onde | Para quê | Padrão |
| --- | --- | --- | --- |
| `DB_URL` | Task Definition (`environment`) | endereço do banco | `jdbc:mysql://localhost:3307/crud_alunos?createDatabaseIfNotExist=true` |
| `DB_USER` | Task Definition (`environment`) | usuário do banco | `root` |
| `DB_PASSWORD` | Secrets Manager (`secrets`) | senha do banco | **nenhum**: sem ela não sobe |
| `JWT_SECRET` | Secrets Manager (`secrets`) | segredo do JWT (≥ 32 caracteres) | **nenhum**: sem ele não sobe |
| `TZ` | Task Definition (`environment`) | fuso | `America/Sao_Paulo` |
| `CORS_ALLOWED_ORIGINS` | `environment` (quando houver front-end) | origens permitidas | vazio = sem CORS |
| `ADMIN_BOOTSTRAP_PASSWORD` / `_USERNAME` | Secrets Manager, **só no 1º deploy** | cria o primeiro admin | vazio = desligado / `admin` |
| `SWAGGER_ENABLED` | `environment` (se quiser ligar) | liga a documentação | `false` |

## Apêndice B: estrutura do projeto

```
api-alunos-com-claude/
├── apicrud-alunos/
│   ├── Dockerfile, docker-compose.yml, task-definition.json, application-local.yml.example
│   ├── pom.xml, mvnw
│   └── src/main/java/br/com/vitor/apicrud_alunos/
│       ├── config/       SecurityConfig, WebConfig (CORS, negociação), PasswordConfig, OpenApiConfig
│       ├── controller/   AlunoController, AuthController, docs/ (interfaces do Swagger)
│       ├── service/      AlunoService, AuthService, UserService
│       ├── repository/   AlunoRepository, UserRepository, PermissionRepository
│       ├── model/        Aluno, User, Permission
│       ├── dto/          AlunoRequest/Response, SignIn/Refresh/Register, TokenResponse, UserResponse
│       ├── mapper/ assembler/   AlunoMapper, AlunoModelAssembler (HATEOAS)
│       ├── security/     JwtTokenProvider, JwtTokenFilter, entry point, access denied, AdminBootstrapRunner, Authorities
│       └── exception/    ErrorResponse, GlobalExceptionHandler, FalhaDeInfraestrutura, exceções de negócio
│   └── src/main/resources/ application.yml, db/migration/V1..V6
│   └── src/test/           unitários, web, segurança, integration/ (Testcontainers), data.sql, application.yml (H2)
├── .github/workflows/continuous-deployment.yml
├── postman/              coleção e ambientes (local e AWS)
└── PassoAPasso.md, Duvida-CICD.md, memoria.md, Playbook.md
```

## Apêndice C: migrations

| Versão | O que faz |
| --- | --- |
| `V1` | tabela `aluno` |
| `V2` | 8 alunos iniciais |
| `V3` | 400 alunos de exemplo (paginação) |
| `V4` | `permission`, `users`, `user_permission` |
| `V5` | permissões `ADMIN` e `USER` e o admin semeado |
| `V6` | remove o admin semeado (só se a senha ainda for a semeada) |

## Apêndice D: checklist antes de cada release

- [ ] `./mvnw -B clean verify` verde com `JWT_SECRET`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS` e `ADMIN_BOOTSTRAP_PASSWORD` **fora do ambiente**; também com `-Dsurefire.runOrder=random`.
- [ ] `docker compose build` ok (rodar dentro de `apicrud-alunos`).
- [ ] Se há migration nova: validada subindo no MySQL real (os testes com H2 não a exercitam; os de integração sim).
- [ ] Se a Task Definition ou as variáveis mudam: os segredos e a política IAM já existem **antes** do merge.
- [ ] Swagger atualizado para qualquer endpoint novo ou alterado.
- [ ] Usuário confirmou: **RDS `Available`** e Service com *Desired tasks* = 1.
- [ ] Depois do deploy: rota nova responde, 401 sem token, Swagger desligado dá 401, `/actuator/health` `UP`.
- [ ] Tag, back-merge e anotações nos três arquivos.

## Apêndice E: problemas comuns

| Sintoma | Causa e solução |
| --- | --- |
| `DB_PASSWORD`/`JWT_SECRET` não foi definida ao rodar pelo IDE | o IDE abriu antes de as variáveis existirem; fechar e reabrir, ou rodar num terminal novo |
| `Access denied for user 'root' (using password: YES)` sem citar a senha | `${DB_PASSWORD}` ausente enviado como texto literal (corrigido pelo listener do `main`) |
| Todas as rotas 401 depois de ligar a segurança | esperado; ajustar os testes de contrato (`addFilters=false`) e criar o login |
| Tarefa do ECS para com `AccessDeniedException ... GetSecretValue` | a política de leitura está em outra role; criar na `ecsTaskExecutionRole` e forçar novo deploy |
| Service `0/1 Tasks running` | ler o **Stopped reason** na aba Tasks (filtro *Any desired status*) |
| Esteira verde, API não responde | *Desired tasks* = 0, ou o IP mudou |
| Deploy com `circuit breaker` e tarefas saindo com código 1 | RDS parado/subindo; esperar `Available` e `gh run rerun <id> --failed` |
| PR de back-merge `BLOCKED` com o build "verde" | o check do commit da `main` é o deploy que falhou; reexecutar o job |
| 500 em toda listagem depois do HATEOAS | `methodOn` com retorno `record`; usar `linkTo(...).slash(id)` |
| `NoClassDefFoundError` de `PropertyReferenceException` | pacote antigo; no Spring Data 4 é `org.springframework.data.core` |
| `ObjectOptimisticLockingFailureException` em `DELETE` simultâneo | trocar ler+apagar por `delete ... where id` atômico |
| Pré-voo de CORS vira 401 | faltou `.cors()` **e** `OPTIONS` liberado no `SecurityConfig` |
| `docker compose build` diz `no configuration file provided` | rodar dentro de `apicrud-alunos` |

## Apêndice F: regras de ouro (lições gerais)

1. **Prova antes de "feito":** teste passando e resposta real, não suposição.
2. **Sabote para confirmar** que o teste pega a falha.
3. **Segredo nunca em arquivo versionado**; `secrets` na Task Definition, nunca `environment`; um teste de higiene garante.
4. **Negar por padrão** (`denyAll`): rota nova nasce fechada.
5. **O banco manda, não o token:** permissões e estado da conta são relidos a cada requisição.
6. **Mesma resposta para qualquer falha de login** e tempo equivalente.
7. **Banco fora do ar é 503**, nunca 401/500, e rápido (timeout curto).
8. **Nunca editar migration aplicada;** corrigir com migration nova.
9. **Confirmar o RDS `Available` antes de mesclar na `main`;** verificar a versão pelo comportamento depois do deploy.
10. **Anotar enquanto faz:** o que deu errado e como se resolveu é a parte mais valiosa do playbook.

## Apêndice G: limitações conhecidas e próximos passos

- Sem limite de tentativas de login nem bloqueio de conta (40 tentativas erradas em paralelo, todas 401, sem 429); cada tentativa custa ~80 ms de CPU (BCrypt).
- Refresh token não revogável (não há logout); não há endpoint para trocar a senha nem para dar `ADMIN` a outro usuário (só pelo banco).
- **Para usuários reais:** HTTPS (ALB com certificado e domínio), usuário próprio do banco (privilégios só sobre `crud_alunos`), mais de uma instância com ALB e `healthCheck` apontando para `/actuator/health/liveness`, alarmes e backups conferidos.
- Itens de limpeza: sobras na AWS (`rest-with-claude-app-sg`, cluster `rest-with-claude-spring`), segredo `admin-bootstrap-password` sem uso, nome do repositório ECR com erro de digitação (`respositorio`), `HELP.md` do Initializr, escolha entre MapStruct e o mapper manual.
