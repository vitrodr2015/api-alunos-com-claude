# Playbook2: criar uma API profissional (Spring Boot + MySQL + AWS)

Guia reutilizável para criar uma API do zero até a produção. Não conta a história de um projeto: diz **o que fazer, em que ordem, por quê, como fazer e como provar que ficou certo**. A stack é fixa, então os módulos trazem comandos e trechos de código prontos para adaptar.

Documentos irmãos: `Colinha-Nova-API.md` (a lista curta das etapas), `Colinha-Prompts.md` (um prompt por etapa, para Claude Code ou Codex), `Playbook.md` e `PassoAPasso.md` (a história do projeto de alunos que originou tudo isto).

---

## Stack assumida

| Peça | Escolha | Observação |
| --- | --- | --- |
| Linguagem | Java 25 (LTS) | a mesma versão no `pom.xml`, no Dockerfile e na esteira |
| Framework | Spring Boot 4.1.x (Spring 7, Spring Security 7, Spring Data 4, Jackson 3) | **confirme a versão e os pacotes reais**: receitas antigas (Jackson 2, `org.springframework.data.mapping`) não compilam |
| Build | Maven (com Maven Wrapper) | |
| Banco | MySQL 8.4, Flyway | banco em memória (H2) só para testes rápidos |
| Segurança | Spring Security + JWT (`java-jwt` 4.4.0, HS256) + BCrypt | |
| Testes | JUnit 5, Mockito, MockMvc, Testcontainers 2.0.5, REST Assured 6.0.0 | versões do Testcontainers pelo BOM do Boot |
| Documentação | springdoc-openapi 3.2.0 (Swagger), coleção do Postman | |
| Contêiner e CI | Docker, GitHub Actions | |
| Nuvem | AWS: ECR, ECS Fargate, RDS MySQL, Secrets Manager, IAM, CloudWatch e (para produção real) ALB + ACM | região de exemplo: `us-east-1` |

**Convenções de texto:** `<api>` = nome da API; `<Recurso>` e `<recurso>` = a entidade principal; `<id-da-conta>`, `<dominio>` e `<IP>` = marcadores. **Nunca** coloque valores reais (conta, IP, endpoint, senha) em arquivo versionado de repositório público.

---

## Parte A. Fundamentos

### A1. Princípios

1. **Prova antes de "pronto".** Teste passando ou resposta real. Nunca "acho que funciona".
2. **Sabotar para confirmar.** Quebre o código de um jeito conhecido, veja o teste falhar, restaure (o arquivo deve voltar idêntico). Teste que nunca falhou não prova nada.
3. **Segredo nunca no repositório,** nem em log, nem em chat.
4. **Negar por padrão.** Rota nova nasce fechada; segredo não tem valor padrão.
5. **A fonte da verdade é o banco,** não o token nem o cache.
6. **Falhar cedo e com mensagem clara,** na subida, e não no meio de uma requisição.
7. **Migration aplicada nunca se edita.** Corrige-se com uma nova.
8. **Anotar enquanto faz.** O que deu errado e como se resolveu vale mais que o que deu certo.

### A2. Perfis de projeto

| Perfil | Módulos obrigatórios | Pode adiar |
| --- | --- | --- |
| Protótipo local | 0 a 6 e 15 | nuvem, esteira, segurança forte, QA |
| API interna | 0 a 12, 14, 15 e 18 | limites de abuso, QA completo |
| Produção com usuários reais | todos | nada |

### A3. Definição de pronto (vale para todo módulo)

1. a prova do "Pronto quando" foi mostrada;
2. a **sabotagem** foi feita e desfeita;
3. a suíte inteira passa **com os segredos fora do ambiente** (como no CI);
4. a documentação da API, se mudou, está atualizada;
5. nada sensível entrou nos arquivos;
6. o módulo foi anotado.

---

## Parte B. Decisões iniciais

Decida e registre **antes** de escrever código. As escolhas de pilha já estão feitas; sobram estas.

| Decisão | Opções | Critério | Consequência |
| --- | --- | --- | --- |
| Quem usa a API? | pública, interna, só serviços | exposição e risco | define HTTPS e os limites de abuso |
| Quem pode o quê? | 2 papéis (`USER`, `ADMIN`), mais papéis | quantos perfis reais existem | define a matriz do módulo 11 |
| Navegador chama? | sim, não | existe front-end? | se não, CORS fica vazio |
| Formatos | só JSON; mais XML/YAML/HAL | alguém realmente pede? | cada formato extra é superfície a testar |
| Exclusão | lógica (`ativo`), física | dá para desfazer? | muda o contrato e as permissões |
| Dados de exemplo em produção? | sim, nunca | o ambiente é real? | seed só no que roda em teste |
| Dados pessoais | quais, por quanto tempo | lei de proteção de dados | logs, retenção e exclusão |
| Ambientes | só produção; produção e homologação | custo e risco | dobra a infraestrutura |

---

## Parte C. Os módulos

| Bloco | Módulos |
| --- | --- |
| Fundação | 0 regras, 1 projeto e CI, 2 contrato, 3 configuração e segredos |
| Dados e núcleo | 4 banco e migrations, 5 camadas e erros, 6 CRUD vertical, 7 testes com banco real, 8 listagem, 9 formato |
| Segurança | 10 autenticação, 11 autorização e CORS, 12 admin inicial e endurecimento, 13 limites de abuso |
| Operação | 14 falha de infraestrutura, 15 contêiner, 16 QA da imagem |
| Entrega | 17 nuvem, 18 esteira, 19 release, 20 operação |

**O molde de cada módulo:** Objetivo; Por quê; Decisões; Como; Pronto quando; Sabotagem; Armadilhas clássicas (sintoma, causa, correção); Entregáveis.

---

## Bloco 1. Fundação

### Módulo 0. Regras de trabalho

**Objetivo.** Combinar como o trabalho será feito e registrado, antes de qualquer código.

**Por quê.** Sem regras escritas, cada sessão (ou pessoa) decide de um jeito, e o que foi aprendido se perde com a conversa.

**Decisões.** O repositório é público ou privado? Quem aprova o quê? O que um colaborador (ou um agente) pode fazer sozinho: commit, merge, deploy? Onde ficam as anotações?

**Como.**
1. Crie os arquivos: `memoria.md` (regras, decisões, estado, pendências), `PassoAPasso.md` (registro cronológico) e `Duvida-CICD.md` (diferenças e problemas da nuvem). Se usar agente, o `CLAUDE.md` ou `AGENTS.md` com as regras (veja o prompt de abertura em `Colinha-Prompts.md`).
2. Escreva as regras: um passo por vez; prova antes de "pronto"; nada de segredo; trabalhar por branch e PR; **ação irreversível (merge na principal, deploy, console da nuvem) só com confirmação, e a confirmação de uma ação não vale para a próxima**.
3. Crie o `.gitignore` **na raiz** (um só), ignorando `application-local.yml`, `.env*` e arquivos de segredo.

**Pronto quando.** Uma sessão nova (ou outra pessoa) retoma o projeto só lendo os arquivos.

**Sabotagem.** Peça a alguém, ou a uma sessão nova, que continue **sem a conversa**. As perguntas que ela fizer são as lacunas a anotar.

**Armadilhas clássicas.**
- *Sintoma:* ninguém sabe em que ponto o projeto está. *Causa:* o estado vivia só na conversa. *Correção:* o estado em arquivo.
- *Sintoma:* valores sensíveis aparecem no repositório público. *Causa:* o histórico do Git guarda tudo; apagar depois não resolve. *Correção:* marcadores (`<id-da-conta>`, `<IP>`) desde o começo.
- *Sintoma:* as anotações dizem o contrário da realidade. *Causa:* o "estado" não foi relido. *Correção:* a cada passo, reler "estado" e "pendências".

**Entregáveis.** Os arquivos de anotação, o arquivo de contexto do agente e o `.gitignore`.

---

### Módulo 1. Projeto, repositório e CI

**Objetivo.** Um projeto que compila, num repositório protegido, com CI que bloqueia código quebrado.

**Por quê.** Sem CI e sem proteção das branches, um commit quebrado entra na `main` e, com o deploy automático, vai direto para a produção; e versões de Java diferentes entre projeto, imagem e esteira só quebram na hora de publicar.

**Decisões.** O fluxo de branches (Git Flow: `main`, `develop`, `feature/*`, `release/X.Y.Z`); repositório público ou privado (as regras de proteção podem não valer em repositório **privado de conta gratuita**).

**Como.**
1. Gere o projeto em `start.spring.io`: Maven, Java 25, Spring Boot 4.1.x, grupo `<grupo>`, artefato `<api>`, dependências mínimas (Spring Web, que no Boot 4 é `spring-boot-starter-webmvc`, e DevTools). Mantenha o Maven Wrapper.
2. `git init`, primeiro commit, repositório no GitHub (`gh repo create`), branches `main` e `develop`.
3. Crie o workflow de CI (modelo no apêndice D4): `mvn -B verify` em push na `main` e em PR para `main`, `develop` e `release/**`.
4. **Rode o workflow uma vez** (abra um PR qualquer). Só depois, em *Settings → Rules → Rulesets*, crie as regras para `main`, `develop` e `release/**`: PR obrigatório (0 aprovações, se for projeto individual), check **`build`** obrigatório com a branch atualizada, sem force push; em `main` e `develop`, sem apagar a branch.
5. Daqui em diante tudo vai por branch e PR (`gh pr create`, `gh pr checks`, `gh pr merge --merge`).

**Pronto quando.** Um PR com teste quebrado de propósito fica com o build vermelho e o merge bloqueado.

**Sabotagem.** É o próprio teste: crie a branch com um teste quebrado, abra o PR, confirme o bloqueio e feche **sem mesclar**.

