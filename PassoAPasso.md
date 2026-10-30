# Passo a passo

Registro, na ordem em que foram feitos, dos passos seguidos neste projeto.

## 1. Gerar o projeto no Spring Initializr

- **Status:** concluído
- Maven, Java 25 (LTS), Spring Boot 4.1.2.
- Grupo `br.com.vitor`, artefato `apicrud-alunos`.
- Dependências: Spring Web (`spring-boot-starter-webmvc`) e Spring Boot DevTools.
- `java.version` no `pom.xml` = 25, igual ao JDK instalado (25.0.4).
- Verificado: `.\mvnw clean package` passa (1 teste) e gera `target/apicrud-alunos-0.0.1-SNAPSHOT.jar`.
- O Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`) veio do Initializr e foi mantido.
- Commit: `69152c9` – Configurações iniciais.

## 2. `application.properties` → `application.yml`

- **Status:** concluído
- `src/main/resources/application.properties` renomeado para `application.yml`:

  ```yaml
  spring:
    application:
      name: apicrud-alunos
  ```

- Commit: `69152c9` – Configurações iniciais.

## 3. Estrutura de camadas

- **Status:** pastas criadas localmente, ainda sem commit
- Pacote base: `src/main/java/br/com/vitor/apicrud_alunos/`.
- Pastas: `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `exception`, `config`, `security`. Todas vazias, exceto `exception` (passo 4).
- O Git não guarda pastas vazias; elas só vão para o repositório com a primeira classe de cada uma.
- Segundo o playbook, nesta fase entram só `controller`, `service`, `model`, `exception`, `dto` e `mapper`. `repository` entra mais adiante; `config` e `security`, quando o passo pedir.

## 4. Tratamento de erros centralizado

- **Status:** concluído, ainda sem commit
- Pacote `exception`:
  - `ErrorResponse`: `record` com `dataHora`, `status`, `erro` e `mensagem`. É o único formato de erro da API.
  - `ResourceNotFoundException` (404), `InvalidRequestException` (400) e `ConflictException` (409).
  - `GlobalExceptionHandler`: `@RestControllerAdvice` que estende `ResponseEntityExceptionHandler`.
- Como cada situação sai:

  | Situação | Status |
  | --- | --- |
  | `InvalidRequestException`, corpo ilegível, parâmetro ausente, tipo errado (ex.: id não numérico) | 400 |
  | `ResourceNotFoundException`, rota inexistente | 404 |
  | método HTTP não permitido | 405 |
  | `Accept` que a API não produz | 406 |
  | `ConflictException` | 409 |
  | `Content-Type` não aceito | 415 |
  | qualquer outro erro | 500, mensagem genérica; o detalhe e o stack trace só vão para o log |

- A resposta de erro é sempre `application/json`, mesmo quando o cliente pede outro `Accept`.
- Os campos `erro` e `mensagem` saem em português, inclusive nos erros do próprio Spring (ex.: `"erro": "Não encontrado"`, `"mensagem": "O valor 'abc' é inválido para 'id'"`).
- Teste: `GlobalExceptionHandlerTests` (`@WebMvcTest` com um controller falso dentro do próprio teste) cobre 400, 404, 405, 406, 409, 415 e 500. Resultado: 11 testes passando, mais o `contextLoads`.
- Ficam para depois:
  - 400 por ordenação em campo inexistente e por validação de campos (`@Valid`): dependem de repository e do starter de validação.
  - 401 e 403 em JSON: entram junto com a segurança.

## 5. Contrato da API de alunos

- **Status:** definido (escrito antes do código)
- Recurso único: `aluno`, rota base `/alunos` (sem versão na URL).
- CRUD mockado: os alunos ficam em memória, dentro do service, e somem ao reiniciar. O `repository` e o banco entram numa fase posterior.

### Campos