**Armadilhas clássicas.**
- *Sintoma:* o check `build` não aparece ao criar a regra. *Causa:* o workflow nunca rodou. *Correção:* rode uma vez e só então crie a regra.
- *Sintoma:* tudo passa no PC e quebra só ao publicar. *Causa:* versões de Java diferentes. *Correção:* a mesma versão no `pom.xml`, no Dockerfile e no workflow.
- *Sintoma:* a regra de proteção não vale. *Causa:* repositório privado em conta gratuita. *Correção:* repositório público ou plano pago.
- *Sintoma:* dois `.gitignore`. *Causa:* o gerador cria um dentro do projeto. *Correção:* apagar e manter só o da raiz.

**Entregáveis.** Projeto, workflow de CI, regras de proteção, anotação do fluxo de branches.

---

### Módulo 2. Contrato da API

**Objetivo.** Escrever o que a API promete antes de implementar.

**Por quê.** Sem contrato, cada pessoa inventa um status e um formato de erro, e o teste não tem o que conferir; mudar depois custa caro.

**Decisões.** O recurso e os campos; as regras (unicidade, obrigatoriedade); exclusão lógica ou física; versão na URL; o formato de erro.

**Como.** Crie um arquivo versionado com duas tabelas:
- **Campos** de `<Recurso>`: nome, tipo, regra (tamanho, formato), quem preenche (entrada, saída ou os dois). O `id` e a situação (`ativo`) **nunca** são do cliente.
- **Endpoints:** verbo, rota, entrada, sucesso e erros.

| Verbo | Rota | Sucesso | Erros |
| --- | --- | --- | --- |
| POST | `/<recursos>` | 201 + `Location` | 400, 409 |
| GET | `/<recursos>` | 200 lista paginada | 400 |
| GET | `/<recursos>/{id}` | 200 | 400, 404 |
| PUT | `/<recursos>/{id}` | 200 | 400, 404, 409 |
| PATCH | `/<recursos>/{id}/disable` e `/enable` | 200, idempotentes | 400, 404 |
| DELETE | `/<recursos>/{id}` | 204 | 400, 404 |

Defina a **ordem de validação**: corpo (400), id (400), existência (404), conflito (409), alteração. A entidade nunca sai pela API: use DTOs de entrada e de saída.

**Pronto quando.** O contrato foi revisado e aprovado **antes** de qualquer código.

**Sabotagem.** Peça a um revisor que tente implementar só pelo contrato. As perguntas dele são as lacunas.

**Armadilhas clássicas.**
- *Sintoma:* o contrato muda depois de pronto. *Causa:* paginação e formato ficaram para depois. *Correção:* decida cedo e registre a mudança quando acontecer.
- *Sintoma:* "id inválido" e "não existe" saem iguais. *Correção:* 400 para o primeiro, 404 para o segundo.
- *Sintoma:* dúvida sobre repetir uma operação. *Correção:* declare no contrato que ativar e desativar são idempotentes.

**Entregáveis.** O contrato versionado e a decisão sobre versão e exclusão.

---

### Módulo 3. Configuração e segredos

**Objetivo.** Nenhum valor sensível no código, e a aplicação se recusa a subir mal configurada.

**Por quê.** O Spring **não falha** quando falta `${DB_PASSWORD}`: manda o texto literal como senha, e o erro que aparece nem cita a senha. E segredo com valor padrão acaba indo para a produção.

**Decisões.** Quais valores são segredo (senha do banco, segredo do JWT, senha do admin inicial); como se configura o ambiente local.

**Como.**
1. Troque `application.properties` por `application.yml`. Use `${VAR}` **sem padrão** para segredos e `${VAR:padrão}` só para o que não é segredo (modelo completo no apêndice D2).
2. No `main`, um listener que para a subida com mensagem clara (um `@Component` com `@Value` roda tarde demais, depois de o Hibernate tentar conectar):

```java
aplicacao.addListeners((ApplicationEnvironmentPreparedEvent evento) -> {
    var ambiente = evento.getEnvironment();
    if (ambiente.getProperty("spring.datasource.url") != null
            && ambiente.getProperty("DB_PASSWORD", "").isBlank()) {
        throw new IllegalStateException("A variável de ambiente DB_PASSWORD não foi definida.");
    }
    if (ambiente.getProperty("JWT_SECRET", "").isBlank()) {
        throw new IllegalStateException("A variável de ambiente JWT_SECRET não foi definida (mínimo 32 caracteres).");
    }
});
```

3. Configuração só da sua máquina: `application-local.yml` (ignorado pelo Git, vale com `SPRING_PROFILES_ACTIVE=local`) e o modelo `application-local.yml.example` **versionado, fora de `src/main/resources`**.
4. O `src/test/resources/application.yml` **substitui** o principal nos testes: repita nele o que os testes exigem, com valores **falsos e óbvios** (`JWT_SECRET` de teste, por exemplo).
5. Um teste de higiene que **lê o `application.yml` real** e proíbe segredo com valor padrão (comece com esta regra; ela ganha outras a cada módulo).
6. Gere segredos fortes com `openssl rand -hex 32`. No Windows, variáveis de usuário só são vistas por processos abertos **depois** de criadas.

**Pronto quando.** Sem a variável, a aplicação para com texto claro; com ela, sobe. O teste de higiene falha se alguém der um valor padrão a um segredo.

**Sabotagem.** Coloque um valor padrão num segredo e mostre o teste de higiene falhar.

**Armadilhas clássicas.**
- *Sintoma:* "Access denied" sem citar a senha. *Causa:* o framework enviou o texto literal `${DB_PASSWORD}`. *Correção:* o listener acima.
- *Sintoma:* os testes passam local e quebram no CI (ou o contrário). *Causa:* o arquivo de teste substitui o principal e algo ficou só num deles.
- *Sintoma:* "variável não definida" no IDE, embora ela exista no sistema. *Causa:* o IDE abriu **antes** de a variável ser criada. *Correção:* fechar e reabrir.
- *Sintoma:* senha "errada" que parece certa. *Causa:* quebra de linha no fim do valor (típico de `echo`). *Correção:* validar espaço e quebra de linha nas pontas.

**Entregáveis.** `application.yml`, `application-local.yml.example`, listener no `main`, teste de higiene, `.gitignore` conferido com `git check-ignore`.

---

## Bloco 2. Dados e núcleo

### Módulo 4. Banco e migrations

**Objetivo.** Esquema versionado, reproduzível e conferido contra o código.

**Por quê.** Sem migrations versionadas, cada ambiente tem um banco diferente e ninguém sabe qual; e o `validate` faz a subida falhar cedo quando código e banco divergem, em vez de errar só quando a rota for chamada.

**Decisões.** A *collation* (maiúsculas e acentos contam?); política de dados de exemplo; nomes de tabela (evite palavras reservadas: `users`, não `user`).

**Como.**
1. **MySQL local em Docker.** Verifique antes se a porta padrão está livre (o Windows pode ter um serviço MySQL na 3306); mapeie outra, como a 3307:

```bash
docker run -d --name mysql-dev \
  -e MYSQL_ROOT_PASSWORD="$SENHA_DEV" -e MYSQL_DATABASE=<api> \
  -p 3307:3306 -v mysql-dev-data:/var/lib/mysql mysql:8.4 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

   A senha de desenvolvimento vem de variável do shell e nunca de arquivo versionado.
2. **Dependências** (apêndice D1): `spring-boot-starter-data-jpa`, `mysql-connector-j` (runtime), `spring-boot-starter-flyway` e `flyway-mysql` (no Boot 4 o `flyway-core` sozinho não autoconfigura), `h2` (teste).
3. **Datasource e JPA** no `application.yml`: `ddl-auto: validate`, `open-in-view: false`, `flyway.enabled: true`.
4. **`V1__create_table_<recurso>.sql`**, com a unicidade **no banco**:

```sql
CREATE TABLE <recurso> (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  data_nascimento DATE NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

5. **Entidade** (`@Entity`, `@Table`, `@Id` com `IDENTITY`, `@Column` com **nome explícito** em cada campo) e `JpaRepository`.
6. **Testes rápidos:** `src/test/resources/application.yml` com `flyway.enabled: false`, `ddl-auto: create-drop`, `defer-datasource-initialization: true`, e um `data.sql` **sem `id` explícito** (com id, a sequência do H2 não avança e a primeira criação colide).
7. Para validar uma migration nova, suba a aplicação no `mysql-dev`; ou rode os testes contra o MySQL real com `-Dspring.datasource.url=... -Dspring.flyway.enabled=true -Dspring.jpa.hibernate.ddl-auto=validate -Dspring.sql.init.mode=never`.

**Pronto quando.** A aplicação sobe no banco real e aplica a V1; ao trocar o nome de uma coluna na entidade, a subida **falha** com `missing column`.

**Sabotagem.** (1) Renomeie a coluna na entidade e veja a subida falhar. (2) Edite uma migration já aplicada e veja o Flyway recusar (checksum diferente).

**Armadilhas clássicas.**
- *Sintoma:* o contêiner não sobe por "porta em uso". *Causa:* um serviço do sistema ocupa a porta padrão. *Correção:* mapear outra porta.
- *Sintoma:* o e-mail "A@x" e "a@x" são o mesmo no banco, mas não nos testes. *Causa:* a *collation* do MySQL ignora maiúsculas e o H2 não; o mesmo vale para **acentos** (um login com nome acentuado pode achar outro usuário).
- *Sintoma:* os testes rápidos passam e a migration está errada. *Causa:* eles não rodam as migrations. *Correção:* o módulo 7.
- *Sintoma:* dados de exemplo aparecem em produção. *Causa:* seed numa migration comum. *Correção:* seed só no que roda em teste; se já foi, só outra migration desfaz.
- *Sintoma:* `ddl-auto: update` "resolve" e some a rastreabilidade. *Correção:* nunca use `update`.

**Entregáveis.** Migrations, entidade, repository, comando do banco local documentado, anotação da decisão sobre *collation*.

---

### Módulo 5. Camadas e erros padronizados

**Objetivo.** Estrutura previsível e um único formato de erro.

**Por quê.** Sem um formato único, o cliente trata cada rota de um jeito, e um erro inesperado pode vazar pilha, nome de classe ou SQL para quem ataca.

**Decisões.** O idioma das mensagens; os campos do erro; quais exceções de negócio existem.

**Como.**
1. Pacotes em `br.<grupo>.<api>`: `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `exception`, `config`, `security` (o controller só fala HTTP; o service guarda as regras e as transações).
2. `ErrorResponse`, um `record` com `dataHora` (UTC, com milissegundos), `status`, `erro` e `mensagem`.
3. Exceções de negócio: `ResourceNotFoundException` (404), `InvalidRequestException` (400), `ConflictException` (409).
4. `GlobalExceptionHandler` com `@RestControllerAdvice`, **estendendo `ResponseEntityExceptionHandler`**, para os erros do próprio Spring (405, 406, 415, corpo ilegível, parâmetro ausente, tipo errado) saírem no mesmo formato, em JSON, mesmo quando o cliente pede outro `Accept`. O 500 devolve mensagem genérica; a pilha só vai para o log.
5. Teste com um **controller falso** dentro do próprio teste (`@WebMvcTest`, que no Boot 4 fica em `org.springframework.boot.webmvc.test.autoconfigure`).

**Pronto quando.** 400, 404, 405, 406, 409, 415 e 500 saem no mesmo formato.

**Sabotagem.** Remova o tratador do 409 (ou do 500) e mostre o teste falhar.

**Armadilhas clássicas.**
- *Sintoma:* o erro do framework sai em outro formato. *Causa:* o tratador não cobre as exceções do próprio framework. *Correção:* estender a classe-base.
- *Sintoma:* o 500 mostra pilha, classe ou SQL. *Correção:* mensagem genérica e detalhe só no log.
- *Sintoma:* os erros 401 e 403 saem sem formato. *Causa:* nascem nos filtros de segurança, antes do tratador global. *Correção:* o módulo 10.

**Entregáveis.** O formato de erro, as exceções, o tratador global e os testes.

---

### Módulo 6. CRUD em fatia vertical

**Objetivo.** Os endpoints do contrato, ponta a ponta, já com o banco.

**Por quê.** Fazer o CRUD antes do banco obriga a reescrever o service depois; e é aqui que nascem os defeitos que só o QA acha (data inexistente corrigida em silêncio, apagar que dá 500 com pedidos simultâneos).

**Decisões.** As validações de cada campo; o que o cliente **não** pode definir; as fronteiras das transações.

**Como.**
1. **DTOs** de entrada (com `jakarta.validation`: `@NotBlank`, `@Size`, `@Email`, `@Past`) e de saída (`record`). Mensagens fixas em português nas anotações.
2. **Mapper** escrito à mão (ou MapStruct, se preferir) e **service** com `@Transactional` (`readOnly = true` nas leituras).
3. **Data estrita.** O parser corrige datas por padrão (`2000-02-30` vira `2000-02-29`). Torne a leitura estrita (no modo estrito o ano deve ser `uuuu`; `yyyy` não funciona):

```java
@JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE) LocalDate dataNascimento
```

4. **Apagar de forma atômica**, numa instrução só (ler e depois apagar dá erro 500 se dois pedidos chegarem juntos):

```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("delete from <Recurso> a where a.id = :id")
int apagarPorId(@Param("id") Long id);
// no service: if (repository.apagarPorId(id) == 0) throw new ResourceNotFoundException(...)
```

5. **Swagger desde o primeiro endpoint:** `springdoc-openapi-starter-webmvc-ui`, com as anotações nas **interfaces** `<Recurso>ControllerDocs` que o controller implementa (o controller fica só com a lógica), e **desligado por padrão** (`springdoc.api-docs.enabled: ${SWAGGER_ENABLED:false}`). Documente 200/201/204 e 400/401/404/409.
6. **Testes:** unitário do service (Mockito) e web (`@SpringBootTest` + `@AutoConfigureMockMvc`) cobrindo o caso feliz e os erros.

**Pronto quando.** Cada endpoint prova o caso feliz e os erros 400, 404 e 409; uma data inexistente dá 400; dois `DELETE` simultâneos do mesmo recurso dão 1 sucesso e o resto 404.

**Sabotagem.** Deixe a leitura da data leniente; troque o apagar atômico por ler e depois apagar. Os testes devem falhar.

**Armadilhas clássicas.**
- *Sintoma:* o service é reescrito quando o banco chega. *Causa:* o CRUD foi feito primeiro em memória.
- *Sintoma:* `2000-02-30` vira `2000-02-29` sem erro. *Causa:* o parser corrige datas. *Correção:* modo estrito.
- *Sintoma:* 500 quando dois `DELETE` chegam juntos. *Causa:* ler e depois apagar. *Correção:* uma instrução só.
- *Sintoma:* o cliente consegue definir `id` ou a situação do registro. *Causa:* o DTO aceita campos demais (*mass assignment*). *Correção:* ignorar o que não é do cliente.
- *Sintoma:* o Swagger mostra o valor de um `example` num filtro e a listagem vem filtrada. *Causa:* o *Try it out* envia o exemplo como valor. *Correção:* não use `example` em parâmetros de filtro.

**Entregáveis.** Controller, service, DTOs, mapper, interface `*Docs`, testes.

---

### Módulo 7. Testes com banco real

**Objetivo.** Provar o comportamento contra o banco de verdade, com as migrations de verdade.

**Por quê.** O H2 se comporta diferente do MySQL (não roda as migrations, trata maiúsculas e acentos de outro jeito), então testes verdes podem esconder um erro que só aparece em produção.

**Decisões.** Se o CI tem Docker (o `ubuntu-latest` do GitHub tem).

**Como.**
1. Dependências de teste (apêndice D1): `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-mysql` (versões da BOM do Boot) e `io.rest-assured:rest-assured` 6.0.0.
2. **Classe base com um único contêiner** para todas as classes de integração, e um perfil que liga as migrations:

```java
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
    "app.admin-bootstrap.username=admin.integracao",
    "app.admin-bootstrap.password=<senha-falsa-de-teste-com-12+-caracteres>" })
@ActiveProfiles("testcontainers")
public abstract class AbstractIntegrationTest {
    @ServiceConnection
    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
    static { MYSQL.start(); }
}
```

```yaml
# src/test/resources/application-testcontainers.yml
spring:
  flyway: { enabled: true }
  jpa: { defer-datasource-initialization: false, hibernate: { ddl-auto: validate } }
  sql: { init: { mode: never } }
```

3. Testes de fluxo completo (login com token real, CRUD, 400/404/409/401/403) e de **concorrência** (por exemplo, 20 chamadas simultâneas iguais, com `ExecutorService`).

**Pronto quando.** `mvn verify` passa com as migrations reais; o fluxo completo e os erros estão provados contra o banco real.

**Sabotagem.** Desligue o Flyway no perfil e veja o contexto não subir (`validate` não acha as tabelas).

**Armadilhas clássicas.**
- *Sintoma:* tudo verde e o erro aparece em produção. *Causa:* só testes com H2.
- *Sintoma:* o CI não roda os testes de integração. *Causa:* o runner não tem Docker.
- *Sintoma:* a versão do Testcontainers não casa com o Docker Engine novo. *Correção:* versões da BOM do Boot (2.x); a 1.20 falha com Docker 29.
- *Sintoma:* o build fica lento. *Causa:* um contêiner novo por classe. *Correção:* contêiner único.

**Entregáveis.** Classe base, perfil, testes de fluxo e de concorrência.

---

### Módulo 8. Listagem

**Objetivo.** Paginação, filtros e ordenação seguras.

**Por quê.** Sem limite de tamanho, um `size` enorme derruba a API; sem lista fixa de campos, um `sort` malicioso chega ao banco (o Spring Data o barra, mas a API responde 500).

**Decisões.** Tamanho padrão e máximo; filtros; campos ordenáveis.

**Como.**
1. Receba um `Pageable` no controller. No `application.yml` (e repetido no de teste):

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 50
```

2. No service, **lista fixa** de campos ordenáveis e desempate por id (paginação estável):

```java
private static final Set<String> CAMPOS_ORDENAVEIS = Set.of("id", "nome", "email", "dataNascimento", "ativo");
for (Sort.Order ordem : pageable.getSort()) {
    if (!CAMPOS_ORDENAVEIS.contains(ordem.getProperty())) {
        throw new InvalidRequestException("Não é possível ordenar por esse campo. Campos aceitos: ...");
    }
}
```

3. Filtros por consultas **derivadas** do Spring Data (`findByNomeContainingIgnoreCase...`): elas escapam `%` e `_`.
4. No Spring Data 4 a exceção de propriedade inexistente é `org.springframework.data.core.PropertyReferenceException`.

**Pronto quando.** Um tamanho gigante é reduzido ao máximo; `sort=nome;drop table x` dá 400 (não 500) e a tabela fica intacta; percorrer todas as páginas não repete nem perde itens; o filtro `%` não devolve tudo.

**Sabotagem.** Remova a lista de campos e mostre o teste falhar.

**Armadilhas clássicas.**
- *Sintoma:* `size` enorme derruba a API. *Causa:* sem máximo configurado.
- *Sintoma:* `sort` com `;` dá 500. *Causa:* o valor chega ao banco, que o recusa. *Correção:* validar contra a lista fixa.
- *Sintoma:* a página dois repete itens da um. *Causa:* sem desempate por id.
- *Sintoma:* `NoClassDefFoundError` da exceção de propriedade. *Causa:* pacote antigo. *Correção:* o pacote do Spring Data 4.