| Campo | Entrada | Saída | Regra |
| --- | --- | --- | --- |
| `id` | não (se vier, é ignorado) | sim | inteiro positivo, gerado pela API |
| `nome` | sim | sim | obrigatório, de 3 a 100 caracteres |
| `email` | sim | sim | obrigatório, formato válido, único (sem diferenciar maiúsculas de minúsculas) |
| `dataNascimento` | sim | sim | obrigatória, no passado, formato `yyyy-MM-dd` |
| `ativo` | não | sim | `true` na criação; muda só por `disable`/`enable` |

DTOs: `AlunoRequest` (entrada) e `AlunoResponse` (saída). A entidade `Aluno` nunca sai na API.

### Endpoints

| Verbo | Rota | Entrada | Resposta | Regras e erros |
| --- | --- | --- | --- | --- |
| POST | `/alunos` | corpo `AlunoRequest` | **201** + `AlunoResponse` + cabeçalho `Location` | 400 se algum campo for inválido ou o corpo ilegível; 409 se o e-mail já existir |
| GET | `/alunos` | `page` (padrão 0), `size` (padrão 10, de 1 a 100), `sort` (padrão `id`, ex.: `nome,desc`), filtros opcionais `nome` (contém) e `ativo` | **200** + lista paginada | 400 se `page`, `size`, `ativo` ou `sort` forem inválidos (campos aceitos: `id`, `nome`, `email`, `dataNascimento`); lista vazia é 200 |
| GET | `/alunos/{id}` | id na rota | **200** + `AlunoResponse` | 400 se o id não for inteiro positivo; 404 se não existir |
| PUT | `/alunos/{id}` | corpo `AlunoRequest` (substitui tudo, menos `ativo`) | **200** + `AlunoResponse` | 400 (id ou campos); 404 se não existir; 409 se o e-mail for de **outro** aluno |
| PATCH | `/alunos/{id}/disable` | id na rota | **200** + `AlunoResponse` com `ativo=false` | 400 id inválido; 404 não existe; idempotente (repetir não dá erro) |
| PATCH | `/alunos/{id}/enable` | id na rota | **200** + `AlunoResponse` com `ativo=true` | igual ao `disable` |
| DELETE | `/alunos/{id}` | id na rota, sem corpo | **204** sem corpo | 400 id inválido; 404 não existe |

### Convenções

- Ordem de validação: corpo (400), id (400), existência (404), conflito (409), alteração.
- Erros sempre no formato `ErrorResponse` (passo 4).
- Fora do contrato por enquanto: `PATCH` parcial genérico em `/alunos/{id}`.

## 6. Implementação dos 7 endpoints

- **Status:** concluído, ainda sem commit
- Dependência nova no `pom.xml`: `spring-boot-starter-validation` (Bean Validation, para o `@Valid`).
- Arquivos criados:
  - `model/Aluno`: entidade simples (sem JPA por enquanto).
  - `dto/AlunoRequest`, `dto/AlunoResponse` e `dto/PageResponse<T>` (lista paginada).
  - `mapper/AlunoMapper`: mapper escrito à mão (`toEntity`, `toResponse`). Sem MapStruct/Dozer, por ser um mapeamento pequeno; dá para trocar depois sem mexer no controller.
  - `service/AlunoService`: regras (id positivo, 404, e-mail único, paginação, filtros, ordenação) e armazenamento em memória. Os métodos são `synchronized`.
  - `controller/AlunoController`: os 7 endpoints; só fala com o service e com o mapper.
- `GlobalExceptionHandler` ganhou a mensagem de validação do corpo, que lista os campos inválidos, ex.: `email: deve ser um e-mail válido; nome: deve ter de 3 a 100 caracteres`.
- Logs SLF4J no service: uma linha `info` por operação de escrita, só com o id (sem nome, e-mail ou data de nascimento).
- Testes: `AlunoControllerTests` (22 testes) cobre os 7 endpoints e os status 200, 201, 204, 400, 404 e 409. Total da suíte: 34 testes passando.

## 7. Coleção do Postman