**Entregáveis.** Listagem, lista de campos, testes de paginação.

---

### Módulo 9. Formato da resposta

**Objetivo.** Respostas previsíveis para quem consome.

**Por quê.** O cliente depende do **texto exato** da resposta (ordem dos campos, formato de data); mudar isso sem querer quebra quem consome.

**Decisões.** Só JSON ou mais formatos; se haverá links (HAL).

**Como.**
1. JSON como padrão. Ordem dos campos com `@JsonPropertyOrder` e datas com `@JsonFormat` nos DTOs. **No Boot 4 o Jackson é o 3** (pacote `tools.jackson`), mas as anotações continuam em `com.fasterxml.jackson.annotation`.
2. Só se alguém pedir: XML e YAML (`tools.jackson.dataformat:jackson-dataformat-xml` e `-yaml`; os conversores já vêm no Spring 7) e HAL (`spring-boot-starter-hateoas`).
3. Os **erros são sempre JSON**, mesmo com `Accept` de XML ou YAML. Um `Accept` não suportado responde 406, em JSON.
4. Um teste que compara o **texto exato** do JSON de uma resposta e de um erro.

**Pronto quando.** O texto exato está num teste.

**Sabotagem.** Mude a ordem de um campo e veja o teste falhar.

**Armadilhas clássicas.**
- *Sintoma:* erro de compilação por pacote que não existe. *Causa:* receita de uma versão antiga da biblioteca de JSON.
- *Sintoma:* 500 em toda listagem depois de adicionar links (HATEOAS). *Causa:* `methodOn(...)` cria um proxy do tipo de retorno e um `record` é final. *Correção:* montar os links com `linkTo(...).slash(id)`.
- *Sintoma:* o conversor de YAML só aceita `application/yaml`. *Correção:* configurar o `application/x-yaml` também, se o cliente usar.

**Entregáveis.** Anotações de formato e o teste de texto exato.

---

## Bloco 3. Segurança

### Módulo 10. Autenticação

**Objetivo.** Saber quem chama, sem abrir brecha.

**Por quê.** Sem autenticação, qualquer um lê e apaga; e cada detalhe errado vira brecha: um login que responde diferente revela quais usuários existem; um token sem a conferência do tipo deixa o *refresh* valer como acesso.

**Decisões.** As expirações; a política de senha. **Faça um plano e aprove antes de codificar:** esta etapa deixa a API toda protegida e quebra os testes de contrato por 401 até serem ajustados; trabalhe numa branch e só abra o PR com o build verde.

**Como.**
1. **Dependências:** `spring-boot-starter-security`, `com.auth0:java-jwt:4.4.0`, `spring-security-test` (teste).
2. **Migration** com `permission`, `users` (plural) e `user_permission` (chave composta; apagar o usuário apaga as ligações). Entidades `Permission implements GrantedAuthority` e `User implements UserDetails` (`@ManyToMany(fetch = EAGER)`); `UserService implements UserDetailsService`.
3. **Hash de senha:** BCrypt por um encoder delegante com **só o BCrypt** registrado (recusa `{noop}`, `md5` etc.). O BCrypt só aceita **72 bytes**: valide no cadastro e responda 400.

```java
Map<String, PasswordEncoder> encoders = new HashMap<>();
encoders.put("bcrypt", new BCryptPasswordEncoder());
return new DelegatingPasswordEncoder("bcrypt", encoders);
```

4. **Provedor de JWT** (`JwtTokenProvider`): HS256; acesso **15 min**, *refresh* **24 h**; claims `iss`, `sub`, `roles`, `type` (`access` ou `refresh`), `iat`, `exp`; valida assinatura, emissor, expiração **e o tipo**; exige `exp` e `sub`; **recarrega o usuário do banco** a cada validação e recusa conta desativada, bloqueada ou expirada; segredo de pelo menos 32 bytes, sem padrão. Toda falha vira uma `InvalidTokenException` com mensagem genérica.
5. **Filtro** `JwtTokenFilter extends OncePerRequestFilter`: lê `Authorization: Bearer ...`; token ruim **não derruba** a requisição (segue sem autenticação e a rota protegida responde 401). **Não é `@Component`**: a configuração o cria com `new` (se fosse bean, rodaria duas vezes).
6. **Configuração** stateless (`SessionCreationPolicy.STATELESS`), CSRF desligado (não há cookie), sem formulário nem Basic, e **401 e 403 em JSON** no mesmo `ErrorResponse`, escritos por um `SecurityErrorWriter` (esses erros nascem nos filtros, antes do tratador global).
7. **Endpoints** `POST /auth/signin` (público) e `POST /auth/refresh` (público; o *refresh* **só no corpo**, nunca na URL) e `POST /auth/register` (só ADMIN, permissão padrão `USER`, o corpo **não escolhe** permissões). Respostas com token levam `Cache-Control: no-store`.
8. **Login com a mesma resposta para qualquer falha** (usuário inexistente, senha errada, conta desativada) e com tempo parecido (meça). O `DaoAuthenticationProvider` confere o estado da conta **antes** da senha: trate todas as falhas como a mesma exceção.
9. **Testes:** `JwtTokenProviderTests` (vencido, adulterado, outro emissor, outro segredo, tipo errado nos dois sentidos, troca das `roles`, `alg=none`, sem `exp`, usuário desativado), os testes de filtro e de endpoints **com os filtros ligados**, e os testes de contrato antigos passam a `@AutoConfigureMockMvc(addFilters = false)`, com um comentário avisando que **não testam segurança**.

**Pronto quando.** O login entra; token vencido, adulterado, de outro segredo, com `alg=none`, com papéis alterados e do tipo errado dão 401; desativar o usuário no banco derruba o token na requisição seguinte; `mvn -B clean verify` passa com os segredos **fora** do ambiente.

**Sabotagem.** Tire a conferência do tipo do token e mostre testes falhando; faça o login responder diferente para usuário inexistente.

**Armadilhas clássicas.**
- *Sintoma:* o *refresh* aparece em logs e histórico. *Causa:* foi enviado na URL.
- *Sintoma:* erro estranho com senhas longas. *Causa:* o BCrypt só aceita 72 bytes. *Correção:* validar no cadastro.
- *Sintoma:* o filtro roda duas vezes. *Causa:* foi registrado como bean.
- *Sintoma:* dá para descobrir quais usuários existem. *Causa:* respostas ou tempos diferentes.
- *Sintoma:* o *refresh* vale como acesso. *Causa:* o tipo do token não é conferido.
- *Sintoma:* dezenas de testes de contrato falham com 401. *Correção:* rodá-los sem os filtros (com o aviso) e testar a segurança à parte.
- *Sintoma:* o Spring cria um usuário `user` com senha no log. *Causa:* não há `UserDetailsService`. *Correção:* o `UserService`.

**Entregáveis.** Entidades, hash, provedor, filtro, configuração, endpoints, testes.

---

### Módulo 11. Autorização e CORS

**Objetivo.** Definir **quem pode o quê** e **quais origens** podem chamar a API pelo navegador.

**Por quê.** Estar logado não significa poder tudo: com `authenticated()` no fim, toda rota nova nasceria aberta a qualquer logado; e CORS com `*` deixa qualquer site chamar a API pelo navegador do usuário.

**Decisões.** O modelo de permissões; quais operações são destrutivas e irreversíveis; quais origens (se houver).

**Como.**
1. Regras por rota num **único lugar** (`SecurityConfig`), permissões em constantes (`Authorities.ADMIN`, `Authorities.USER`), **negar no fim**:

```java
.authorizeHttpRequests(rotas -> rotas
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/auth/signin", "/auth/refresh").permitAll()
    .requestMatchers("/auth/register").hasAuthority(Authorities.ADMIN)
    .requestMatchers(HttpMethod.GET, "/<recursos>", "/<recursos>/{id}").hasAnyAuthority(USER, ADMIN)
    .requestMatchers(HttpMethod.HEAD, "/<recursos>", "/<recursos>/{id}").hasAnyAuthority(USER, ADMIN)
    .requestMatchers(HttpMethod.POST, "/<recursos>").hasAnyAuthority(USER, ADMIN)
    .requestMatchers(HttpMethod.PUT, "/<recursos>/{id}").hasAnyAuthority(USER, ADMIN)
    .requestMatchers(HttpMethod.PATCH, "/<recursos>/{id}/disable", "/<recursos>/{id}/enable").hasAnyAuthority(USER, ADMIN)
    .requestMatchers(HttpMethod.DELETE, "/<recursos>/{id}").hasAuthority(Authorities.ADMIN)  // irreversível
    .anyRequest().denyAll())
```

2. A autorização vem **antes** da existência: quem não pode recebe 403 até para um id que não existe (não descobre o que existe).
3. **CORS** em `WebConfig.addCorsMappings`: lista de origens vinda de `cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:}`, **vazia por padrão** (sem CORS), separada por vírgula; métodos da API; cabeçalhos `Authorization, Content-Type, Accept`; **sem credenciais** (`allowCredentials(false)`); `Max-Age` 3600; `Location` exposto. A aplicação **não sobe** com `*` nem com origem inválida (`http(s)://domínio[:porta]`, sem barra nem caminho). Na `SecurityConfig`, `.cors(Customizer.withDefaults())` **e** `OPTIONS` liberado.
4. **Matriz de testes** perfil x rota (anônimo, logado sem permissão, outra permissão, `USER`, `ADMIN`), com os filtros ligados.

**Pronto quando.** A matriz passa; uma rota nova sem regra dá 403 a quem está logado e 401 ao anônimo; permissão revogada no banco vale na requisição seguinte (mesmo com o token antigo); origem permitida passa e a não permitida é barrada.