- **Status:** concluído, ainda sem commit
- Pasta `postman/` na raiz do repositório:
  - `api-alunos.postman_collection.json`: coleção com 3 pastas.
    - `0 - Carga inicial (10 alunos)`: 10 `POST` com alunos de exemplo (pode rodar de novo; o que já existe responde 409).
    - `1 - CRUD de alunos`: os 7 endpoints em sequência (o primeiro item guarda o id em `{{alunoId}}`; o último remove o aluno).
    - `2 - Erros esperados`: 400, 404, 405, 409 e 415, conferindo o formato `ErrorResponse`.
  - `api-alunos-local.postman_environment.json`: ambiente `API Alunos - Local` com `baseUrl` (`http://localhost:8080`), `alunoId` e variáveis de e-mail preenchidas pelos scripts.
- Como usar: Postman → Import → escolher os dois arquivos → selecionar o ambiente → subir a aplicação (`.\mvnw.cmd spring-boot:run`) → rodar pelo Runner.
- Verificado com um script em Node que dispara as 36 requisições e confere o status esperado: 36 ok, 0 falhas.

## 8. CRUD mockado com registros fixos e testes de integração

- **Status:** concluído, ainda sem commit
- `AlunoService` passou a guardar os alunos numa `CopyOnWriteArrayList` (antes era um mapa em memória).
- A aplicação já sobe com 5 alunos fixos (ids 1 a 5): Ana Souza, Bruno Lima, Carla Dias, Diego Martins e Eduarda Costa. Os próximos criados começam no id 6.
  - Na coleção do Postman, os 5 primeiros itens da carga inicial passam a responder 409 (e-mail já existe); os 5 últimos criam normalmente.
- `AlunoControllerTests` agora usa `@SpringBootTest` + `@AutoConfigureMockMvc` (antes `@WebMvcTest`), com a aplicação inteira. Cobre busca, listagem, criação, atualização, remoção, 404 e 400, mais os registros fixos. 24 testes no total; a suíte inteira tem 36, todos passando.

## 9. Respostas em JSON, XML e YAML

- **Status:** concluído, ainda sem commit
- O formato vem do cabeçalho `Accept`: `application/json`, `application/xml`, `application/yaml` ou `application/x-yaml`. Sem `Accept` (ou com `*/*`), a resposta é JSON.
- Dependências novas no `pom.xml`: `tools.jackson.dataformat:jackson-dataformat-xml` e `jackson-dataformat-yaml`. O Spring Boot 4 usa o Jackson 3, que mudou de pacote (`tools.jackson`), com versão gerenciada pelo próprio Boot.
- Diferença para o playbook: ele pede uma classe própria que estende `AbstractJackson2HttpMessageConverter` e um `YAMLMapper`. Isso era do Jackson 2 / Spring 6. No Spring 7 o conversor de YAML (`JacksonYamlHttpMessageConverter`) já vem pronto e é registrado sozinho, só com a dependência; o XML também. Por isso não criei conversor.
- `config/WebConfig`:
  - `configureContentNegotiation` com `defaultContentType(MediaType.APPLICATION_JSON)`.
  - `extendMessageConverters` faz o conversor de YAML aceitar também `application/x-yaml` (por padrão ele só aceita `application/yaml`).
- Datas saem como texto (`2000-05-10`) nos três formatos: o Jackson 3 já não escreve datas como número por padrão, então não foi preciso mexer em `WRITE_DATES_AS_TIMESTAMPS`.
- O corpo das requisições (`POST`/`PUT`) também pode ser enviado em XML ou YAML, pelo `Content-Type`.
- Os erros (`ErrorResponse`) continuam sempre em JSON, mesmo quando o cliente pede XML ou YAML.
- Na lista paginada em XML, a lista aparece como `<content><content>...</content></content>` (um elemento embrulhando os itens). É funcional; dá para limpar depois.
- Testes: `ContentNegotiationTests` (10) cobre os formatos, o padrão sem `Accept`, as datas, o corpo em XML/YAML e o erro em JSON. A suíte inteira tem 46 testes, todos passando.

## 10. Serialização