**Sabotagem.** (1) Deixe o `USER` apagar. (2) Troque `denyAll()` por `authenticated()`. (3) Esqueça a regra do `HEAD`. (4) Remova ao mesmo tempo o `.cors()` e a liberação do `OPTIONS`. Cada caso deve quebrar algum teste.

**Armadilhas clássicas.**
- *Sintoma:* rota nova acessível a qualquer logado. *Causa:* regra final `authenticated()`.
- *Sintoma:* `HEAD` devolve 403 enquanto `GET` funciona. *Causa:* o Spring MVC trata `HEAD` como `GET`, mas a segurança compara o método exato.
- *Sintoma:* o navegador mostra "erro de CORS" quando o servidor devolveu 401. *Causa:* sem `.cors()` na cadeia, os erros de segurança saem **sem** cabeçalhos de CORS e o navegador esconde a mensagem.
- *Sintoma:* o pré-voo vira 401. *Causa:* faltaram **os dois**: `.cors()` e `OPTIONS` liberado (um cobre o outro; a falta de um só passa despercebida).
- *Sintoma:* o pré-voo com cabeçalho proibido misturado volta 200. *Causa:* o Spring devolve só os cabeçalhos permitidos e quem barra é o navegador.
- *Sintoma:* o administrador revogado continua mandando. *Causa:* a permissão foi lida do token em vez do banco.

**Entregáveis.** Regras, matriz de testes, configuração de CORS, anotação da tabela "quem pode o quê".

---

### Módulo 12. Admin inicial e endurecimento

**Objetivo.** Nenhum login conhecido e nenhum caminho sensível aberto.

**Por quê.** Um usuário semeado numa migration vai para o repositório (público) e para a produção com senha conhecida; e as proteções que não são testes se perdem na próxima mudança.

**Decisões.** Como nasce o primeiro administrador; quais caminhos sensíveis conferir.

**Como.**
1. **Nada de usuário semeado com senha conhecida.** Se uma migration semeou, outra o remove **só se a senha ainda for a semeada** (se alguém já trocou, ele é legítimo):

```sql
DELETE FROM users WHERE user_name = 'admin' AND password = '<hash exato da migration que semeou>';
```

2. **Primeiro admin por uma execução controlada:** um `ApplicationRunner` ativado pela variável `ADMIN_BOOTSTRAP_PASSWORD` (desligado por padrão); só cria se **não existir ADMIN**; senha de 12 a 72 bytes, sem espaço ou quebra de linha nas pontas; nome `[A-Za-z0-9._@-]{3,100}`; **falha** (não promove) se já existir usuário comum com o nome; **nunca registra a senha**. Suba uma vez e **remova a variável**.
3. **Guardas na esteira**, testes que **leem os arquivos reais**: sem segredo com valor padrão; sem segredo em `environment` da definição da tarefa (tem de estar em `secrets`, com ARN do Secrets Manager); só uma migration pode semear usuário; o hash da migration de remoção é igual ao da que semeou; `*` proibido no CORS; Swagger e bootstrap desligados por padrão.
4. **Caminhos sensíveis** (`/actuator/env`, `/actuator/beans`, `/actuator/heapdump`, `/h2-console`, `/.env`, `/.git/config`, `/application.yml`, `/WEB-INF/web.xml`, `/error`): 401 ao anônimo e **403 até ao ADMIN**; `POST /actuator/shutdown` também.
5. **Erros que não vazam** pilha, nome de classe, pacote nem caminho de arquivo; cabeçalhos de segurança presentes (`nosniff`, `X-Frame-Options: DENY`, sem `X-Powered-By`).

**Pronto quando.** Um banco novo termina sem usuário conhecido; subir com a variável cria o administrador; subir de novo com a variável **não** recria nem troca a senha; sem a variável o login continua funcionando.

**Sabotagem.** Escreva a senha no log; coloque um segredo em `environment`; mostre os testes falhando.

**Armadilhas clássicas.**
- *Sintoma:* existe um login conhecido em produção. *Causa:* o usuário semeado teve a senha mostrada ou guardada em arquivo.
- *Sintoma:* a aplicação cai poucos instantes depois de "Started". *Causa:* o `ApplicationRunner` roda depois de o servidor abrir; configuração errada o derruba logo em seguida.
- *Sintoma:* a senha do administrador "não confere". *Causa:* quebra de linha no valor da variável.

**Entregáveis.** O runner, a migration de remoção, os guardas, os testes de caminhos sensíveis.

---

### Módulo 13. Limites de abuso e ciclo de vida do token

**Objetivo.** Reduzir força bruta e dar controle sobre as sessões. *(O projeto de referência ficou devendo este módulo: foi anotado como limitação conhecida.)*

**Por quê.** Sem limite, alguém tenta milhares de senhas (medimos 40 tentativas erradas em paralelo, todas respondidas, sem bloqueio); e um token roubado vale até vencer, porque não existe logout.

**Decisões.** Os limites e o desbloqueio; revogação por rotação ou por lista; troca de senha. **Faça um plano e aprove antes de codificar:** há decisões de produto.

**Como.**
1. Limite de tentativas de login **por usuário e por origem** (429 com `Retry-After`), com armazenamento compartilhado se houver mais de uma instância (o contador em memória não vale).
2. *Refresh* com **rotação** (cada uso emite um novo e invalida o antigo) ou **lista de revogação**; endpoint de logout.
3. Troca de senha pelo próprio usuário, que invalida os tokens antigos.
4. Atualize o Swagger e os testes.

**Pronto quando.** Tentativas em excesso recebem 429; um *refresh* revogado dá 401; trocar a senha invalida os tokens antigos.

**Sabotagem.** Remova a revogação e mostre o teste do *refresh* revogado falhar.

**Armadilhas clássicas.**
- *Sintoma:* o limite funciona em teste e não em produção. *Causa:* contador em memória com mais de uma instância.
- *Sintoma:* um atacante bloqueia o usuário legítimo de propósito. *Causa:* bloqueio só por usuário. *Correção:* combinar com a origem e ter desbloqueio.
- *Sintoma:* o limite revela quais usuários existem. *Correção:* responder igual para existente e inexistente.

**Entregáveis.** Limite, revogação, troca de senha, testes e documentação da API.

---

## Bloco 4. Operação

### Módulo 14. Falha de infraestrutura

**Objetivo.** Quando uma dependência cai, a API responde **rápido** e com o **código certo**, e volta sozinha.

**Por quê.** Com o banco fora do ar a API respondia 401 (parecia token vencido) ou 500, depois de 30 s, e o cliente tentava logar de novo à toa; e um `health` que depende do banco faz o orquestrador reiniciar a tarefa em laço.

**Decisões.** O tempo máximo de espera por uma conexão; o que o orquestrador consulta; quais exceções contam como "banco fora do ar".

**Como.**
1. **Actuator** (`spring-boot-starter-actuator`) expondo **só o `health`**, sem detalhes, com *liveness* e *readiness*:

```yaml
management:
  endpoints: { web: { exposure: { include: health } } }
  endpoint:  { health: { probes: { enabled: true }, show-details: never, show-components: never } }
```

   `GET /actuator/health/**` público e **só de leitura** (`permitAll`); o resto do `/actuator` fica negado. O corpo do `health` traz também `groups: ["liveness","readiness"]` (só os nomes).
2. **Timeout curto de conexão:** `spring.datasource.hikari.connection-timeout: 5000` (o padrão é 30 s).
3. **Classificador** que reconhece, pela **cadeia de causas**, que o banco está fora do ar:

```java
List.of(CannotCreateTransactionException.class, CannotGetJdbcConnectionException.class,
        DataAccessResourceFailureException.class, QueryTimeoutException.class, JDBCConnectionException.class,
        SQLTransientConnectionException.class, SQLNonTransientConnectionException.class)
// bancoIndisponivel(Throwable): percorre getCause() e devolve true se algum for desses tipos
```

   Use-o em **três lugares**: no tratador global (`ServicoIndisponivelException` vira **503 com `Retry-After: 5`**), **no filtro do JWT** (que recarrega o usuário do banco) e no **login**. Erro de commit e outros erros **não** contam: continuam 500.
4. O **liveness** não depende do banco; é o que o orquestrador e o balanceador consultam. O `health` completo inclui o banco (503 `DOWN` quando ele cai).
5. Logs sem segredo e sem dado pessoal.

**Pronto quando.** Derrubar o banco de verdade (contêiner descartável) devolve 503 em poucos segundos em `GET` autenticado, login e `health`; o *liveness* segue UP; e a API volta sozinha quando o banco volta.

**Sabotagem.** Volte o timeout ao padrão e mostre o teste falhar; remova o tratamento do filtro e veja o 401 enganoso reaparecer.

**Armadilhas clássicas.**
- *Sintoma:* 401 (parece token vencido) com o banco fora, depois de 30 s. *Causa:* o filtro recarrega o usuário do banco e a exceção escapa; o timeout padrão do pool é longo. *Correção:* classificar e responder 503, com timeout curto.
- *Sintoma:* o orquestrador derruba a aplicação quando o banco cai. *Causa:* o verificador consultado inclui o banco. *Correção:* apontar para o *liveness*.
- *Sintoma:* `healthCheck` dentro do contêiner falha. *Causa:* a imagem `eclipse-temurin:*-jre` não tem `curl` nem `wget`. *Correção:* usar o verificador do balanceador, ou instalar a ferramenta.
- *Sintoma:* `/actuator/health/../env` retorna algo. *Causa:* nenhuma: o firewall do Spring Security recusa o `..` com 400; teste mesmo assim.

**Entregáveis.** O classificador, a configuração do `health`, o teste de queda do banco e a anotação do timeout escolhido.

---

### Módulo 15. Contêiner

**Objetivo.** Uma imagem enxuta e segura.

**Por quê.** A imagem é o que de fato vai para a produção: rodar como root dá poder demais a quem invadir, e uma imagem com o JDK e o código-fonte é maior e expõe mais.

**Decisões.** A imagem base (aqui, `eclipse-temurin:25-jdk` para compilar e `eclipse-temurin:25-jre` para rodar).

**Como.**
1. `Dockerfile` em **duas fases** (modelo no apêndice D3): a de build copia só `mvnw`, `.mvn` e `pom.xml` e baixa as dependências (camada em cache), depois copia `src` e compila com `-DskipTests`; a de execução tem só o JRE e o jar, cria um usuário comum e roda com `USER app`. O `sed -i 's/\r$//' mvnw` remove o CRLF do Windows.
2. `.dockerignore` (`target/`, `.git/`, arquivos de IDE).
3. `docker-compose.yml` só para dar nome à imagem (`image: <usuario>/<api>`) e mapear a porta. O CI passa a rodar `docker compose build` em todo PR.
4. Os **testes rodam antes** (no CI), não dentro do build da imagem.

**Pronto quando.** `docker build` e `docker run -p 8080:8080` respondem; o processo **não** é root (`docker exec <contêiner> id`).

**Sabotagem.** Remova a linha do usuário e confira que o `id` mostra root; restaure.

**Armadilhas clássicas.**
- *Sintoma:* o script do wrapper não roda no Linux. *Causa:* CRLF do Windows. *Correção:* remover no build.
- *Sintoma:* comportamento diferente entre a máquina e a imagem. *Causa:* versões de Java diferentes.
- *Sintoma:* o ECR lista 3 itens por envio. *Causa:* o Docker novo acrescenta o índice e o atestado de proveniência. *Correção:* é normal.
- *Sintoma:* `docker compose build` diz "no configuration file provided". *Causa:* rodou fora da pasta do `docker-compose.yml`.

**Entregáveis.** Dockerfile, `.dockerignore`, compose, ajuste do CI.

---

### Módulo 16. QA da imagem de produção

**Objetivo.** Atacar o artefato que vai para a produção **antes** de ele ir.

**Por quê.** Teste unitário prova que o código faz o que você **pensou**, não o que um atacante ou a concorrência fazem; no nosso QA, 412 verificações acharam 6 defeitos que os 294 testes da época não pegaram.

**Decisões.** O que cobrir; como isolar o ambiente.

**Como.**
1. **Ambiente isolado e descartável** (não toque na nuvem nem no banco de desenvolvimento):

```bash
docker network create qa-net
docker run -d --name qa-mysql --network qa-net -e MYSQL_ROOT_PASSWORD="$SENHA_QA" mysql:8.4
docker build -t qa-<api> <pasta-do-projeto>/   # a imagem de produção
docker run -d --name qa-app --network qa-net -p 8081:8080 \
  -e DB_USER=root -e DB_URL='jdbc:mysql://qa-mysql:3306/<api>?createDatabaseIfNotExist=true' \
  -e DB_PASSWORD="$SENHA_QA" -e JWT_SECRET="$(openssl rand -hex 32)" \
  -e ADMIN_BOOTSTRAP_PASSWORD="$SENHA_ADMIN_QA" qa-<api>
```

2. **Cubra:** matriz de autorização por perfil; ataques ao token (`alg=none`, outro segredo, payload alterado, vencido, tipo errado, sem `exp`, forjado dizendo ADMIN); cadastro (limites, bytes do BCrypt, campos extras); injeção em login, filtros e `sort`; limites de todos os campos; ids estranhos (0, negativo, `Long.MAX`, estouro); paginação, ordenação e filtros; negociação de conteúdo; corpo de vários MB; CORS; erros sem vazar detalhes; **concorrência** (POST iguais, cadastros iguais, `DELETE` do mesmo recurso, PUTs cruzados); persistência e reinício; troca do `JWT_SECRET`; segredos ausentes (a aplicação não sobe); banco derrubado e recuperação; desligamento limpo; segredos, senhas e tokens **fora dos logs**.
3. **Método:** uma bateria de requisições HTTP (inclusive em nível de *socket* para o que o cliente comum não envia: cabeçalhos duplicados, `TRACE`, URLs malformadas) e uma fase de banco, logs, reinício e queda do banco (`docker stop qa-mysql`).
4. Classifique cada falha como **defeito real** ou **expectativa errada**; corrija cada defeito (com teste e sabotagem) e **refaça o QA** na imagem corrigida.
5. **Limpe tudo** no fim: `docker rm -f qa-app qa-mysql; docker network rm qa-net; docker rmi qa-<api>`, e apague os arquivos com senha.

**Pronto quando.** Nenhuma resposta 5xx para entrada inválida e nenhum segredo nos logs; os defeitos achados foram corrigidos e o QA refeito.

**Sabotagem.** O próprio QA é a sabotagem do conjunto: se ele não achar nada, desconfie da bateria.

**Armadilhas clássicas.**
- *Sintoma:* a bateria "passa" tudo. *Causa:* as expectativas foram escritas só para o que já funciona. *Correção:* inclua casos hostis.
- *Sintoma:* o QA mexeu no banco de desenvolvimento. *Causa:* o ambiente de teste não era isolado.
- *Sintoma:* sobram contêineres, redes e arquivos com senha. *Correção:* limpeza obrigatória.
- *Sintoma:* um "defeito" que é erro do script de teste (por exemplo, a data "de ontem" calculada em UTC, que à noite é "hoje" no fuso local). *Correção:* investigar antes de corrigir o código.

**Entregáveis.** O relatório de defeitos (severidade, como reproduzir, causa), as correções e o QA refeito.

---

## Bloco 5. Entrega

### Módulo 17. Nuvem (AWS)

**Objetivo.** Infraestrutura mínima, segura e com custo controlado.

**Por quê.** A ordem importa e cada erro custa horas: a política de leitura do segredo criada na role errada derruba a tarefa; a tarefa sem os segredos novos nem sobe; e sem HTTPS a senha e o token trafegam em texto puro.

**Decisões.** Região; nomes dos recursos; quem executa (**o console é de uma pessoa, não do agente**); domínio e HTTPS.

**Como.** Siga esta ordem. (Os detalhes de tela, que mudam com o tempo, ficam no `Duvida-CICD.md`.)

1. **Banco (RDS):** *Aurora and RDS → Create database → Full configuration*, **MySQL 8.4**, template **Sandbox**, **Single-AZ**, instância **`db.t4g.micro`**, **20 GiB** (gp3), **Self managed** credentials, usuário `root` com senha forte (que **não** vai para o Git). **Confira o *Estimated monthly costs* antes de criar** (os padrões custam cerca de US$ 145/mês). *Public access*: **No** em produção; **Yes** só se você precisar de acesso do seu computador, com regra por **My IP**. O database é criado pela própria aplicação (`createDatabaseIfNotExist=true`).
2. **ECR:** repositório **privado** `<repositorio>` (o nome não pode ser alterado depois e reaparece na Task Definition, no `docker push` e no workflow).
3. **Usuário IAM da esteira**, sem acesso ao console, com `AmazonEC2ContainerRegistryFullAccess` e `AmazonECS_FullAccess` (amplas; para produção real, restrinja), e uma **chave de acesso** (anote a secret na hora: ela não aparece de novo).
4. **Cluster ECS** (AWS Fargate).
5. **Segredos (Secrets Manager)**, tipo *Other type of secret → Plaintext*, só o valor (sem aspas, espaço ou quebra de linha): senha do banco, **segredo do JWT** (`openssl rand -hex 32`, **não** reaproveite o do ambiente local) e, para o primeiro deploy, a senha do admin inicial.
6. **Permissão de leitura:** *IAM → Roles →* a **Task execution role** da tarefa (por exemplo `ecsTaskExecutionRole`) *→ Create inline policy*, **nela e não em outra role**:

```json
{ "Version": "2012-10-17",
  "Statement": [{ "Effect": "Allow", "Action": "secretsmanager:GetSecretValue",
    "Resource": [ "arn:aws:secretsmanager:<regiao>:<id-da-conta>:secret:<prefixo>/db-password-*",
                  "arn:aws:secretsmanager:<regiao>:<id-da-conta>:secret:<prefixo>/jwt-secret-*" ] }] }
```

   Mantenha o `-*` no fim (a AWS acrescenta um sufixo ao ARN).
7. **Security groups:** o da aplicação (porta 8080, origem **My IP** enquanto não houver balanceador); no grupo do RDS, **3306 só para o grupo da aplicação** (e, se precisar, para o seu IP `/32`). **Nunca** `0.0.0.0/0` na 3306.
8. **Task Definition** (Fargate, Linux/X86_64, `awsvpc`, 1 vCPU, 3 GB): contêiner na porta **8080**; `environment` com `DB_USER`, `DB_URL` e `TZ` (valores não secretos); `secrets` com `DB_PASSWORD` e `JWT_SECRET` como **`ValueFrom` = ARN completo**; logs `awslogs`. Exporte o JSON para `task-definition.json` no repositório (sem os campos gerados pela AWS: `taskDefinitionArn`, `revision`, `status`, `requiresAttributes`, `compatibilities`, `registeredAt`, `registeredBy`). **Um ARN inventado quebra o deploy.**
9. **Primeira imagem no ECR, antes do serviço:**

```bash
aws configure --profile <perfil>
aws ecr get-login-password --region <regiao> --profile <perfil> | docker login --username AWS --password-stdin <id-da-conta>.dkr.ecr.<regiao>.amazonaws.com
docker tag <imagem>:latest <id-da-conta>.dkr.ecr.<regiao>.amazonaws.com/<repositorio>:latest
docker push <id-da-conta>.dkr.ecr.<regiao>.amazonaws.com/<repositorio>:latest
```

   Rode os comandos **um de cada vez** e espere `Login Succeeded` antes do push.