- **Status:** concluído, ainda sem commit
- Formato combinado, igual em JSON, XML e YAML:
  - **Ordem dos campos** (`@JsonPropertyOrder`): `AlunoResponse` = `id, ativo, email, nome, dataNascimento` (ordem escolhida pelo usuário); `PageResponse` = `content, page, size, totalElements, totalPages`; `ErrorResponse` = `dataHora, status, erro, mensagem`.
  - **Data de nascimento** (`@JsonFormat`): sempre `yyyy-MM-dd`, na saída (`AlunoResponse`) e na entrada (`AlunoRequest`).
  - **Data e hora do erro:** `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` em UTC, em milissegundos (antes saía com 9 casas, ex.: `2026-10-29T23:15:08.325835500Z`; agora `2026-10-29T23:15:08.325Z`).
  - **Lista no XML:** `<content><item>...</item></content>` (antes `<content><content>...`), com as anotações do Jackson XML em `PageResponse`. JSON e YAML não mudam. Lista vazia sai como `<content/>`.
- Serializador próprio (`@JsonSerialize`): não foi preciso, nenhum valor exige apresentação especial hoje. Entra quando surgir um caso real (ex.: mascarar um dado sensível).
- Anotações no Jackson 3: `@JsonFormat` e `@JsonPropertyOrder` continuam no pacote `com.fasterxml.jackson.annotation`; as do XML ficam em `tools.jackson.dataformat.xml.annotation`.
- Testes: `SerializationTests` (6) compara o texto exato do JSON, XML e YAML, a ordem da página, a lista vazia e a data e hora do erro. Suíte total: 52 testes, todos passando.

## 11. Infraestrutura na AWS (tutorial `AWS-Esteira CICD.md`)

- **Status:** em andamento (seções 1 a 7 do tutorial feitas; 8 em diante pendentes)
- Os detalhes de tela e as diferenças em relação ao tutorial estão no `Duvida-CICD.md`. Aqui ficam a ordem, as decisões e o porquê.
- Região: N. Virginia (`us-east-1`). Cadeia final: GitHub Actions → imagem no ECR → ECS Fargate (aplicação) → RDS MySQL (banco), com a senha no Secrets Manager.

### O que foi criado (na ordem)

| **Seção do tutorial** | **O que foi criado** | **Nome usado** |
| --- | --- | --- |
| 1. Banco (RDS) | Servidor MySQL, Single-AZ, `db.t4g.micro`, 20 GiB | `database-crud-alunos` |
| 2. Repositório (ECR) | Repositório privado de imagens | `respositorio-crud-alunos` (falta um "i"; o nome não pode ser alterado) |
| 3. IAM | Usuário da esteira + chave de acesso | `GITHUB_ACTIONS_CRUD_ALUNOS` |
| 4. Cluster (ECS) | Cluster Fargate | `cluster-crud-alunos` |
| 5. Secrets Manager | Secret da senha do banco + política de leitura na `ecsTaskExecutionRole` | `secrets_crud_alunos/db-password`, política `LerSecretDB_CrudAlunos` |
| 6. Security groups | Grupo da aplicação (porta 8080) e regra 3306 no grupo `default` do RDS | `security-group-crud-alunos` |
| 7.1 Task Definition | Contêiner, porta 8080, variáveis e logs | `crud-alunos-task-definition:1` |
| 7.2 `task-definition.json` | Arquivo para a esteira, criado a partir do JSON da revisão `:1` | `apicrud-alunos/task-definition.json` |

### Decisões e por quê