10. **Service ECS** (*Desired tasks* = 1): estratégia de capacidade `FARGATE`; na rede, **remova as sub-redes de Local Zone** (o Fargate não funciona nelas), troque o security group `default` pelo da aplicação e ligue **Public IP** (só enquanto não houver balanceador).
11. **Teste pelo IP** da tarefa (aba *Tasks* do cluster): `http://<IP>:8080/<recursos>` deve dar 401 sem token.
12. **Para produção real (o projeto de referência não fez):** usuário de banco com privilégio **só no schema da aplicação** (nunca `root`); **ALB com certificado ACM e domínio** (HTTPS), com o *target group* apontando para `/actuator/health/liveness`; mais de uma tarefa.
13. **Ligar e desligar:** *Desired tasks* = 0 e RDS *Stop temporarily*; o RDS parado **religa sozinho após 7 dias**. Ao religar, **espere o RDS ficar `Available`** antes de qualquer deploy.

**Pronto quando.** A API responde (por HTTPS, em produção real) e o banco **não** é acessado como `root`.

**Sabotagem.** De fora da rede permitida, tente alcançar a porta 3306 do banco: deve falhar.

**Armadilhas clássicas.**
- *Sintoma:* a tarefa para com `AccessDeniedException ... secretsmanager:GetSecretValue`. *Causa:* a política foi criada em outra role. *Correção:* criá-la na role de **execução** da tarefa e forçar novo deploy (*Update → Force new deployment*).
- *Sintoma:* a tarefa nem sobe. *Causa:* falta um segredo novo na definição, ou há um ARN inventado.
- *Sintoma:* a senha chega à aplicação como o próprio ARN. *Causa:* o tipo da variável é `Value` em vez de `ValueFrom`.
- *Sintoma:* `CannotPullContainerError`. *Causa:* o serviço foi criado antes de a imagem existir.
- *Sintoma:* "CIDR block, a security group ID ... has to be specified" ao salvar a regra. *Causa:* o campo *Source* recebeu só o texto do nome. *Correção:* escolher o grupo na lista suspensa ou colar o `sg-...`.
- *Sintoma:* custo alto. *Causa:* os padrões da criação do banco. *Correção:* conferir a estimativa antes.
- *Sintoma:* senha e token em texto puro na rede. *Causa:* só HTTP. *Correção:* HTTPS.

**Entregáveis.** O roteiro numerado (para a pessoa executar), a definição da tarefa **sem senha**, e as diferenças de tela anotadas.

---

### Módulo 18. Esteira de deploy

**Objetivo.** O merge na `main` publica a imagem e troca a tarefa.

**Por quê.** Deploy manual é esquecível e desigual; automático, só na `main` e só depois dos testes verdes, garante que o que roda em produção é o que passou, e o PR nunca enxerga os segredos.

**Decisões.** Quais eventos disparam o quê; onde ficam as credenciais da nuvem.

**Como.**
1. Cadastre no GitHub (*Settings → Secrets and variables → Actions*) `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` do usuário da esteira.
2. Estenda o workflow do módulo 1 (modelo completo no apêndice D4) com passos **só em `push`** (`if: github.event_name == 'push'`): login na AWS e no ECR, `docker tag` e `docker push` com a tag do **commit** e `latest`, `amazon-ecs-render-task-definition` (troca a imagem do contêiner) e `amazon-ecs-deploy-task-definition` com `wait-for-service-stability: true`.
3. `permissions: contents: read` e `paths-ignore: '**.md'`.
4. A esteira **não altera o *Desired tasks***.

**Pronto quando.** Um merge de teste na `main` troca a tarefa e a rota nova responde (só com o ambiente ligado e o RDS `Available`).

**Sabotagem.** Abra um PR e confirme que os passos de deploy **não** rodam e que os segredos não aparecem.

**Armadilhas clássicas.**
- *Sintoma:* a esteira termina verde e a API não responde. *Causa:* *Desired tasks* = 0 (a esteira não o altera), ou o IP mudou.
- *Sintoma:* o IP da API muda a cada deploy. *Causa:* o IP público é da tarefa. *Correção:* balanceador com nome estável.
- *Sintoma:* o deploy parou depois de mexerem no workflow. *Correção:* mudanças no workflow só com pedido e revisão.

**Entregáveis.** O workflow, os *secrets* cadastrados, a anotação do que dispara o quê.

---

### Módulo 19. Release

**Objetivo.** Levar uma versão para a produção sem surpresa.

**Por quê.** O deploy pode falhar porque o banco estava parado, e o rollback deixa o código antigo no ar com tudo "verde"; só conferir por uma rota nova mostra a verdade.

**Decisões.** O número da versão (`X.Y.Z`); o que entra; a janela.

**Como.**

```bash
git fetch origin && git checkout -b release/X.Y.Z origin/develop && git push -u origin release/X.Y.Z
gh pr create --base main --head release/X.Y.Z --title "Release X.Y.Z" --body "..."
gh pr checks release/X.Y.Z --watch
# PARE: peça ao responsável o "pode" e a confirmação de que o RDS está Available e o Service tem uma tarefa
gh pr merge <n> --merge                      # merge commit; a esteira faz o deploy
gh run watch <id-da-execucao> --exit-status
aws ecs describe-services --cluster <cluster> --services <service> \
  --query 'services[0].[taskDefinition,deployments[].[rolloutState,runningCount]]' --output text
# verifique PELO COMPORTAMENTO: rota nova responde, protegidas dão 401, o que está desligado dá 401
git tag -a vX.Y.Z origin/main -m "Release X.Y.Z" && git push origin vX.Y.Z
git checkout -b feature/back-merge-X.Y.Z origin/develop && git merge origin/main   # e PR para develop
```

Registre nas anotações (`PassoAPasso.md`, `memoria.md`, `Duvida-CICD.md`).

**Pronto quando.** A rota nova responde em produção, e a tag e o *back-merge* existem.

**Sabotagem.** Antes da primeira release real, ensaie o procedimento do runbook (apêndice D8) num ambiente de teste.

**Armadilhas clássicas.**
- *Sintoma:* o deploy falha com *circuit breaker* e tarefas saindo com código 1. *Causa:* o RDS estava parado ou subindo (a aplicação exige o banco no arranque). *Correção:* esperar o banco e reexecutar o job (`gh run rerun <id> --failed`).
- *Sintoma:* "tudo verde" e a rota nova não existe. *Causa:* o rollback deixou a **versão anterior** no ar (reusa o digest da implantação anterior, mesmo que o `latest` do ECR já aponte para a nova). *Correção:* verificar pelo comportamento.
- *Sintoma:* o PR de *back-merge* fica `BLOCKED` com o build "verde". *Causa:* o *head* do PR é o commit da `main`, cujo check `build` é o deploy que falhou. *Correção:* reexecutar o job; o PR destrava.
- *Sintoma:* o responsável acha uma coisa e o ambiente está em outra. *Causa:* confiou-se numa leitura parcial (`runningCount` 1 pode ser de uma tarefa que cai e é recriada). *Correção:* pedir a confirmação.

**Entregáveis.** A release, a tag, o *back-merge* e a anotação do que aconteceu.

---

### Módulo 20. Operação

**Objetivo.** A operação deixa de depender de memória.

**Por quê.** Sem alarmes você descobre a queda pelo usuário; sem backup testado não existe backup; e sem anotar, o mesmo erro volta. *(O projeto de referência não chegou a montar alarmes nem testar backups.)*

**Decisões.** O que alarmar; por quanto tempo guardar logs; política de backup; quem é avisado.

**Como.**
1. **Alarmes (CloudWatch):** tarefa caindo (o `Service` abaixo do desejado), respostas 5xx, banco (conexões, armazenamento), custo (*Budgets*).
2. **Logs** no CloudWatch (`/ecs/<task-definition>`), com **retenção** definida. O perfil da CLI da esteira não lê o CloudWatch Logs: use o console (*ECS → tarefa → Logs*).
3. **Backups do RDS** ligados, **com teste de restauração** (restaure para uma instância nova e confira os dados).
4. **Varredura de dependências e da imagem** na esteira (por exemplo, Dependabot e a varredura do ECR).
5. **Runbook** (apêndice D8): ligar e desligar, deploy falhou, rollback, banco fora, rotação de segredos.

**Pronto quando.** Outra pessoa consegue operar só com o runbook, e o próximo release sai sem surpresa.

**Sabotagem.** Derrube o banco de propósito em ambiente de teste e confira que o alarme dispara.

**Armadilhas clássicas.**
- *Sintoma:* o ambiente "ligou sozinho" e gerou custo. *Causa:* o RDS parado religa sozinho após 7 dias. *Correção:* anotar a data e o procedimento de parar de novo.
- *Sintoma:* o backup existe e a restauração falha. *Causa:* nunca foi testada.
- *Sintoma:* o mesmo erro volta. *Causa:* a lição não foi anotada.

**Entregáveis.** Alarmes, política de logs e de backup, runbook, anotações.

---

## Parte D. Apêndices

### D1. Dependências (`pom.xml`)

```xml
<parent>spring-boot-starter-parent 4.1.x</parent>
<!-- principais -->
spring-boot-starter-webmvc, spring-boot-starter-validation, spring-boot-starter-data-jpa,
spring-boot-starter-security, spring-boot-starter-actuator, spring-boot-starter-flyway,
org.flywaydb:flyway-mysql, com.mysql:mysql-connector-j (runtime), com.auth0:java-jwt:4.4.0,
org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0,
spring-boot-devtools (runtime, optional)
<!-- só se pedir: HAL / XML / YAML -->
spring-boot-starter-hateoas, tools.jackson.dataformat:jackson-dataformat-xml, ...-yaml
<!-- teste -->
spring-boot-starter-webmvc-test, com.h2database:h2, spring-security-test,
spring-boot-testcontainers, org.testcontainers:testcontainers-junit-jupiter,
org.testcontainers:testcontainers-mysql, io.rest-assured:rest-assured:6.0.0
```