- **Nomes próprios** em vez dos do tutorial (`rest-with-claude...`), para não misturar com outros projetos.
- **Database `crud_alunos`**, criado pela aplicação na primeira conexão (`createDatabaseIfNotExist=true` na `DB_URL`), em vez do `rest_udemy` do tutorial. O database não foi criado no RDS.
- **Variável `DB_URL` a mais que o tutorial.** O tutorial supõe que o endereço do banco já está no código; aqui ele entra como variável de ambiente na Task Definition. A aplicação lê `DB_URL`, `DB_USER` e `DB_PASSWORD` (ainda não implementado).
- **Senha só no Secrets Manager** (`DB_PASSWORD` em `secrets`, tipo `ValueFrom`). Nunca no código, na imagem nem no Git.
- **Porta 3306 do banco** só para o grupo da aplicação e para o IP do usuário (`/32`). Nunca `0.0.0.0/0`.
- **`task-definition.json` fica versionado** (não vai no `.gitignore`): a esteira só enxerga o que está no repositório. Ele tem ID da conta e ARNs, mas nenhuma senha.
- **Ordem diferente do tutorial:** a imagem vai para o ECR (seção 8) **antes** de criar o Service (seção 9), com `Desired tasks = 1`. Sem imagem, as tarefas falhariam em `CannotPullContainerError`.
- A aplicação atual guarda os alunos em memória e ainda não usa o banco, então a imagem sobe normalmente no Fargate. Isso permite testar o caminho completo (imagem → ECR → Service → IP público → Postman) antes da conexão com o MySQL.

### Cuidados aprendidos

- A criação do RDS tem padrões caros (cerca de 145 USD/mês); confira o *Estimated monthly costs* antes de criar.
- O console está sendo usado como **usuário raiz**; o ideal é um usuário IAM administrador com MFA.
- Existe um grupo `rest-with-claude-app-sg` sobrando de uma tentativa anterior, ainda com regra na 3306 do banco.
- A política da `ecsTaskExecutionRole` pode precisar de `logs:CreateLogGroup` (`awslogs-create-group` está `true`).

### Falta fazer

1. ~~`Dockerfile`~~ (feito, passo 12).
2. Seção 8: enviar a imagem para o ECR (Docker e AWS CLI já instalados; falta `aws configure` com perfil próprio).
3. Seção 9: criar o Service.
4. Seção 10: testar a API pelo IP público (Postman).
5. Conexão com o MySQL na aplicação: JPA, driver, `application.yml` lendo `DB_URL`/`DB_USER`/`DB_PASSWORD`, `repository`.
6. Seção 11: workflow do GitHub Actions (`.github/workflows`; resolver antes a pasta `.github/` ignorada) e cadastro dos secrets.
7. Seções 12 a 15: rotina, problemas comuns, limpeza e ligar/desligar (custos).

## 12. Dockerfile

- **Status:** concluído e testado localmente, ainda sem commit
- Arquivos em `apicrud-alunos/`: `Dockerfile` e `.dockerignore` (deixa `target/`, `.git/`, arquivos de IDE e `HELP.md` fora do contexto do build).
- **Duas fases:**
  1. **Build** (`eclipse-temurin:25-jdk`): copia só `mvnw`, `.mvn` e `pom.xml` e baixa as dependências (`dependency:go-offline`), para o Docker reaproveitar essa camada em builds seguintes; depois copia `src` e roda `mvnw clean package -DskipTests`. Os testes não rodam aqui: rodam na esteira antes do build da imagem.
  2. **Execução** (`eclipse-temurin:25-jre`): só o Java e o `app.jar`, sem Maven nem código-fonte. Roda com um usuário comum (`app`), não como root. Expõe a porta 8080.
- `sed -i 's/\r$//' mvnw` no build: remove quebras de linha do Windows (CRLF) do `mvnw`, que impediriam o script de rodar no Linux.
- Java 25 na imagem, igual ao `java.version` do `pom.xml` e ao JDK local (versões precisam bater, como o playbook avisa).
- **Teste local:** `docker build -t crud-alunos-local .` e `docker run -p 8080:8080 crud-alunos-local`. A aplicação subiu em 4 segundos, `GET /alunos/1` devolveu a Ana Souza, um id inexistente devolveu o erro 404 em português, o usuário no contêiner é `app` e a arquitetura é x86_64 (a mesma da Task Definition). Imagem com 521 MB.
- Ainda sem `HEALTHCHECK`: o projeto não tem o Actuator.

## Próximos passos

- Commitar o que está pendente (serialização, `Dockerfile`, `AWS-Esteira CICD.md`, `Duvida-CICD.md`, `task-definition.json`, `.md`).
- Seguir a lista "Falta fazer" do passo 11.