### D2. `application.yml` (modelo)

```yaml
spring:
  application:
    name: <api>
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${DB_URL:jdbc:mysql://localhost:3307/<api>?createDatabaseIfNotExist=true}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD}                 # sem padrão
    hikari:
      connection-timeout: 5000
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  data:
    web:
      pageable: { default-page-size: 20, max-page-size: 50 }

security:
  jwt:
    secret: ${JWT_SECRET}                    # sem padrão; mínimo 32 caracteres
    issuer: <api>
    access-token-expiration: 15m
    refresh-token-expiration: 24h

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:}  # vazio = sem CORS; nunca '*'

app:
  admin-bootstrap:
    username: ${ADMIN_BOOTSTRAP_USERNAME:admin}
    password: ${ADMIN_BOOTSTRAP_PASSWORD:}   # vazio = desligado

springdoc:
  api-docs: { enabled: "${SWAGGER_ENABLED:false}" }
  swagger-ui: { enabled: "${SWAGGER_ENABLED:false}", persist-authorization: true }

management:
  endpoints: { web: { exposure: { include: health } } }
  endpoint: { health: { probes: { enabled: true }, show-details: never, show-components: never } }
```

### D3. Dockerfile e `docker-compose.yml`

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

```yaml
services:
  app:
    build: .
    image: <usuario>/<api>
    ports:
      - "8080:8080"
```

### D4. Workflow do GitHub Actions

```yaml
name: CI/CD
on:
  push:
    branches: [ "main" ]
    paths-ignore: [ '**.md' ]
  pull_request:
    branches: [ "main", "develop", "release/**" ]
permissions:
  contents: read
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v6
      with: { distribution: temurin, java-version: '25', cache: maven }
    - name: Build and test with Maven
      working-directory: <pasta-do-projeto>
      run: mvn -B verify
    - name: Docker Compose Build
      working-directory: <pasta-do-projeto>
      run: docker compose build
    # --- só em push na main ---
    - name: Configure AWS credentials
      if: github.event_name == 'push'
      uses: aws-actions/configure-aws-credentials@v4
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: <regiao>
    - name: Login to Amazon ECR
      if: github.event_name == 'push'
      id: ecr-login
      uses: aws-actions/amazon-ecr-login@v2
    - name: Push to Amazon ECR
      if: github.event_name == 'push'
      run: |
        ECR_IMAGE=${{ steps.ecr-login.outputs.registry }}/<repositorio>
        docker tag <usuario>/<api>:latest $ECR_IMAGE:${{ github.sha }}
        docker tag <usuario>/<api>:latest $ECR_IMAGE:latest
        docker push $ECR_IMAGE:${{ github.sha }}
        docker push $ECR_IMAGE:latest
    - name: Render ECS task definition
      if: github.event_name == 'push'
      id: render-task
      uses: aws-actions/amazon-ecs-render-task-definition@v1
      with:
        task-definition: <pasta-do-projeto>/task-definition.json
        container-name: <nome-do-conteiner>
        image: ${{ steps.ecr-login.outputs.registry }}/<repositorio>:${{ github.sha }}
    - name: Deploy to Amazon ECS
      if: github.event_name == 'push'
      uses: aws-actions/amazon-ecs-deploy-task-definition@v2
      with:
        task-definition: ${{ steps.render-task.outputs.task-definition }}
        service: <service>
        cluster: <cluster>
        wait-for-service-stability: true
```

### D5. `task-definition.json` (esqueleto, sem senha)

```json
{
  "family": "<task-definition>",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024", "memory": "3072",
  "executionRoleArn": "arn:aws:iam::<id-da-conta>:role/ecsTaskExecutionRole",
  "runtimePlatform": { "cpuArchitecture": "X86_64", "operatingSystemFamily": "LINUX" },
  "containerDefinitions": [{
    "name": "<nome-do-conteiner>",
    "image": "<id-da-conta>.dkr.ecr.<regiao>.amazonaws.com/<repositorio>:latest",
    "essential": true,
    "portMappings": [{ "containerPort": 8080, "hostPort": 8080, "protocol": "tcp" }],
    "environment": [
      { "name": "DB_USER", "value": "root" },
      { "name": "TZ", "value": "America/Sao_Paulo" },
      { "name": "DB_URL", "value": "jdbc:mysql://<endpoint-do-rds>:3306/<api>?createDatabaseIfNotExist=true" }
    ],
    "secrets": [
      { "name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:<regiao>:<id-da-conta>:secret:<prefixo>/db-password-XXXXXX" },
      { "name": "JWT_SECRET",  "valueFrom": "arn:aws:secretsmanager:<regiao>:<id-da-conta>:secret:<prefixo>/jwt-secret-XXXXXX" }
    ],
    "logConfiguration": { "logDriver": "awslogs", "options": {
      "awslogs-group": "/ecs/<task-definition>", "awslogs-region": "<regiao>",
      "awslogs-stream-prefix": "ecs", "awslogs-create-group": "true" } }
  }]
}
```

### D6. Comandos úteis da AWS CLI (leitura)

```bash
# estado do serviço e do rollout
aws ecs describe-services --cluster <cluster> --services <service> \
  --query 'services[0].[desiredCount,runningCount,taskDefinition,deployments[].[status,rolloutState]]' --output text
# eventos recentes (causa de tarefa que não sobe)
aws ecs describe-services --cluster <cluster> --services <service> --query 'services[0].events[0:10].[createdAt,message]' --output text
# tarefas paradas (motivo e código de saída)
aws ecs list-tasks --cluster <cluster> --desired-status STOPPED
aws ecs describe-tasks --cluster <cluster> --tasks <arn> --query 'tasks[0].[stoppedReason,containers[0].exitCode]'
# IP público da tarefa (via interface de rede)
aws ecs describe-tasks --cluster <cluster> --tasks <arn> --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text
aws ec2 describe-network-interfaces --network-interface-ids <eni> --query 'NetworkInterfaces[0].Association.PublicIp' --output text
# imagens no ECR
aws ecr describe-images --repository-name <repositorio> --query 'sort_by(imageDetails,&imagePushedAt)[-5:].[imagePushedAt,imageTags]' --output text
```

### D7. Checklist de release

- [ ] Suíte inteira verde com os segredos **fora** do ambiente; também com `-Dsurefire.runOrder=random`.
- [ ] `docker compose build` ok (dentro da pasta do compose).
- [ ] Migration nova validada no banco real.
- [ ] Se a definição da tarefa ou as variáveis mudam: os segredos e as políticas já existem.
- [ ] Documentação da API (Swagger) atualizada.
- [ ] Responsável confirmou: **RDS `Available`** e Service com uma tarefa.
- [ ] Depois do deploy: rota nova responde; protegidas dão 401; Swagger desligado dá 401; `/actuator/health` UP.
- [ ] Tag, *back-merge* e anotações.

### D8. Runbook de incidentes

**Deploy falhou com "rolled back by the deployment circuit breaker"**
1. Veja os eventos do serviço e as tarefas paradas (D6). Código de saída 1 logo na subida: a aplicação não conseguiu iniciar.
2. Causa mais comum: o RDS parado ou subindo. Confirme `Available`.
3. O rollback deixa a **versão anterior** no ar. Confirme a versão por uma rota que só existe na nova.
4. Com o ambiente certo: `gh run rerun <id> --failed`. Um PR de *back-merge* bloqueado destrava quando o job passar.

**Banco fora do ar**
1. A API deve responder 503 em poucos segundos e o *liveness* deve seguir UP. Se não, revise o módulo 14.
2. Religue o banco e espere `Available`. A API volta sozinha; se não voltar, reinicie a tarefa (*Force new deployment*).
3. Registre a causa e o tempo de recuperação.

**Rotação de um segredo**
1. Crie o segredo novo, atualize o `valueFrom` no `task-definition.json` e libere a leitura na política da role de execução.
2. Faça o deploy. Se for o `JWT_SECRET`, os tokens antigos deixam de valer (todos precisam logar de novo).
3. Apague o segredo antigo.

**Ligar e desligar o ambiente**
- Desligar: *Desired tasks* = 0 no Service; RDS *Stop temporarily* (religa sozinho em 7 dias; anote a data).
- Ligar: RDS *Start* e espere `Available`; depois *Desired tasks* = 1; pegue o IP novo.

### D9. Baseline de segurança (para o QA)

Anônimo recebe 401 em todas as rotas protegidas; sem permissão recebe 403; rota sem regra é negada; `alg=none`, outro segredo, papéis alterados, vencido e tipo errado são recusados; o login responde igual para qualquer falha e em tempo parecido; nenhum campo extra do cliente vira privilégio; injeção em login, filtros e ordenação não tem efeito; erros não vazam pilha, classe, SQL nem caminho; segredos, senhas e tokens não aparecem nos logs; CORS de origem não liberada não recebe cabeçalhos de permissão; caminhos sensíveis são negados até ao administrador; o `health` não mostra detalhes; a aplicação não sobe com segredo ausente ou curto.

### D10. Controle de custo

- Conferir a estimativa antes de criar qualquer recurso (os padrões do RDS são caros).
- O que cobra parado: armazenamento do banco, imagens no ECR, segredos.
- Como pausar: *Desired tasks* = 0 e parar o RDS; lembrar que ele religa sozinho depois de 7 dias.
- Alerta de orçamento (*Budgets*) configurado.

---

## Como validar este playbook

1. Crie uma **API pequena** (dois recursos) seguindo **só** este documento.
2. Cada vez que precisar de algo que não está escrito, ou que está escrito errado, **anote a lacuna** e corrija o módulo.
3. Repita o QA do módulo 16 na API nova: se ele achar defeitos que o playbook deveria ter evitado, a armadilha correspondente está faltando.
