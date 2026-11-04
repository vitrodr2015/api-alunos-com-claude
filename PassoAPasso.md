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
- **Atualizado nos passos 26 e 28:** os limites de paginação deste contrato (`size` padrão 10, faixa de 1 a 100, 400 fora dela) mudaram (passo 26) e a listagem e o `GET` por id passaram a responder em HAL (passo 28); a regra vigente está nesses passos.

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
- A aplicação sobe com alunos fixos. Começou com 5 (ids 1 a 5): Ana Souza, Bruno Lima, Carla Dias, Diego Martins e Eduarda Costa. Depois entraram o id 6 (Felipe Rocha, junto com a esteira) e o id 7 (Gustavo Alves, `gustavo.alves@escola.com`, commit `d539331`) e o id 8 (Helena Barros, `helena.barros@escola.com`, primeira mudança do ciclo do Git Flow, branch `feature/aluno-8`). Os próximos criados começam no id 9. O teste `AlunoControllerTests` confere os registros fixos, inclusive o 7.
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

- **Status:** em andamento (seções 1 a 11 e 15 do tutorial feitas; faltam 12 a 14 e a conexão da aplicação com o MySQL)
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
| 9. Service (ECS) | Service Fargate com 1 tarefa, grupo `security-group-crud-alunos`, IP público ligado e sem subnets de Local Zone | `service-crud-alunos` |
| 8. Imagem no ECR | Imagem do `Dockerfile` enviada com a tag `latest`, usando o perfil `crud-alunos` da AWS CLI | `respositorio-crud-alunos:latest` (digest `sha256:49b1057e...`) |
| 10. Teste | `GET /alunos` pelo IP público da tarefa, no Postman (sem Swagger e sem login) | porta 8080 |
| 11. Esteira | Workflow do GitHub Actions + secrets `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` | `.github/workflows/continuous-deployment.yml`; deploy gerou `crud-alunos-task-definition:2` |
| 15. Ligar e desligar | Desligado em 2026-10-30 (Desired tasks = 0, RDS parado); ligado de novo em 2026-11-01 (Desired tasks = 1) | `service-crud-alunos`, `database-crud-alunos` |

### Decisões e por quê

- **Nomes próprios** em vez dos do tutorial (`rest-with-claude...`), para não misturar com outros projetos.
- **Database `crud_alunos`**, criado pela aplicação na primeira conexão (`createDatabaseIfNotExist=true` na `DB_URL`), em vez do `rest_udemy` do tutorial. O database não foi criado no RDS.
- **Variável `DB_URL` a mais que o tutorial.** O tutorial supõe que o endereço do banco já está no código; aqui ele entra como variável de ambiente na Task Definition. A aplicação lê `DB_URL`, `DB_USER` e `DB_PASSWORD` (implementado no passo 19).
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
2. ~~Seção 8: enviar a imagem para o ECR~~ (feito).
3. ~~Seção 9: criar o Service~~ (feito; a tarefa só subiu depois de dar à `ecsTaskExecutionRole` a permissão de ler o secret).
4. ~~Seção 10: testar a API pelo IP público (Postman)~~ (feito; sem Swagger nem login, o teste é `GET /alunos`).
5. ~~Conexão com o MySQL na aplicação: JPA, driver, `application.yml` lendo `DB_URL`/`DB_USER`/`DB_PASSWORD`, `repository`~~ (feito nos passos 17 a 22, só na `develop`; falta o `AlunoService` usar o repository).
6. ~~Seção 11: workflow do GitHub Actions e secrets~~ (feito, ver "Esteira" abaixo; a pasta `.github/` está versionada e sem regra de ignore).
7. Seções 12 a 14 (rotina, problemas comuns, limpeza) ainda não executadas. A seção 15 já foi usada (ligar e desligar).

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

## 13. Esteira (GitHub Actions)

- **Status:** concluído; a esteira rodou e fez o deploy (commit `8fdf837`, "Esteira CI/CD e mais um aluno fixo")
- Arquivos: `.github/workflows/continuous-deployment.yml` e `apicrud-alunos/docker-compose.yml` (dá o nome `vitrodr/crud-alunos` à imagem gerada).
- Secrets do GitHub (Settings → Secrets and variables → Actions): `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY`, do usuário `GITHUB_ACTIONS_CRUD_ALUNOS`.
- Gatilho: `push` na `main` (build, testes e deploy) e `pull_request` para a `main` (só build e testes). Push que muda só `.md` não dispara (`paths-ignore`).
- Etapas: JDK 25 → `mvn -B verify` (roda os testes) → `docker compose build` → (só em push) login na AWS e no ECR → envio da imagem para `respositorio-crud-alunos` com a tag do commit e `latest` → renderiza `apicrud-alunos/task-definition.json` com a imagem do commit → registra a nova revisão e atualiza `service-crud-alunos` no `cluster-crud-alunos`, esperando o Service estabilizar.
- Permissões: `permissions: contents: read` (menor privilégio). A esteira só lê o repositório; o deploy usa as chaves da AWS, não o `GITHUB_TOKEN`.
- Publicação no Docker Hub: deixada comentada, opcional.
- Resultado: Task Definition `crud-alunos-task-definition:2`, com a imagem marcada com o hash do commit.
- A esteira não altera o *Desired tasks*; com 0, termina verde, mas nenhuma aplicação sobe.
- Detalhes e diferenças em relação ao tutorial: `Duvida-CICD.md`, seção 11.

## 14. Ligar e desligar o ambiente AWS

- **Status:** feito e usado
- **Desligar (2026-10-30):** Service com *Desired tasks* = 0; RDS `database-crud-alunos` parado (*Stop temporarily*). O banco de sobra `vitrodr-db-server` foi apagado.
- **Ligar (2026-11-01):** RDS → Start; Service → Update → *Desired tasks* = 1 (campo que fica na página do **Service**, não na da Task Definition); IP novo na aba Tasks; teste de `GET /alunos` no Postman, ok.
- O RDS parado religa sozinho após 7 dias (previsto: cerca de 2026-11-06).
- Custo com tudo ligado: tarefa de cerca de US$ 1,30 por dia, mais o RDS. Ao terminar, voltar *Desired tasks* para 0 e parar o RDS.
- Passo a passo e avisos: `Duvida-CICD.md`, seção 15.

## 15. Proteger a branch principal

- **Status:** concluído (regra criada e teste de bloqueio confirmado)
- Em Settings → Rules → Rulesets, ruleset `pre-main`, alvo: branch padrão (`main`), lista de bypass vazia.
- Regras marcadas: exigir pull request antes de mesclar (aprovações em 0, projeto individual), exigir o check **`build`** verde (nome do job em `.github/workflows/continuous-deployment.yml`) com a branch atualizada, bloquear force push e bloquear a exclusão da `main`.
- Limitação: em repositório **privado** de conta gratuita o GitHub não aplica rulesets (aviso na própria tela). Como o repositório é público, a regra vale.
- Teste de bloqueio: branch `teste-ci-quebrado` com um teste quebrado de propósito (nunca mesclar). O PR deve ficar com o `build` vermelho e o Merge bloqueado. **Resultado confirmado em 2026-11-01:** o PR ficou com o `build` vermelho, o Merge ficou bloqueado e o PR foi fechado sem mesclar.
- Daqui em diante, mudanças vão por branch e pull request; o merge gera o push na `main` que dispara o deploy.
- **Ponto de atenção (repositório público):** o repositório é público. Foram encontrados no histórico o ID da conta AWS, o endpoint do RDS, ARNs, IDs de security group e o IP de casa; não há senha nem chave de acesso. Os `.md` atuais foram limpos (IP de casa e IDs de security group viraram marcadores), mas o histórico do Git mantém os valores antigos. Decisão: não reescrever o histórico (projeto de estudo). Cuidados daqui para frente: nunca versionar senha, chave ou token; usar marcadores para IPs e IDs nos `.md`; manter a senha do RDS forte; desligar o *Public access* do RDS quando não precisar do acesso pelo computador local.

## 16. Fluxo Git Flow

- **Status:** concluído; ciclo completo praticado em 2026-11-01 (release `1.0.0`)
- Motivo: o usuário quis juntar vários PRs numa versão e só então acionar a esteira, e escolheu estudar o Git Flow.
- Branches:

  | Branch | Papel | Sai de | Volta para |
  | --- | --- | --- | --- |
  | `main` | produção; cada merge vira versão com tag `vX.Y.Z` e dispara o deploy | | |
  | `develop` | integração dos PRs de feature | `main` | |
  | `feature/*` | uma mudança | `develop` | `develop` |
  | `release/X.Y.Z` | fecha a versão (ajustes finais) | `develop` | `main` e `develop` |
  | `hotfix/*` | correção urgente | `main` | `main` e `develop` |

- Rulesets (Settings → Rules): `pre-main` foi renomeado para `protect-main`; criados `protect-develop` e `protect-release` (`release/**`). Regras iguais nos três: PR obrigatório (0 aprovações, projeto individual), check `build` obrigatório e com a branch atualizada, sem force push. Em `main` e `develop` também não é possível apagar a branch; em `release/**` é, para poder limpar depois do merge. Sem lista de bypass.
- `develop` criada a partir da `main` (commit `22480ce`).
- Workflow: PRs para `main`, `develop` e `release/**` rodam build e testes; o deploy continua só em `push` na `main` (primeira feature: `feature/ci-gitflow`). O `push` em `develop` não foi incluído de propósito: os passos de deploy usam `if: github.event_name == 'push'` e publicariam a cada merge na `develop`.
- `gh` (GitHub CLI) instalado com `winget` e logado; passou a abrir PRs e a acompanhar o `build` pelo terminal.
- Ciclo completo, como foi feito:
  1. `feature/ci-gitflow` → PR #3 → `develop`.
  2. `feature/aluno-8` → PR #4 → `develop`.
  3. `release/1.0.0` criada a partir da `develop`.
  4. PR #5 `release/1.0.0` → `main`, com merge commit (`7e10a2f`). O merge disparou a esteira, que fez o deploy (`success`); a tarefa foi trocada e o IP mudou.
  5. Tag anotada `v1.0.0` no commit `7e10a2f`.
  6. PR #6 `main` → `develop`, para a `develop` receber o merge commit da release.
- A `release/1.0.0` foi usada como acumulador (ideia do usuário): o PR #5 ficou aberto enquanto mais PRs podiam entrar na release, e o deploy só aconteceu no merge. No Git Flow clássico a release só recebe correções e as features novas esperam a próxima; fica como segundo exercício.
- As branches já mescladas foram apagadas do GitHub em 2026-11-02 (15 branches, pela API `gh api -X DELETE .../git/refs/heads/<nome>`, a pedido do usuário). Ficaram `main`, `develop` e `teste-ci-quebrado` (nunca mesclada, de propósito). As tags `v1.0.0` e `v1.1.0` continuam.

## 17. Banco de dados local (MySQL em Docker)

- **Status:** concluído (2026-11-01)
- Contêiner `mysql-dev`, imagem `mysql:8.4` (versão 8.4.11), `utf8mb4` e `utf8mb4_unicode_ci`, com o volume `mysql-dev-data` (os dados sobrevivem a reiniciar ou recriar o contêiner).
- Database criado na subida (`MYSQL_DATABASE`): `crud_alunos`, o mesmo nome usado na AWS. Vazio, sem tabelas.
- **Porta 3307 no computador** (`-p 3307:3306`), não 3306: o Windows tem o serviço **MySQL84** (MySQL Server 8.4 instalado, início automático) ocupando a 3306. A URL local fica `jdbc:mysql://localhost:3307/crud_alunos`. O `netstat -ano` não mostrou a 3306 ocupada nem as portas do `mysqld`; o erro do Docker (`Only one usage of each socket address`) foi o que revelou o conflito.
- Senha: só de desenvolvimento, **não registrada** em arquivo do repositório (que é público). Para conectar: HeidiSQL ou DBeaver em `localhost`, porta `3307`, usuário `root`.
- Comando usado (a senha foi passada por variável do shell, para não ficar escrita no comando):

  ```bash
  docker run -d --name mysql-dev \
    -e MYSQL_ROOT_PASSWORD="$SENHA_DEV" -e MYSQL_DATABASE=crud_alunos \
    -p 3307:3306 -v mysql-dev-data:/var/lib/mysql mysql:8.4 \
    --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
  ```

- Verificação feita por `docker exec`: versão 8.4.11, database `crud_alunos` com `utf8mb4`/`utf8mb4_unicode_ci`, sem tabelas. Conferido também pelo usuário no HeidiSQL (`localhost`, porta 3307, `root`): conectou e viu o schema `crud_alunos` vazio, o "Pronto quando" do passo.
- Outros contêineres antigos na máquina (`rest-mysql`, `rest-app`, `keen_williamson`) são de projetos anteriores e estão parados; não foram tocados.
- Comandos úteis: `docker stop mysql-dev` e `docker start mysql-dev`. Apagar o contêiner (`docker rm`) mantém os dados; só `docker volume rm mysql-dev-data` os apaga. O Compose da Fase 9 vai substituir este comando.

## 18. Dependências de JPA e do driver

- **Status:** concluído nesta etapa (branch `feature/jpa-dependencias`); a aplicação **real** só sobe depois do passo seguinte do playbook (datasource)
- `pom.xml`: `spring-boot-starter-data-jpa`, `com.mysql:mysql-connector-j` (escopo `runtime`; o `mysql-connector-java` é o antigo, descontinuado) e `com.h2database:h2` (escopo `test`).
- `src/test/resources/application.yml` (novo): `spring.application.name`, `jpa.hibernate.ddl-auto: create-drop` e `jpa.open-in-view: false`. Sem `datasource` nesse arquivo, o Spring Boot usa o H2 em memória nos testes. O arquivo de teste substitui o de `src/main/resources` durante os testes (mesmo nome, vence o `test-classes`).
- Resultado: 52 testes passando com o H2 (`ApicrudAlunosApplicationTests` e os `@SpringBootTest` sobem o contexto inteiro com o JPA). Na primeira rodada, logo depois de mudar o `pom.xml`, o Maven terminou com `BUILD FAILURE` por falha de teste; o motivo não foi guardado. Três rodadas seguintes passaram sem falhas. Fica anotado como instabilidade não explicada; se voltar, guardar o relatório em `target/surefire-reports`.
- **Atenção ao fluxo:** sem `datasource` no `application.yml` principal, a aplicação real falha ao subir (`Failed to determine a suitable driver class`). Como o merge na `main` faz deploy, **este passo não pode ir para a `main` sem o datasource**: os dois passos têm de entrar juntos na mesma release.
- Ainda não há `@Entity`: o `Aluno` continua em memória até o repository entrar.

## 19. Datasource sem expor segredos

- **Status:** concluído (branch `feature/datasource`)
- `src/main/resources/application.yml`: `datasource` com `driver-class-name: com.mysql.cj.jdbc.Driver`, `url: ${DB_URL:...}`, `username: ${DB_USER:root}` e `password: ${DB_PASSWORD}` **sem valor padrão**.
- Padrão de variáveis escolhido: as próprias (`DB_URL`, `DB_USER`, `DB_PASSWORD`), as mesmas que a Task Definition da AWS já usa. Não usamos as nativas do Spring (`SPRING_DATASOURCE_*`).
- Valor padrão da URL (só para desenvolvimento): `jdbc:mysql://localhost:3307/crud_alunos?createDatabaseIfNotExist=true`, o MySQL do Docker do passo 17 (porta 3307, não 3306). O usuário padrão é `root`. Na AWS, `DB_URL` vem da Task Definition.
- **Descoberta importante:** o Spring Boot **não falha** quando `${DB_PASSWORD}` não existe. Ele envia o texto literal `${DB_PASSWORD}` como senha, o MySQL responde `Access denied for user 'root' (using password: YES)` e o erro final aparece como `Unable to determine Dialect without JDBC metadata`, sem citar a senha. Isso não cumpre o "Pronto quando" do passo (erro claro).
- Correção: em `ApicrudAlunosApplication.main`, um listener de `ApplicationEnvironmentPreparedEvent` lança `IllegalStateException` com a mensagem "A variável de ambiente DB_PASSWORD não foi definida..." se houver `spring.datasource.url` e a senha estiver vazia. Primeira tentativa foi um `@Component` com `@Value`, mas ele rodava depois de o Hibernate tentar conectar, então não servia. No `main`, os testes (que usam o H2 e não passam por ele) não são afetados.
- Verificado (2026-11-01):
  - sem `DB_PASSWORD`: a aplicação para na hora com a mensagem clara, sem tentar conectar;
  - com `DB_PASSWORD` e o `mysql-dev` ligado: sobe em cerca de 4 s, conecta no MySQL e `GET /alunos/8` devolve Helena Barros;
  - `mvnw test`: 52 testes passando (H2);
  - `grep` por senha, chave e token no repositório: só nomes e ARNs de secret, nenhum valor. A senha de desenvolvimento não aparece em nenhum arquivo versionado.
- Rodar pela IDE ou pelo terminal agora exige a variável: no IntelliJ, Run → Edit Configurations → Environment variables (`DB_PASSWORD=...`); no terminal, `export DB_PASSWORD=...` antes de `./mvnw spring-boot:run`.
- **Cuidado com o deploy:** a aplicação passa a **exigir o banco no arranque**. Na AWS, o RDS precisa estar `Available` antes do Service subir, senão a tarefa cai ao iniciar. A Task Definition já tem `DB_URL`, `DB_USER` e `DB_PASSWORD` (esta última via Secrets Manager), com os mesmos nomes. O `Aluno` continua em memória: o banco só é conectado, ainda sem tabelas nem `repository`.

## 20. Perfis e arquivos locais

- **Status:** concluído (branch `feature/perfis-locais`)
- O `.gitignore` da raiz já ignorava `application-local.yml`, `application-secret*.yml`, `.env` e `.env.*`; conferido com `git check-ignore`. Nada novo foi preciso nele.
- Configuração só desta máquina vai em `apicrud-alunos/src/main/resources/application-local.yml` (ignorado). Só vale com o perfil ativo: `SPRING_PROFILES_ACTIVE=local` (na IDE, em Environment variables; no terminal, `export SPRING_PROFILES_ACTIVE=local`).
- Teste feito com um `application-local.yml` temporário (`server.port: 8081`): sem o perfil a aplicação subiu na 8080 e ignorou o arquivo; com `SPRING_PROFILES_ACTIVE=local` subiu na 8081. O arquivo temporário foi removido e nunca apareceu no `git status`.
- Criado o modelo `apicrud-alunos/application-local.yml.example` (versionado, só com exemplos comentados e sem segredos). Fica fora de `src/main/resources` para não ser empacotado no JAR. O nome `.example` não casa com a regra de ignore.
- Os testes têm o próprio `src/test/resources/application.yml`, que **substitui** o de `main`, não soma. Prova no projeto: o `application.yml` principal tem datasource MySQL e a senha sem padrão, e os testes sobem normalmente com H2, o que não aconteceria se os dois fossem somados. Quando entrar a segurança (JWT), o arquivo de teste precisa declarar tudo o que os testes exigirem.
- A senha do banco não vai em nenhum desses arquivos: continua na variável `DB_PASSWORD`.

## 21. Migrations com Flyway

- **Status:** concluído (branch `feature/flyway`)
- **Diferença do playbook:** ele manda `flyway-core` + `flyway-mysql`. No Spring Boot 4 a autoconfiguração do Flyway vem no starter próprio, então o `pom.xml` leva `spring-boot-starter-flyway` (traz o `flyway-core`) e `org.flywaydb:flyway-mysql`; as versões são gerenciadas pelo Boot.
- `src/main/resources/application.yml`: `spring.flyway.enabled: true`, `locations: classpath:db/migration`, `spring.jpa.hibernate.ddl-auto: validate` (o Hibernate só confere o banco, nunca o altera) e `open-in-view: false` (o log avisava que vinha ligado por padrão).
- `src/test/resources/application.yml`: `spring.flyway.enabled: false`. Os testes usam H2 e a migration é específica do MySQL.
- Migration `src/main/resources/db/migration/V1__create_table_aluno.sql`: tabela `aluno` com `id` (BIGINT, auto incremento), `nome` (VARCHAR 100), `email` (VARCHAR 255, único), `data_nascimento` (DATE) e `ativo` (BOOLEAN, padrão verdadeiro), em InnoDB `utf8mb4`/`utf8mb4_unicode_ci`. A collation não diferencia maiúsculas de minúsculas, o que dá a unicidade de e-mail sem caixa que o contrato (passo 5) pede.
- Verificado no `mysql-dev` (banco vazio, 2026-11-01):
  - 1ª subida: Flyway criou `flyway_schema_history` e aplicou a versão 1 (`create table aluno`); a aplicação subiu em cerca de 4 s;
  - 2ª subida: `Successfully validated 1 migration` e `Schema is up to date. No migration necessary`;
  - `GET /alunos/8` responde normalmente (o `Aluno` ainda está em memória);
  - `mvnw test`: 52 testes passando, com o Flyway desligado.
- O aviso "MySQL 8.4 is newer than this version of Flyway" **não apareceu**. Durante o build, o Maven mostra avisos `HTTP Status: 401` de repositórios que o `flyway-mysql` declara; são só ruído, o build não falha.
- Falhas de teste durante a verificação: uma subida falhou com `Port 8080 was already in use`, porque a instância anterior ainda estava no ar. Foi colisão do teste, não da aplicação.
- Ainda **sem seed**: os 8 alunos fixos continuam na lista em memória do `AlunoService`. Quando o service passar a usar o banco, eles entram em uma migration nova (`V2__...`), nunca editando a `V1`.
- Regras: nunca editar uma migration já aplicada (criar uma nova); banco que já existia sem Flyway precisa de `baseline-on-migrate`; evitar `ddl-auto: update`.
- **Deploy:** ao subir na AWS, o Flyway vai criar a tabela `aluno` no database `crud_alunos` do RDS na primeira vez. Isso exige o RDS `Available` e o usuário `root` com permissão de criar tabela (tem).

## 22. Entidade e repository

- **Status:** concluído (branch `feature/entidade-repository`)
- `model/Aluno` virou entidade JPA: `@Entity`, `@Table(name = "aluno")`, `@Id` + `@GeneratedValue(strategy = IDENTITY)` e `@Column` em cada campo, com nomes e tamanhos iguais aos da migration `V1__create_table_aluno.sql` (`nome` 100, `email` 255, `data_nascimento`, `ativo`). Em Spring Boot 4 / Jakarta, as anotações vêm de `jakarta.persistence`.
- `repository/AlunoRepository extends JpaRepository<Aluno, Long>`. Sem consultas próprias por enquanto; elas entram quando o `AlunoService` passar a usar o banco.
- O `AlunoService` **continua usando a lista em memória**: este passo só cria a entidade e o repository. A troca fica para o passo seguinte, junto com a migration `V2__...` dos 8 alunos fixos.
- Verificado (2026-11-01):
  - `mvnw test`: 52 testes passando (H2, com `ddl-auto: create-drop`);
  - aplicação no MySQL local com `ddl-auto: validate`: sobe em cerca de 4 s, sem reclamar de coluna ou tipo (o "Pronto quando");
  - prova de que o `validate` confere de verdade: troquei temporariamente o nome da coluna `data_nascimento` na entidade e a aplicação **não subiu**, com `Schema validation: missing column [data_nasc_errada] in table [aluno]`. A alteração foi desfeita. Até este passo o `validate` não tinha nenhuma entidade para conferir, então "subir sem erro" ainda não provava nada.
- Os testes com H2 não validam a migration do MySQL (o H2 cria o schema a partir da entidade, e o Flyway está desligado neles); a conferência contra o MySQL real é a subida acima. (Correção: eu tinha anotado aqui que um teste de repositório entraria no Passo 24 do playbook; o Passo 24 trata dos dados de teste e do Flyway desligado, não de teste de repositório.)

## 23. Trocar o mock pelo repository

- **Status:** concluído (branch `feature/service-com-repository`)
- `AlunoService` agora recebe o `AlunoRepository` **por construtor** e não tem mais a lista em memória, nem o `synchronized` (a concorrência passa a ser do banco). Mesmo contrato: as mesmas validações e as mesmas exceções (`InvalidRequestException`, `ResourceNotFoundException`, `ConflictException`) e as mesmas mensagens em português.
- `@Transactional` nos métodos de escrita (`criar`, `atualizar`, `desativar`, `ativar`, `remover`) e `@Transactional(readOnly = true)` nos de leitura (`listar`, `buscar`). `open-in-view: false` já estava definido no passo 21.
- `AlunoRepository` ganhou as consultas que substituem os filtros em memória:
  - `buscar(nome, ativo, Pageable)`, em JPQL: `locate(lower(:nome), lower(a.nome)) > 0 and (:ativo is null or a.ativo = :ativo)`. Usei `locate` em vez de `like` para `%` e `_` digitados pelo usuário não virarem curinga; `ativo` nulo traz todos;
  - `existsByEmailIgnoreCase` e `existsByEmailIgnoreCaseAndIdNot` (na atualização o próprio aluno não conta como conflito).
- Ordenação: `sort` continua no formato `campo` ou `campo,asc|desc`, com os campos `id`, `nome`, `email` e `dataNascimento`; `nome` e `email` ordenam sem diferenciar maiúsculas de minúsculas, e o `id` desempata (menos quando já se ordena por `id`, para não gerar `order by id, id`). Paginação: `page` ≥ 0 e `size` de 1 a 100, como antes.
- **Carga inicial:** os 8 alunos que ficavam fixos na lista foram para a migration `V2__insert_alunos_iniciais.sql` (a `V1` não foi editada). Sem id explícito: numa tabela vazia o `AUTO_INCREMENT` gera 1 a 8 e os próximos começam no 9.
- **Diferença do playbook:** o playbook deixa o `data.sql` para o Passo 24, mas sem carga os testes do CRUD (`AlunoControllerTests` e outros, que esperam os alunos 1 a 8) quebram e o CI ficaria vermelho. Por isso criei já `src/test/resources/data.sql` (os mesmos 8 alunos, também sem id explícito: no H2 um id explícito deixaria a sequência em 1 e a primeira criação colidiria) e `spring.jpa.defer-datasource-initialization: true` no `application.yml` de teste, para o script rodar depois de o Hibernate criar a tabela. O Passo 24 pode ampliar esse arquivo.
- **Testes unitários do service:** `AlunoServiceTests`, com Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock AlunoRepository`, `@InjectMocks AlunoService`), 16 testes sem Spring e sem banco: criação (ativo, id ignorado, conflito), busca (achou, 404, id inválido sem consultar o banco), atualização (campos, `ativo` preservado, conflito, 404 antes de checar o e-mail), ativar/desativar (idempotentes, 404), remover (apaga, 404 sem apagar) e listagem (página, ordenação com `ignoreCase` e desempate por id, filtros nulos, parâmetros inválidos).
- **Resultado:** `mvnw test` com 68 testes passando (52 antigos + 16 novos). Os 52 antigos, que testavam o CRUD mockado, voltam a passar contra o banco (H2 + `data.sql`).
- **Verificado no MySQL local** (`mysql-dev`, 2026-11-01): ao subir, o Flyway aplicou a `V2` e a aplicação ficou de pé em cerca de 5 s. As rotas devolvem os dados do banco: lista paginada e ordenada, filtros `nome=SOUZA` e `ativo`, `GET /alunos/8`; `POST` → 201 com `Location`; e-mail repetido, inclusive trocando a caixa → 409; `PUT` (e 409 com e-mail de outro aluno); `PATCH disable/enable`; `DELETE` → 204 e depois 404; ids inválidos e `sort` inválido → 400. Conferi o banco direto por `docker exec`. O aluno de teste (id 9) foi apagado pela própria rota; o próximo `id` local será 10.
- **Limite conhecido:** se dois pedidos com o mesmo e-mail chegarem ao mesmo tempo, os dois passam pela checagem e a restrição `UNIQUE` do banco recusa o segundo com `DataIntegrityViolationException`, que hoje cai no tratamento genérico (500). Dá para mapear para 409 no `GlobalExceptionHandler`; fica anotado.
- **Efeito no deploy:** no primeiro deploy com o banco, o Flyway cria a tabela e aplica a `V2` no RDS (o RDS precisa estar `Available`). A coleção do Postman, com 10 alunos de exemplo, passa a responder 409 para os e-mails que já existem na carga.

## 24. Testes rápidos com H2: dados de teste e Flyway desligado

- **Status:** concluído (branch `feature/h2-dados-de-teste`)
- Quase tudo o que o passo pede já tinha entrado nos passos 18, 21 e 23. O `src/test/resources/application.yml` completo ficou assim: `flyway.enabled: false` (passo 21), `jpa.hibernate.ddl-auto: create-drop`, `jpa.open-in-view: false`, `jpa.defer-datasource-initialization: true` (passo 23) e, **novo neste passo**, `spring.sql.init.mode: always`. Para banco embutido o `always` já é o padrão; ficou explícito para não depender disso.
- `src/test/resources/data.sql` (passo 23): os 8 alunos, **sem informar o `id`** nos inserts. Com id explícito o H2 não avança a sequência e o próximo `POST` colide; sem id, o auto incremento segue de 9 em diante.
- **`@DirtiesContext` não foi necessário.** Conferi os testes que alteram dados: eles só mexem em alunos que criam na hora (e-mail e nome únicos) ou no id inexistente `987654`; nenhum altera os alunos 1 a 8, e as listagens filtram por uma marca única. Como prova, rodei a suíte 3 vezes com `-Dsurefire.runOrder=random` (embaralha a ordem das **classes**; a ordem dos métodos dentro de cada classe é do JUnit): 68 testes passando nas 3. Se um teste futuro precisar alterar os alunos 1 a 8, aí sim usar `@DirtiesContext` (recria o contexto e restaura a carga).
- **"`mvn test` passa sem nenhum banco ligado":** os testes usam o H2 em memória (sem `datasource` no arquivo de teste) e o Flyway desligado; o `DB_PASSWORD` e o MySQL não entram. A esteira do GitHub, que não tem MySQL nenhum, roda os 68 testes e passa em cada PR (`build` verde).
- **Limite (lembrete do playbook):** como o Flyway fica desligado nos testes, as migrations **não são exercitadas** por eles. Cada migration nova precisa ser validada subindo a aplicação contra o MySQL local (passo 17), como fizemos com a `V1` e a `V2`. A conferência automática com MySQL real é o Passo 39 do playbook.
- Correção de uma anotação minha: no passo 22 eu disse que um teste de repositório entraria no Passo 24; não é o caso. O Passo 24 é este (dados de teste e Flyway desligado).

## 25. Release 1.1.0: a aplicação usa o MySQL na AWS

- **Status:** concluído (2026-11-02); deploy `success`, aplicação respondendo na AWS lendo do RDS
- Ciclo, como foi feito:
  1. `release/1.1.0` criada a partir da `develop` (12 commits, 17 arquivos: passos 17 a 24).
  2. PR #16 `release/1.1.0` → `main`, com o `build` verde (68 testes). Antes do merge o usuário confirmou o RDS `Available` e o Service com *Desired tasks* = 1.
  3. Merge com *merge commit* (`dd7d051`) → a esteira rodou e o deploy terminou com `success`.
  4. Tag anotada `v1.1.0` em `dd7d051`.
  5. PR #17 `main` → `develop` (merge commit `e9cd5ed`), para a `develop` receber a release.
- **Conferido na AWS** (pela AWS CLI, perfil `crud-alunos`): Service `ACTIVE`, 1 tarefa `RUNNING`, deploy `COMPLETED`, Task Definition **`crud-alunos-task-definition:6`**. (Eu havia previsto a `:3`; a numeração é maior porque cada deploy da `main` registra uma revisão nova.)
- **Teste da API na AWS** (IP público da tarefa, porta 8080): `GET /alunos` devolveu 8 alunos (`totalElements` 8); `GET /alunos/8` devolveu Helena Barros; id inexistente deu 404. Ou seja, o Flyway rodou no RDS (tabela `aluno` e `V2`) e a aplicação conectou com a senha do Secrets Manager. Era o primeiro teste real da aplicação com o banco na nuvem.
- A numeração do `AUTO_INCREMENT` no RDS segue em 9, como no banco local.
- Lembretes: o IP muda a cada tarefa; os dados agora ficam no RDS e sobrevivem à troca de tarefa; o RDS parado derruba a aplicação ao subir. Ao terminar, *Desired tasks* = 0 e parar o RDS.
- **Ambiente desligado em 2026-11-02:** Service em Desired tasks = 0 (conferido pela CLI) e RDS `Stopped temporarily` (confirmado pelo usuário no console). O RDS religa sozinho em 7 dias.

## 26. Paginação com `Pageable` (passo 25 do playbook)

- **Status:** concluído (branch `feature/paginacao`)
- **O que mudou:** a listagem deixou de ler `page`, `size` e `sort` como parâmetros próprios com validação à mão e passou a receber um `Pageable`, que o Spring resolve. O controller só recebe `Pageable pageable`; o `AlunoService.listar(nome, ativo, pageable)` escolhe a consulta e acrescenta o `id` como desempate (e como ordem padrão), para a paginação ser estável entre páginas.
- **Limites no `application.yml`:** `spring.data.web.pageable.default-page-size: 20` e `max-page-size: 50`. O `application.yml` **de teste substitui o principal**, então os mesmos dois valores foram repetidos nele (o playbook já avisa desse cuidado).
- **Repository:** as consultas passaram a ser derivadas, `findByNomeContainingIgnoreCase` e `findByNomeContainingIgnoreCaseAndAtivo` (o `@Query` com `locate` do passo 23 saiu). O Spring Data já escapa `%` e `_`, então `nome=%` procura o texto `%` e não vira curinga.
- **Ordenação inválida = 400:** `GlobalExceptionHandler` trata `PropertyReferenceException` e devolve o `ErrorResponse` com 400, ex.: `Não é possível ordenar por 'senha': esse campo não existe`. **Diferença de versão:** no Spring Data 4 (Boot 4) a classe é `org.springframework.data.core.PropertyReferenceException`; o pacote antigo `org.springframework.data.mapping` não existe mais e quebra o contexto inteiro (`NoClassDefFoundError`). No meu primeiro `mvn test` eu filtrei a saída e deixei passar o erro de compilação; só a leitura do relatório do surefire mostrou a causa.
- **O contrato do passo 5 mudou (decisão do playbook):**

  | Antes (contrato do passo 5) | Agora |
  | --- | --- |
  | `size` padrão 10; faixa de 1 a 100 | `size` padrão **20**; máximo **50** |
  | `size` acima de 100 → 400 | `size` acima de 50 → **reduzido para 50** (200) |
  | `page=-1`, `size=0`, `page=abc` → 400 | caem nos padrões do Spring: `page` 0 e `size` 20 (200) |
  | `sort` só por `id`, `nome`, `email`, `dataNascimento` | `sort` por **qualquer campo** do aluno (inclui `ativo`); campo inexistente → 400 |
  | `nome` e `email` ordenavam sem diferenciar maiúsculas de minúsculas à força | segue a collation do banco (no MySQL `utf8mb4_unicode_ci` já ignora a caixa; no H2 dos testes não) |

  O `size` devolvido na resposta é o **efetivo** (já reduzido).
- **Testes:** 74 passando. No service (Mockito) saíram os testes de validação de página e entraram os de escolha da consulta e do desempate pelo `id`. No controller, o teste "parâmetros inválidos → 400" foi dividido: ordenação por campo inexistente → 400, filtro `ativo=talvez` → 400, e novos para padrão 20, `size=500` → 50, `page`/`size` fora da faixa → padrões e ordenação por `dataNascimento,desc` e por `ativo`.
- **Verificado no MySQL local** (2026-11-02): sem parâmetros → `page` 0, `size` 20; `size=500` → 50; `size=3&page=2&sort=nome,desc` → página certa; `sort=senha` → 400 com a mensagem acima; `page=-1`, `size=0` e `page=abc` → 200 nos padrões; `nome=SOUZA` → 1; `nome=%` → 0.
- **Postman:** o item "400 - Listar com size 101" esperava 400 e passaria a falhar; virou "200 - Listar com size 101 (não é erro: é reduzido para 50)" e confere `size` = 50. Ele continua na pasta "2 - Erros esperados", só com o nome avisando. A descrição do item de listagem foi atualizada. A coleção continua JSON válido.
- **Ainda não está na produção:** a mudança de contrato só chega à AWS na próxima release (a API da 1.1.0 continua com o contrato antigo).

## 27. Massa de teste: 400 alunos para a paginação

- **Status:** concluído (na mesma branch do passo 26, `feature/paginacao`)
- **Para quê:** com 8 alunos não dá para ver a paginação trabalhando (tudo cabe em uma página). Foram gerados **400 alunos** determinísticos: 20 primeiros nomes × 20 sobrenomes (400 nomes completos distintos), e-mail único com o número da linha (ex.: `adriana.almeida001@escola.com`), datas de nascimento entre 1990 e 2007 e **1 em cada 10 inativo (40)**, para testar também o filtro `ativo`. O gerador é um script Node curto, sem aleatoriedade (rodar de novo produz exatamente o mesmo SQL), com lotes de 100 linhas por `INSERT`; ele conferiu 400 nomes distintos, 400 e-mails distintos e 40 inativos.
- **Onde ficou:**
  - `src/main/resources/db/migration/V3__insert_400_alunos_para_paginacao.sql` (MySQL; a `V1` e a `V2` não foram tocadas);
  - o mesmo conteúdo no fim de `src/test/resources/data.sql` (H2 dos testes). Sem `id` explícito nos dois, como no passo 24.
- **Testes novos** (`AlunoControllerTests`, seis): total e número de páginas com `size=20`; páginas seguidas sem repetir nem pular alunos (ids 1–50, 51–100 e o início da página 9); `size=500` devolve no máximo 50 itens; página além do fim devolve lista vazia com o total correto; filtro `ativo=false` pagina só os inativos; filtro por nome com ordenação e página pequena. Como outros testes criam e desativam alunos no mesmo banco, os totais são comparados com "pelo menos". Suíte: **80 testes** passando, inclusive em ordem aleatória de classes.
- **Verificado no MySQL local** (2026-11-02): ao subir, o Flyway aplicou a `V3`; no banco ficaram 408 alunos (40 inativos). Pela API: padrão `size` 20 → 21 páginas; `page=20` tem 8 itens (408 = 20×20 + 8); `page=21` devolve lista vazia com `totalElements` 408; `size=500` → 50 itens; `ativo=false` → 40; `nome=adriana&sort=nome,desc` → 20 resultados em ordem decrescente; `sort=dataNascimento,desc` → datas decrescentes; `sort=senha` → 400.
- **Detalhe do banco local:** o id mais alto é 409 (não 408), porque o id 9 foi o aluno de teste apagado no passo 23. Por isso a página 0 de `size=50` termina no id 51 e a página 1 começa no 52. Está correto: nenhum aluno repetido ou pulado.
- **Bateria completa depois da paginação** (2026-11-02, MySQL local, por curl): 28 verificações, todas conforme o contrato, e o banco terminou com os mesmos 408 alunos. Cobriu: `GET` lista, por id, inexistente (404), id `0` e `abc` (400); `POST` 201 com `Location`, e-mail repetido com outra caixa (409), campos inválidos, data futura e corpo ilegível (400), `Content-Type` não aceito (415); `PUT` 200, e-mail de outro aluno (409) e inexistente (404); `PATCH disable`/`enable` (idempotentes, 404 se não existe); `DELETE` 204 e depois 404, id inválido (400); método não permitido (405) e `Accept` inexistente (406); formatos JSON, XML e YAML; `sort` inexistente e `ativo=talvez` (400) e `size=500` (200, reduzido). O aluno criado na bateria (id 410) foi apagado pela própria rota, então o próximo id local será 411.
- **Não executado:** a coleção do Postman em si (só foi validada como JSON e teve o item do `size=101` ajustado); a bateria acima foi por curl.
- **Atenção ao levar para a AWS:** a `V3` é uma migration como qualquer outra. Na **próxima release**, o Flyway vai aplicá-la no RDS e inserir os 400 alunos lá também. Em projeto real, dados de exemplo não vão para produção; aqui é aceitável por ser estudo, mas é uma decisão a tomar **antes** de fazer a release: depois de aplicada, a `V3` não pode ser editada (só desfeita por uma `V4` que apague as linhas).

## 28. HATEOAS (passo 26 do playbook, opcional)

- **Status:** concluído (branch `feature/hateoas`). Escolha do usuário: opção 1, HAL só na **listagem** e no **`GET /alunos/{id}`**; os demais endpoints não mudam.
- **Dependência:** `spring-boot-starter-hateoas` (Spring HATEOAS 3.1.3, gerenciado pelo Boot 4.1.2). Funciona com o Jackson 3 do Boot 4; confirmei rodando, não só pela documentação.
- **Como ficou:**
  - `AlunoResponse` ganhou `@Relation(collectionRelation = "alunos", itemRelation = "aluno")`, para a lista sair em `_embedded.alunos` (sem isso o nome seria `alunoResponseList`).
  - `assembler/AlunoModelAssembler implements RepresentationModelAssembler<AlunoResponse, EntityModel<AlunoResponse>>`: cada aluno recebe `self`, `alunos` (a coleção) e **só a ação que faz sentido**: `disable` se está ativo, `enable` se está inativo.
  - `AlunoController.listar` devolve `PagedModel<EntityModel<AlunoResponse>>` via `PagedResourcesAssembler` (gera `first`, `prev`, `self`, `next`, `last` e o bloco `page`); `buscar` devolve `EntityModel<AlunoResponse>`. O service passou a devolver `Page<Aluno>` e o `PageResponse` (o record do passo 10) foi **apagado**, porque o `Page` do Spring Data + o `PagedModel` o substituem.
  - Os dois `GET` declaram `produces = {application/hal+json, application/json}`.
- **Formato novo da listagem:**
  ```json
  { "_embedded": { "alunos": [ { "_links": { "self": {...}, "alunos": {...}, "disable": {...} }, "id": 1, "ativo": true, "email": "...", "nome": "...", "dataNascimento": "..." } ] },
    "_links": { "first": {...}, "prev": {...}, "self": {...}, "next": {...}, "last": {...} },
    "page": { "number": 0, "size": 20, "totalElements": 408, "totalPages": 21 } }
  ```
  Os links da página **preservam os filtros** (`nome`, `ativo`) e o tamanho. Lista vazia vem **sem `_embedded`** (só `_links.self` e `page`). Primeira página não tem `prev`; última não tem `next`. Os campos de cada aluno continuam na ordem do passo 10, com `_links` na frente.
- **O contrato mudou de novo:**

  | Antes (passos 5 a 10) | Agora |
  | --- | --- |
  | `content`, `page`, `size`, `totalElements`, `totalPages` na raiz | `_embedded.alunos`, `_links` e `page{number,size,totalElements,totalPages}` |
  | `GET` lista e por id em JSON, XML ou YAML | em **HAL (JSON)**; `Accept` de XML/YAML → **406** (erro em JSON). Sem `Accept` ou `*/*` → JSON (HAL) |
  | `POST`, `PUT`, `PATCH`, `DELETE` | **sem mudança** (JSON, XML e YAML, sem `_links`) |
  | erros | sem mudança (sempre JSON) |
- **Tropeços** (para o playbook):
  1. **500 em toda listagem** na primeira subida: `methodOn(AlunoController.class).desativar(...)` faz o Spring HATEOAS criar um *proxy do tipo de retorno* do método, e `AlunoResponse` é um `record` (final): `Cannot subclass final class`. O `buscar` funcionou porque devolve `EntityModel`. Solução: os links `disable`/`enable` são montados com `linkTo(AlunoController.class).slash(id)` mais o texto do caminho.
  2. **`test-compile` passou com erro de verdade escondido:** depois de apagar o `PageResponse`, sobrou o `.class` antigo em `target/` e a compilação incremental não reclamou. Só `./mvnw clean test-compile` mostrou os erros. Ao apagar uma classe, rode com `clean`.
  3. **Um `sed` quebrou o `pom.xml`** (inseriu o XML com `\t` literais, `Non-parseable POM`). Voltei o arquivo com `git checkout` e refiz pelo editor. Para editar XML, não use `sed` com escapes de tabulação.
  4. **`MockMvc` e os links:** os links da página nascem da URL da requisição, e o `MockMvc` não coloca `.param(...)` na query string. Nos testes de links com filtro a query vai na própria URL (`get("/alunos?nome=adriana&ativo=true&size=2")`). No `curl` real os filtros apareceram normalmente nos links.
- **Testes:** 88 passando (inclusive em ordem aleatória de classes). Reescritos `ContentNegotiationTests` e `SerializationTests`: XML e YAML passam a ser provados pelo `PATCH /alunos/1/enable` (idempotente, devolve um `AlunoResponse` comum); novos testes provam o HAL, o 406 de XML/YAML nos dois `GET`, `application/hal+json`, links de página, links `disable`/`enable` por estado, filtros nos links e que as escritas não trazem `_links`. Os 30 testes do service e do controller de listagem trocaram `$.content` por `$._embedded.alunos`, e `$.page`/`$.size`/`$.totalElements` por `$.page.*`.
- **Verificado no MySQL local** (2026-11-02): bateria completa por `curl` (30 verificações, 0 divergências), incluindo seguir os links `next` de uma página para outra, `disable` virando `enable` depois do `PATCH`, 406 em XML/YAML nos `GET`, e `PATCH` ainda respondendo em XML e YAML.
- **Postman:** os scripts da listagem passaram a esperar `_embedded`, `_links` e `page` (e `page.size` no item do `size=101`); a descrição do item de listagem menciona o HAL. JSON da coleção válido; a coleção em si não foi executada no Postman.
- **Ainda não está na produção:** a API da AWS (release 1.1.0) continua no contrato antigo.

## 29. Release 1.2.0: paginação, HATEOAS e 400 alunos na AWS

- **Status:** concluído (2026-11-02); deploy `success`, API nova respondendo na AWS
- **O que a release leva** (passos 26 a 28 mais a documentação): paginação com `Pageable` (padrão 20, máximo 50, `sort` inválido → 400), HAL (HATEOAS) na listagem e no `GET /alunos/{id}`, a migration `V3` com 400 alunos de exemplo e os `.md` atualizados. 9 commits sem contar os merges.
- **Ciclo, como foi feito:**
  1. PR #23 `feature/hateoas` → `develop` (merge commit `168aa44`, `build` verde).
  2. `release/1.2.0` cortada da `develop`; PR #24 `release/1.2.0` → `main`, `build` verde (88 testes).
  3. Antes do merge: o usuário confirmou RDS `Available`, Service com *Desired tasks* = 1 e a aceitação da `V3` (400 alunos no RDS). Conferi a única parte que a CLI enxerga: Service `ACTIVE`, 1 tarefa rodando na `:6`. Como a 1.1.0 exige o banco para subir, uma tarefa de pé também indica o RDS no ar.
  4. Merge com *merge commit* (`0d29d8e`) → a esteira rodou e terminou com `success`.
  5. Tag anotada `v1.2.0` em `0d29d8e`.
  6. PR #25 `main` → `develop` (merge commit `74cfbde`), para a `develop` receber a release.
- **Conferido na AWS** (CLI, perfil `crud-alunos`): Service `ACTIVE`, 1 tarefa `RUNNING`, deploy `COMPLETED`, Task Definition **`crud-alunos-task-definition:7`**; IP público novo em `<IP-publico>` (muda a cada tarefa).
- **Teste da API na AWS:** a lista responde em HAL com `page` = `{number 0, size 20, totalElements 408, totalPages 21}`; `_embedded.alunos`, links `first`, `self`, `next` e `last` com o IP da tarefa; `GET /alunos/8` devolve a Helena em HAL com o link `disable`; `ativo=false` → 40 inativos; `size=500` → reduzido para 50; `sort=senha` → 400 com a mensagem em português; `Accept: application/xml` na lista → 406; id inexistente → 404. Ou seja: o Flyway aplicou a `V3` no RDS (408 alunos = 8 + 400) e o contrato novo está em produção.
- **Primeira mudança de contrato em produção:** quem usava a API da 1.1.0 (`content`, `totalElements` na raiz; XML/YAML nos `GET`) passa a ver o formato do passo 28. Como é projeto de estudo, não há cliente real.
- **Os dados de exemplo agora estão no RDS.** Para remover, só com uma migration nova (`V4`) que apague as linhas; a `V3` não se edita.
- **Sobra para limpar:** a branch remota `release/1.2.0` (o sistema bloqueou o apagamento porque não foi pedido; é só pedir).

## 30. Segurança, parte 1: dependências (passo 28 do playbook)

- **Status:** dependências adicionadas; **build vermelho de propósito** até o passo 33 do playbook (ajuste dos testes). A branch `feature/seguranca` **não vai para a `develop` enquanto os testes estiverem falhando**; os passos 28 a 33 do playbook ficam todos nela, com um commit por passo, e o PR só abre quando o `build` voltar a ficar verde.
- `pom.xml`: `spring-boot-starter-security` (Spring Security 7.1.2, gerenciado pelo Boot 4.1.2), `com.auth0:java-jwt:4.4.0` (versão fixa do playbook) e `spring-security-test` (escopo `test`).
- **Observações sobre as versões:**
  - O Boot 4 também tem o starter de teste próprio `spring-boot-starter-security-test`; o playbook pede `spring-security-test`, que funciona com a versão gerenciada, e foi esse que usei.
  - O `java-jwt` 4.4.0 puxa o `jackson-databind` (Jackson **2**) só em tempo de execução. O projeto usa o Jackson **3** (`tools.jackson`) do Boot 4; os pacotes são diferentes, então convivem. Já existe o `java-jwt` 4.6.2, mas o playbook fixa 4.4.0 e mantive.
- **O aviso do passo, comprovado** (2026-11-02):
  - **Testes:** `mvnw clean test` → 59 falhas em 88 (`ContentNegotiationTests` 12, `SerializationTests` 7, `AlunoControllerTests` 40). Os `GET` recebem **401** e as escritas (`POST`, `PUT`, `PATCH`, `DELETE`) recebem **403** no `MockMvc`. Não quebraram `GlobalExceptionHandlerTests` (11), `AlunoServiceTests` (17) nem `ApicrudAlunosApplicationTests` (1).
  - **Aplicação real** (MySQL local): todas as rotas dão **401**, inclusive `POST` e `DELETE` e a raiz `/`, com `WWW-Authenticate: Basic` e **corpo vazio**, ou seja, **não** vem o nosso `ErrorResponse` em JSON. Isso será tratado quando o 401/403 em JSON entrar (pendência antiga da lista, "Erros que faltam").
  - O Spring Security cria sozinho um usuário `user` com senha gerada, anunciada no log na subida (`Using generated security password`). É o comportamento padrão, e será substituído pelo `UserDetailsService` do passo 29 do playbook; a senha gerada não foi usada nem registrada.
- **Efeito na AWS (ainda não executado, só para planejar):** quando a segurança for para uma release, a API pública deixa de responder sem token; o Postman e o teste pelo IP vão precisar de login; e o `JWT_SECRET` (passo 31, sem valor padrão) exige **um segredo novo no Secrets Manager, uma variável nova na Task Definition e permissão de leitura dele na role de execução**. Sem isso, a tarefa não sobe e o deploy trava. Isso entra no `Duvida-CICD.md` quando for executado.

## 31. Segurança, parte 2: usuário e permissões (passo 29 do playbook)

- **Status:** concluído na branch `feature/seguranca` (ainda sem PR: o build segue vermelho até o passo 33 do playbook)
- **Migration `V4__create_tables_users_and_permissions.sql`** (a `V3` já está na produção, então a próxima é a `V4`):
  - `permission` (`id`, `description` única);
  - `users` (`id`, `user_name` única, `full_name`, `password`, `account_non_expired`, `account_non_locked`, `credentials_non_expired`, `enabled`). O nome é `users`, no plural, porque `user` é palavra reservada em vários bancos;
  - `user_permission` (tabela de ligação, chave primária composta `id_user` + `id_permission`, duas chaves estrangeiras; apagar o usuário apaga as ligações dele, mas uma permissão em uso não pode ser apagada).
  - A collation `utf8mb4_unicode_ci` não diferencia maiúsculas de minúsculas, então `Admin` e `admin` são o mesmo `user_name` no MySQL (no H2 dos testes diferenciaria).
- **Entidades** (no pacote `model`, com os nomes de coluna explícitos):
  - `Permission implements GrantedAuthority`: `getAuthority()` devolve a `description`;
  - `User implements UserDetails`: os quatro `is...()` vêm das colunas booleanas, `getUsername()` devolve `userName`, `getAuthorities()` devolve as permissões; `@ManyToMany(fetch = EAGER)` com `@JoinTable(name = "user_permission", joinColumns = id_user, inverseJoinColumns = id_permission)`. EAGER porque o Spring Security usa as permissões fora de uma transação. A coluna `password` guarda **só o hash** (passo 30 do playbook); por ora não há usuário nem senha semeados.
  - Implementam `Serializable` (via `UserDetails` e `GrantedAuthority`), por isso têm `serialVersionUID`.
- **`UserRepository.findByUserName`** devolve `Optional<User>` (o playbook deixa livre; o `Optional` evita o `null`). **`UserService implements UserDetailsService`**: `loadUserByUsername` lança `UsernameNotFoundException("Usuário não encontrado")` sem repetir o nome digitado. Como esse bean existe, o Spring Boot **deixa de criar o usuário padrão com senha gerada**: a linha `Using generated security password` do log do passo 30 sumiu (0 ocorrências).
- **Testes novos (7):**
  - `UserServiceTests` (Mockito, 2): devolve o usuário encontrado; lança `UsernameNotFoundException` quando não existe;
  - `UserDetailsLoadingTests` (5, integração): carrega usuário e permissões pelo nome, usuário sem permissões, as quatro flags de conta preservadas (desativada, bloqueada, expirada), nome inexistente e que o bean é o `UserService`. Cada teste grava com `EntityManager`, dá `flush()` + `clear()` (esvazia o cache do Hibernate, para a leitura passar de verdade pelo SQL) e a transação é desfeita no fim, **sem deixar linhas no banco**.
- **A prova que importa foi no MySQL, não no H2** (o H2 cria o schema a partir das entidades e não exercita a migration): rodei o mesmo `UserDetailsLoadingTests` contra o `mysql-dev` passando por linha de comando `-Dspring.datasource.url=...`, `-Dspring.flyway.enabled=true`, `-Dspring.jpa.hibernate.ddl-auto=validate` e `-Dspring.sql.init.mode=never`. O Flyway aplicou a `V4`, o Hibernate **validou as entidades contra as tabelas reais** e os 5 testes passaram lendo do MySQL; depois `users`, `permission` e `user_permission` ficaram com 0 linhas. Esse é o "pronto quando" do passo.
- **Placar da suíte:** 95 testes, 59 falhas (as mesmas do passo anterior, por causa da segurança; os 7 novos passam). Aplicação sobe no MySQL e a rota segue dando 401 sem credenciais.
- **Para a AWS:** a próxima release que levar isso fará o Flyway criar as três tabelas no RDS (vazias). Nada a fazer no console por causa da `V4`.
- **Ainda não feito:** `permissions` só tem getter/setter de lista; não há `getRoles()` (entra com o JWT, passo 31 do playbook), nem endpoint de cadastro de usuário, nem usuário semeado.

## 32. Segurança, parte 3: hash de senha (passo 30 do playbook)

- **Status:** concluído na branch `feature/seguranca` (ainda sem PR: build vermelho até o passo 33 do playbook)
- **Decisão:** `config/PasswordConfig` expõe um `PasswordEncoder` que é um **`DelegatingPasswordEncoder` com só o BCrypt cadastrado** (padrão `bcrypt`). O playbook deixa a escolha entre BCrypt puro e o encoder delegante; escolhi o delegante porque o hash sai com o algoritmo no começo (`{bcrypt}$2a$10$...`), então dá para trocar o algoritmo padrão no futuro **sem invalidar as senhas antigas** (elas continuam sendo conferidas pelo prefixo). Não usei `PasswordEncoderFactories.createDelegatingPasswordEncoder()` de propósito: esse encoder padrão também aceita `{noop}` (senha em texto puro), `md5`, `sha` e outros inseguros; aqui um hash sem prefixo, ou com prefixo não cadastrado, é **recusado com erro** (`IllegalArgumentException`). O PBKDF2 do projeto de referência não foi usado (só faria sentido por compatibilidade com hashes já existentes).
- **O hash ocupa 68 caracteres** (`{bcrypt}` 8 + os 60 do BCrypt), bem dentro da coluna `users.password` (255).
- **Limite do BCrypt, descoberto rodando (não suposto):** nesta versão do Spring Security (7.1.2), uma senha com **mais de 72 bytes é recusada ao codificar** com `IllegalArgumentException: password cannot be more than 72 bytes`; não é truncada em silêncio (senha de exatamente 72 bytes funciona e o último caractere conta). Um futuro endpoint de cadastro precisa validar o tamanho e responder 400, senão vira 500. Está em teste permanente.
- **Testes novos (11):**
  - `PasswordConfigTests` (7, sem Spring): o hash não é a senha e começa com `{bcrypt}$2`; a mesma senha gera hashes diferentes (salt); certa confere e errada, ou com caixa diferente, não; hash sem prefixo, `{noop}` e `{md5}` dão erro; hash antigo com prefixo cadastrado continua valendo; senha de 72 bytes funciona; senha de 73 bytes é recusada;
  - `PasswordHashingIntegrationTests` (4, integração): o bean usa o prefixo `{bcrypt}`; **o banco guarda o hash e nunca a senha** (lido com SQL nativo `select password from users`); um `DaoAuthenticationProvider` autentica com a senha certa contra o hash vindo do banco e dá `BadCredentialsException` com a errada. Cada teste roda numa transação desfeita, sem deixar linhas.
- **Verificado no MySQL local:** o mesmo `PasswordHashingIntegrationTests` rodado contra o `mysql-dev` (mesmos `-D` do passo anterior: Flyway ligado e `ddl-auto: validate`) passou; `users` ficou com 0 linhas.
- **Placar da suíte:** 106 testes, 59 falhas (as mesmas da segurança). Os 18 testes novos de segurança (passos 29 e 30) passam.
- **O alerta do playbook sobre usuário semeado em migration:** **ainda não semeei nenhum usuário.** Quando o login (passo 32 do playbook) precisar de um, a decisão será tomada ali. Como o repositório é público, um hash de senha fraca ficaria exposto: se houver seed, a senha de estudo deve ser forte e não ficar escrita em arquivo versionado.

## 33. Segurança, parte 4: provedor de token JWT (passo 31 do playbook)

- **Status:** concluído na branch `feature/seguranca` (ainda sem PR: build vermelho até o passo 33 do playbook)
- **Arquivos novos** (pacote `security`): `JwtTokenProvider`, `TokenPair` (record com access token, refresh token e os dois instantes de expiração) e `InvalidTokenException`. Biblioteca: `java-jwt` 4.4.0, algoritmo **HS256**.
- **O que o provedor faz:**
  - **Gerar:** `createTokenPair(user)` devolve um par. Claims: `iss` (emissor `apicrud-alunos`), `sub` (usuário), `roles` (lista das permissões), `type` (`access` ou `refresh`), `iat` e `exp`. Access token de **15 minutos**, refresh de **24 horas** (`security.jwt.access-token-expiration` e `refresh-token-expiration`, em `15m` e `24h`).
  - **Validar:** `getAuthentication(accessToken)` confere assinatura, emissor, expiração e `type = access`; exige `exp` e `sub` presentes; **recarrega o usuário do banco** e recusa se foi apagado, desativado, bloqueado, expirado ou com credenciais expiradas (`AccountStatusUserDetailsChecker`). As permissões do `Authentication` são as **de agora**, não as que estavam no token.
  - **Renovar:** `renew(refreshToken)` aceita só refresh token (um access não renova), recarrega o usuário e devolve um **par novo** com as permissões atuais.
  - **Falhas:** tudo vira `InvalidTokenException` (herda de `AuthenticationException`, então o Spring Security a trata como 401). A mensagem é genérica (`Token expirado`, `Token inválido`, `Token ausente`) para não ajudar quem forja tokens.
- **Segredo:** `security.jwt.secret: ${JWT_SECRET}`, **sem valor padrão**, no mesmo estilo do passo 19. Mínimo de **32 bytes** (HS256). Falha na inicialização, com mensagem clara, em dois níveis: o `main` confere se `JWT_SECRET` existe (`A variável de ambiente JWT_SECRET não foi definida...`) e o provedor confere o tamanho (`...é curto demais: use pelo menos 32 caracteres aleatórios`). Conferido rodando a aplicação: sem a variável → falha com a primeira mensagem; com 10 caracteres → falha com a segunda; com 64 caracteres gerados na hora (`openssl rand -hex 32`) → sobe, e o segredo **não aparece no log**.
- **O `application.yml` de teste** repete a configuração (ele substitui o principal) com um segredo **falso de propósito**, `TESTE-nao-e-um-segredo-real-...`, que não protege nada e não é de nenhum ambiente. É o que a esteira usa, então o CI não precisa de `JWT_SECRET`.
- **401 em JSON:** o `GlobalExceptionHandler` ganhou o tratamento de `InvalidTokenException` → **401** com o nosso `ErrorResponse` (`erro: "Não autenticado"`), junto de um teste com controller falso. Isso cobre o 401 quando o token é validado dentro de um controller; o 401 da cadeia de filtros (rotas protegidas sem token) só existe quando o filtro e o `SecurityFilterChain` entrarem nos passos 32 e 33 do playbook.
- **Testes novos (32):**
  - `JwtTokenProviderTests` (25, unitários, sem Spring nem banco): conteúdo dos tokens; autenticação válida; **vencido**, **adulterado** na assinatura, **outro emissor**, **outro segredo**, **tipo errado** nos dois sentidos (refresh como access e access como refresh); ataque de **troca das `roles` mantendo a assinatura** (elevação de privilégio); **algoritmo `none`**; token assinado por nós **sem validade** e **sem usuário**; token nulo, vazio e mal formado; usuário apagado, desativado, bloqueado, expirado e com credenciais expiradas; permissões de agora e não as do token; renovação com permissões atuais, refresh vencido, refresh de usuário desativado; segredo ausente, curto (31) e de exatamente 32;
  - `JwtTokenIntegrationTests` (6, com o `UserService` e o banco de verdade): token de usuário do banco autentica e carrega as permissões; **desativar, bloquear ou apagar o usuário no banco derruba o token na validação seguinte**; revogar uma permissão no banco a tira do token renovado. Cada teste numa transação desfeita;
  - `GlobalExceptionHandlerTests` (+1): token inválido → 401 JSON.
- **Os testes pegam falha de verdade:** sabotei o código de propósito (tirei a conferência do `type` e a exigência de `exp`) e exatamente 3 testes quebraram: `refreshTokenNaoValeComoAccessToken`, `accessTokenNaoVaiLeComoRefreshToken` e `tokenAssinadoPorNosMasSemDataDeValidadeEhRecusado`. O código foi restaurado e os 25 voltaram a passar.
- **Verificado no MySQL local:** `JwtTokenIntegrationTests` rodado contra o `mysql-dev` (Flyway + `validate`) passou, e `users` e `permission` terminaram com 0 linhas.
- **Placar da suíte:** 138 testes, 59 falhas (as mesmas da segurança); os 32 novos passam.
- **Limites que o playbook pede para lembrar:** (1) sem lista de revogação **não existe logout de verdade**: o token vale até expirar, por isso o access é curto; para revogar de verdade seria preciso guardar os refresh tokens no servidor; (2) recarregar o usuário a cada validação custa uma consulta ao banco por requisição, troca consciente; (3) o refresh token é mais sensível: deve ir no corpo de um `POST`, nunca na URL; (4) a renovação devolve um par novo, mas **não invalida o refresh antigo** (mesma falta de revogação).
- **Lições do caminho:** o `sed` voltou a quebrar um arquivo de teste ao inserir código com tabulação (virou um `t` solto): para inserir código, usar o editor, não `sed`; o `AccountStatusUserDetailsChecker` fica em `org.springframework.security.authentication` (o IDE avisou).
- **A AWS vai precisar de um segredo novo** (ainda não executado, só planejado): sem `JWT_SECRET` na Task Definition a tarefa não sobe. Entra no `Duvida-CICD.md` quando for feito: segredo novo no Secrets Manager (≥ 32 caracteres aleatórios), variável `JWT_SECRET` na Task Definition como `ValueFrom`, e permissão de leitura dele na `ecsTaskExecutionRole` (o ARN do segredo novo precisa entrar na política inline). **Para rodar localmente também será preciso definir `JWT_SECRET`** (por exemplo na variável de usuário do Windows, como a `DB_PASSWORD`).

## 34. Segurança, parte 5: filtro e configuração (passo 32 do playbook)

- **Status:** concluído na branch `feature/seguranca` (ainda sem PR: 59 testes antigos falham por 401 até o passo 33 do playbook, que os ajusta)
- **Arquivos novos:**
  - `security/JwtTokenFilter` (`OncePerRequestFilter`): lê `Authorization: Bearer <token>` e, se o token for válido, autentica. Cabeçalho ausente ou token inválido **não derruba a requisição**: ela segue sem autenticação, e as rotas protegidas respondem 401 pelo entry point, enquanto as públicas continuam funcionando. O esquema `Bearer` não diferencia maiúsculas de minúsculas (RFC 6750).
  - `config/SecurityConfig` (`SecurityFilterChain`): **stateless** (nada de sessão), **CSRF desligado** (não há cookie de sessão para proteger), `.cors(Customizer.withDefaults())` (a configuração de CORS é o passo 36; por ora não há cabeçalhos de CORS), `httpBasic`, `formLogin` e `logout` desligados (a única forma de entrar é o token), `anyRequest().authenticated()` como regra final, e o filtro JWT antes do `UsernamePasswordAuthenticationFilter`.
  - `security/RestAuthenticationEntryPoint` (**401**) e `security/RestAccessDeniedHandler` (**403**), ambos escrevendo o **mesmo `ErrorResponse` em JSON** por meio de `SecurityErrorWriter`. É preciso, porque esses erros nascem nos filtros, antes de qualquer controller, e o `GlobalExceptionHandler` não os alcança. A mensagem do 401 é a mesma para qualquer causa (sem token, vencido, adulterado...), para não revelar por que o token foi recusado. O cabeçalho `WWW-Authenticate: Basic` do passo 28 sumiu.
- **Rotas públicas, a lista inteira** (revisar antes de cada deploy): `OPTIONS` em qualquer caminho (pré-voo de CORS), `POST /auth/signin` e `POST /auth/refresh`. **Diferenças do playbook, de propósito:** deixei de fora o Swagger (o projeto não tem) e o `/auth/register` (a regra dele só entra no passo 34 do playbook). Os controllers de `/auth` ainda não existem: hoje essas duas rotas estão liberadas mas respondem 404. Só são públicas para `POST` (um `GET /auth/signin` dá 401).
- **Armadilha evitada:** o `JwtTokenFilter` **não é `@Component`**. Um filtro que é bean do Spring é registrado também fora da cadeia do Spring Security e passaria a rodar duas vezes; por isso a `SecurityConfig` o cria com `new` e o coloca na cadeia.
- **Testes novos (22):**
  - `SecurityErrorHandlersTests` (3, unitários): 401 e 403 em JSON no formato `ErrorResponse`, na ordem dos campos, sem vazar o texto da exceção;
  - `SecurityFilterChainTests` (19, integração, cadeia inteira com o banco, em transação desfeita): 401 em JSON sem token e em todos os métodos e rotas, inclusive `/` e rotas inexistentes; Swagger e `/auth/register` não são públicos; token vencido, adulterado, de outro emissor, de outro segredo, refresh como access, cabeçalho sem `Bearer`, `Basic`, `Bearer` vazio e lixo, token na URL (não vale), usuário desativado e apagado: tudo 401; com token válido a listagem funciona em HAL, `bearer` minúsculo vale, `POST`, `PUT`, `PATCH` e `DELETE` funcionam sem token CSRF, erros de negócio continuam no `ErrorResponse`; nenhuma sessão nem `Set-Cookie`; `OPTIONS` e `POST` em `/auth/signin` e `/auth/refresh` não dão 401, `GET`/`PUT` nelas dão 401, e uma rota pública ignora um token ruim.
- **Os testes pegam falha de verdade:** sabotei a configuração de duas formas, com `anyRequest().permitAll()` (12 testes quebraram) e sem o filtro JWT na cadeia (5 quebraram). Arquivo restaurado, 22 passando.
- **Prova ponta a ponta com HTTP de verdade** (aplicação no MySQL local, `curl`, um usuário temporário na tabela `users`, apagado no fim): os tokens foram montados por um script em Node que faz o HS256 **à mão**, uma implementação independente do `java-jwt`, o que também confere o formato. Resultado, todos como esperado: sem token 401 em JSON; token válido 200 (HAL, links `disable`); vencido, adulterado, `alg: none`, refresh como access, `Basic` e usuário inexistente → 401; `POST` 201 e `DELETE` 204 com token; `404` de negócio com token; **zero `Set-Cookie`**; **o mesmo token** passou a dar 401 quando o usuário foi desativado no banco, voltou a 200 ao reativar e deu 401 ao apagar; `OPTIONS /alunos` → 200, `POST /auth/signin` → 404, `GET /auth/signin` → 401. Depois: 0 usuários e 408 alunos (nada ficou), e o segredo não aparece no log. (O script não foi para o repositório: o endpoint de login vai substituí-lo.)
- **Placar da suíte:** 160 testes, 59 falhas, todas `Status expected:<2xx/4xx> but was:<401>` nos testes antigos (`ContentNegotiationTests` 12, `SerializationTests` 7, `AlunoControllerTests` 40). Antes do filtro as escritas davam 403 nos testes; agora dão 401 (CSRF desligado). Os 22 novos passam.
- **O 403 em JSON só tem teste unitário:** nenhuma rota pede uma permissão específica ainda (o `hasAuthority("ADMIN")` do `/auth/register` entra no passo 34 do playbook), então não há como provar o 403 pela cadeia inteira por enquanto.
- **Lições do caminho:** o `ObjectMapper` do Boot 4 é o do Jackson 3 (`tools.jackson.databind.ObjectMapper`) e é o que se injeta nos tratadores de erro; ao renomear um teste impreciso (`...SemTokenCsrf...` virou `...ComTokenFuncionamSemPrecisarDeTokenCsrf`), conferir o nome, porque a primeira versão dizia o contrário do que o teste fazia.

## 35. Segurança, parte 6: ajustar os testes (passo 33 do playbook)

- **Status:** concluído na branch `feature/seguranca`; **o build voltou a ficar verde** (PR aberto)
- **O que foi feito:** os três arquivos de testes de contrato que quebraram por 401, `AlunoControllerTests`, `ContentNegotiationTests` e `SerializationTests`, passaram de `@AutoConfigureMockMvc` para `@AutoConfigureMockMvc(addFilters = false)`, cada um com um comentário avisando que **não exercitam a segurança**. Eram os 59 testes que falhavam desde o passo 28. O segredo de teste (`security.jwt.secret`) no `src/test/resources/application.yml` já tinha entrado no passo 31 (o 33 do playbook o pede de novo).
- **Por que `addFilters = false` e não mandar token em cada teste:** esses testes verificam o *contrato* (formato HAL, paginação, erros de negócio, XML/YAML nos `PATCH`), e não quem pode chamar. Tirar os filtros mantém o foco e evita repetir o token em 59 chamadas. O preço, que o playbook avisa: **eles não detectam uma rota que ficou aberta por engano**.
- **Onde a segurança é testada, de verdade:** `security/SecurityFilterChainTests` é o **único** teste com `@AutoConfigureMockMvc` de filtros ligados (conferido por `grep`), e já tem os 401 (sem token, vencido, adulterado, outro emissor, refresh como access, Basic, usuário desativado ou apagado), as rotas públicas e o stateless. Se um dia alguém liberar uma rota sem querer, é ele que quebra. O 403 pela cadeia inteira só será testado quando houver uma rota com `hasAuthority` (passo 34 do playbook).
- **Pronto quando, comprovado:** `mvnw -B clean verify`, com `JWT_SECRET` e `DB_PASSWORD` **fora do ambiente** (como na esteira): 160 testes, 0 falhas, `BUILD SUCCESS`. Também passou em ordem aleatória de classes (`-Dsurefire.runOrder=random`), e o `docker compose build` (passo da esteira) terminou com sucesso com as dependências de segurança.
- **Resumo da Fase 6 até aqui** (passos 28 a 33 do playbook, nossa numeração 30 a 35): dependências → usuário e permissões (`V4`) → hash de senha BCrypt delegante → provedor JWT → filtro e configuração → testes ajustados. 160 testes (eram 88 antes da segurança), 72 deles novos. Ainda **não há** endpoint de login (`/auth/signin`), nem usuário no banco: a API fica 100% protegida e ninguém consegue entrar até os próximos passos.
- **Atenção para a produção:** esta branch muda o comportamento da API para quem a chama (tudo passa a exigir token) e exige `JWT_SECRET` para a aplicação subir. **Não fazer release com isso antes de existir o login e o segredo na AWS**, senão a API em produção fica inacessível e a tarefa nem sobe (ver o planejamento no `memoria.md`).
- **PR #27 mesclado na `develop`** (merge commit `4063d2d`, 2026-11-02, `build` verde em 1m37s). A `main` e a AWS continuam na release 1.2.0, sem segurança.

## 36. Segurança, parte 7: endpoints de autenticação (passo 34 do playbook)

- **Status:** concluído na branch `feature/autenticacao`
- **Endpoints** (`AuthController`, em `/auth`, **só JSON**: são rotas de credenciais e tokens, então XML/YAML dão 406/415):
  - `POST /auth/signin` (público): `{username, password}` → `200` com `{tokenType: "Bearer", accessToken, accessExpiresAt, refreshToken, refreshExpiresAt}`, com `Cache-Control: no-store` e `Pragma: no-cache` (resposta com token nunca deve ficar em cache);
  - `POST /auth/refresh` (público): `{refreshToken}` **no corpo** (nunca na URL) → `200` com um par novo; só aceita refresh token;
  - `POST /auth/register` (**só ADMIN**): `{username, fullName, password}` → `201` com `{id, username, fullName, enabled, permissions}` (nunca a senha nem o hash); `409` se o usuário já existe; `400` para dados inválidos.
- **Classes novas:** `AuthService`, `AuthController`, DTOs (`SignInRequest`, `RefreshRequest`, `RegisterRequest`, `TokenResponse`, `UserResponse`), `PermissionRepository`, `InvalidCredentialsException`; `UserRepository.existsByUserNameIgnoreCase`; na `SecurityConfig`, o bean `AuthenticationManager` (`DaoAuthenticationProvider` + `UserService` + o `PasswordEncoder`) e a regra `.requestMatchers("/auth/register").hasAuthority("ADMIN")` (vale para qualquer método; sem token dá 401 e sem a permissão dá 403).
- **A regra do playbook: não revelar se o usuário existe.** Todo erro de login vira **a mesma** `InvalidCredentialsException` (`401`, `"Credenciais inválidas"`): usuário inexistente, senha errada, conta desativada, bloqueada, expirada ou com credenciais expiradas. Isto importa porque o `DaoAuthenticationProvider` confere o **estado da conta antes da senha**: sem esse tratamento, uma conta desativada responderia diferente mesmo com a senha errada. O Spring ainda gasta o tempo de uma comparação de hash quando o usuário não existe: medido, usuário inexistente levou cerca de 93 a 116 ms e senha errada de um usuário existente cerca de 97 a 101 ms (5 tentativas cada), ou seja, indistinguíveis. Não há bloqueio por tentativas nem limite de requisições (força bruta contra uma senha fraca continua possível; fica anotado como limitação).
- **Primeiro ADMIN (decisão minha, a conferir):** sem nenhum usuário, ninguém consegue chamar `/auth/register`. A migration `V5__seed_permissions_and_admin.sql` insere as permissões `ADMIN` e `USER` e o usuário `admin` (com as duas). O playbook já avisa que semear usuário em migration é "aceitável em estudo, não em produção". Como o repositório é público, **só o hash** vai para o arquivo: a senha é aleatória, de 24 caracteres, gerada na hora e entregue **uma única vez** ao responsável; **não está em nenhum arquivo** (nem aqui). O hash foi gerado com o mesmo `BCryptPasswordEncoder` do projeto, por um programa Java de uma classe só. Para trocar a senha: gerar um novo hash `{bcrypt}...` e rodar um `UPDATE` (ou criar uma migration nova; a `V5` nunca se edita). Em produção real, o admin deveria ser criado fora do repositório.
- **Permissão padrão do cadastro:** `USER`. O corpo do `register` **não escolhe permissões**: um `permissions: ["ADMIN"]` enviado é ignorado (testado), então um ADMIN não cria outro ADMIN pela API, só o banco faz isso.
- **Validação do cadastro:** `username` de 3 a 100 caracteres só com letras, números e `. _ @ -`; `fullName` de 3 a 150; `password` de 8 a 72 caracteres **e no máximo 72 bytes** (o limite do BCrypt é em bytes: 40 letras acentuadas são 40 caracteres mas 80 bytes; sem essa checagem viraria 500). A unicidade do nome ignora maiúsculas e minúsculas (`existsByUserNameIgnoreCase`, que vale também no H2 dos testes).
- **Fechei uma limitação antiga:** o `GlobalExceptionHandler` agora trata `DataIntegrityViolationException` como **409** (sem vazar o texto do banco). Era o caso de dois pedidos simultâneos com o mesmo e-mail de aluno ou o mesmo usuário, que passavam juntos pela checagem do service e davam 500.
- **Testes novos (40):**
  - `AuthControllerTests` (30, integração, **filtros ligados**, banco de verdade, transação desfeita): login (tokens e formato, `no-store`, os tokens funcionam na API, sem senha na resposta, **mesma resposta** para usuário inexistente e senha errada, conta desativada/bloqueada/expirada/credenciais expiradas com a senha certa dão a mesma resposta genérica, senha de 200 caracteres dá 401 e não 500, corpo inválido, só JSON, GET não é público); refresh (par novo funciona, access não renova, vencido/adulterado/lixo, usuário desativado/apagado, corpo sem o campo, token na URL não vale); register (401 sem token ou com token ruim, **403 em JSON** para quem não é ADMIN, 201 só para ADMIN com a permissão USER, **hash gravado e nunca a senha**, o novo usuário consegue entrar mas não é admin, `permissions` no corpo é ignorado, 409 inclusive com outra caixa, 400 para dados inválidos, senha acima de 72 bytes dá 400, senha de exatamente 72 caracteres funciona, só JSON, admin desativado dá 401);
  - `AuthServiceTests` (8, Mockito): login certo; **7 tipos de falha viram a mesma exceção**; refresh delega; register aplica o hash e a permissão padrão, conflito sem tocar na senha, senha acima de 72 bytes recusada antes de qualquer consulta, permissão `USER` ausente no banco é erro de configuração e não cria usuário sem permissão;
  - `GlobalExceptionHandlerTests` (+2): credenciais inválidas → 401 genérico; violação de restrição → 409 sem vazar o detalhe.
  - Agora o **403 pela cadeia inteira tem teste**, o que faltava no passo anterior (nenhuma rota exigia permissão).
- **Os testes pegam falha de verdade (sabotagens, depois restauradas):** (1) o login tratando só `BadCredentials` → 2 testes quebraram (conta desativada e a lista de falhas); (2) sem a regra de ADMIN no `/auth/register` → 4 quebraram (os de 403, que receberam 201); (3) senha gravada sem hash → 4 falhas e 1 erro (o login do usuário recém-criado deu 500).
- **Contra o MySQL real:** os testes de autenticação e segurança (64) passaram com Flyway e `validate`, e o Flyway aplicou a `V5`. Banco depois dos testes: só o `admin` com `ADMIN` e `USER`, e as duas permissões.
- **Prova ponta a ponta com HTTP de verdade** (aplicação no MySQL local, senha real do admin lida de um arquivo temporário fora do repositório e apagado depois): login do admin 200 e `GET /alunos` 200; `register` sem token 401; `register` pelo admin 201 (só `USER`); duplicado com outra caixa 409; senha curta 400; o usuário novo entra (200), lê `/alunos`, e **não** consegue registrar (403 em JSON); refresh 200 e o access novo funciona; refresh com um access 401; refresh usado como Bearer 401; **desativar o usuário no banco derrubou o token dele e também o login com a senha certa (401)**. Limpeza: o usuário de teste foi apagado (ficou só o `admin`, 2 ligações, 408 alunos); a senha do admin e o segredo JWT **não aparecem no log**.
- **Placar:** 200 testes (eram 160), `mvn -B verify` verde com `JWT_SECRET` e `DB_PASSWORD` fora do ambiente, também em ordem aleatória de classes; `docker compose build` ok.
- **Tropeços do caminho:** (1) o `java` de um arquivo só precisou de caminhos do Windows no classpath (`cygpath -w`), não os do Git Bash; (2) no teste unitário, reprogramar um mock dentro de um laço com `when(...).thenThrow(...)` chama o método já programado e estoura: o certo é `doThrow(...).when(...)`; (3) o `sed` continua proibido para inserir código com tabulação, usei só o editor; (4) um `heredoc` grande com muitos apóstrofos falhou no shell: textos longos vão pelo editor.
- **Efeito na AWS (planejado, não executado):** a próxima release que levar isto fará o Flyway criar as tabelas `users`, `permission` e `user_permission` e o `admin` no RDS. Continua valendo: precisa do segredo novo `JWT_SECRET` (Secrets Manager, Task Definition e permissão na role), e quem for usar a API passa a fazer login antes. Ainda faltam, no playbook, o CORS (passo 36) e os testes de segurança restantes (passo 38).

## 37. Segurança, parte 8: autorização por rota (passo 35 do playbook)

- **Status:** concluído na branch `feature/autorizacao`. Ela foi feita **empilhada sobre o PR #29** (autenticação), que o sistema não deixou eu mesclar enquanto você não pedisse explicitamente; depois do seu "pode" ele entrou na `develop` (merge commit `2ae49ed`, 2026-11-02) e este PR passou a poder sair.
- **A ideia do passo:** autenticação responde "quem é você"; autorização responde "o que você pode fazer". Até aqui qualquer usuário logado, mesmo sem nenhuma permissão, fazia tudo em `/alunos`. O alerta do playbook ("No projeto de referência, qualquer usuário autenticado acessa todas as rotas de dados") era exatamente este nosso estado.
- **Decisão de projeto (o playbook deixa em aberto; fica numa tabela só, em `SecurityConfig`, fácil de mudar):**

  | Rota | Quem pode |
  | --- | --- |
  | `GET` e `HEAD` em `/alunos` e `/alunos/{id}` | `USER` ou `ADMIN` |
  | `POST /alunos`, `PUT /alunos/{id}`, `PATCH /alunos/{id}/disable`, `PATCH /alunos/{id}/enable` | `USER` ou `ADMIN` |
  | `DELETE /alunos/{id}` (destrutivo e irreversível) | só `ADMIN` |
  | `POST /auth/register` | só `ADMIN` (do passo anterior) |
  | `POST /auth/signin`, `POST /auth/refresh` e `OPTIONS` | públicos |
  | **qualquer outra rota** | **negada** |

  O raciocínio: apagar não tem volta, então fica só com o ADMIN; criar, editar, ativar e desativar são reversíveis (o `disable` é uma "exclusão lógica"), então o USER também pode.
- **Escolha: regras no `SecurityConfig` (`requestMatchers`) e não `@PreAuthorize`.** O playbook oferece as duas. Deixei tudo em um lugar só, onde dá para ler a lista inteira de quem pode o quê (e a lista de públicas). `@EnableMethodSecurity` duplicaria a regra em dois lugares. As permissões viraram constantes (`security/Authorities`: `ADMIN`, `USER`), porque um texto digitado errado compilaria e deixaria a rota fechada para todos.
- **Regra final mudou de `authenticated()` para `denyAll()`** (diferença do passo 32 do playbook, que pedia `anyRequest().authenticated()`). Com `authenticated()`, **toda rota nova nasceria aberta a qualquer usuário logado**, e é essa a armadilha que o playbook aponta ("parar na autenticação"). Com `denyAll()` a rota nova nasce **fechada** e só abre quando alguém escreve a regra dela. Efeito colateral: uma rota inexistente agora responde **403 para quem está logado** (antes 404) e 401 para anônimo.
- **O HEAD tem regra própria** (`requestMatchers(HttpMethod.HEAD, ...)`): o Spring MVC trata HEAD como GET, mas o Spring Security compara o método exato. Sem a linha, o HEAD cairia no `denyAll` (mutação testada).
- **A autorização vem antes da existência:** um usuário sem permissão recebe 403 também para um id que não existe (e não 404), então não consegue sondar quais alunos existem.
- **Quem manda é o banco, não o token:** o filtro recarrega o usuário a cada requisição, então as permissões vêm do banco. Testado: um token emitido quando o usuário era ADMIN passa a receber 403 no `DELETE` assim que o ADMIN é revogado no banco, e um token forjado por dentro com `"roles":["ADMIN"]` para um usuário que só é USER também recebe 403.
- **Testes novos (12 + ajustes):**
  - `AuthorizationTests` (12, integração, filtros ligados, banco de verdade, transação desfeita): uma **matriz** de 11 chamadas por perfil (`GET /alunos`, `GET /alunos/{id}`, `HEAD`, `POST`, `PUT`, `PATCH disable`, `PATCH enable`, `DELETE`, e três rotas sem regra) contra 6 perfis (anônimo, logado sem permissão, logado com outra permissão, `USER`, `ADMIN`, `USER`+`ADMIN`); o 403 do `DELETE` é JSON e **não apagou nada**; id inexistente dá 403 sem permissão e 404 com permissão; permissão revogada no banco vale na requisição seguinte; token com `roles` forjado; as rotas `/auth` seguem com as regras próprias;
  - `SecurityFilterChainTests` (ajustado): os tokens de teste agora têm a permissão `USER` (antes bastava estar logado); a rota inexistente passou a 403; o teste que apaga usa um ADMIN e prova que o USER recebe 403.
  - O 403 pela cadeia inteira agora tem teste para as rotas de dados.
- **O que as mudanças quebraram, de propósito:** ao ligar as regras, 5 testes antigos falharam, todos por 403 (tokens sem permissão; uma rota inexistente que passou de 404 a 403). Foi o sinal de que as regras estavam valendo.
- **Os testes pegam falha de verdade (sabotagens, depois restauradas, arquivo idêntico ao original):** (1) deixar o USER apagar → 5 testes quebraram; (2) voltar a regra final para `authenticated()` → 6 quebraram; (3) fechar o `PUT` só para ADMIN → 2 quebraram; (4) esquecer a regra do HEAD → 3 quebraram.
- **Contra o MySQL real:** `AuthorizationTests`, `SecurityFilterChainTests` e `AuthControllerTests` (61 testes) passaram com Flyway e `validate`; o banco ficou só com o `admin`, 2 permissões e 408 alunos.
- **Prova ponta a ponta com HTTP de verdade (44 verificações, 0 divergências):** a mesma matriz com quatro perfis reais: anônimo, usuário sem permissão (criado no banco), `USER` (cadastrado pela API e com **login de verdade**) e `ADMIN` (token montado em Node). Anônimo 401 em tudo; sem permissão 403 em tudo (inclusive id inexistente); USER com 403 só no `DELETE` e nas rotas sem regra; ADMIN apaga (204). O 403 vem em JSON e o aluno não foi apagado. Banco limpo no fim (só o `admin`, 408 alunos).
- **Placar:** 212 testes (eram 200), `mvn -B verify` verde com `JWT_SECRET` e `DB_PASSWORD` fora do ambiente, também em ordem aleatória de classes; `docker compose build` ok.
- **Limite honesto:** a regra "USER apaga? não" é uma escolha minha, não uma verdade. Se o modelo de negócio quiser outra divisão (por exemplo, só ADMIN escreve), basta mudar as linhas do `SecurityConfig` e a matriz do `AuthorizationTests`. Também não existe ainda uma forma de dar a permissão ADMIN a outro usuário pela API: isso só se faz no banco.
- **Contratos dos testes antigos:** os testes de contrato (`addFilters = false`) não foram tocados e seguem verdes, por não exercitarem a segurança; é a matriz acima que guarda as permissões.

## 38. Segurança, parte 9: CORS (passo 36 do playbook)

- **Status:** concluído na branch `feature/cors`, **empilhada** sobre `feature/autorizacao` (PR #30). O PR #29 (autenticação) já foi mesclado na `develop`; os próximos entram na ordem #30 (autorização), depois este, depois o do endurecimento.
- **O que é CORS:** o navegador, por padrão, não deixa uma página de um domínio ler a resposta de uma API de outro domínio. A API precisa dizer, por cabeçalhos, quais domínios (origens) ela autoriza. Antes de uma chamada "arriscada" (com `Authorization` ou `Content-Type: application/json`, ou métodos como `PUT`/`DELETE`), o navegador manda um pedido de teste, o **pré-voo** (`OPTIONS`), **sem token**. É ele que o Spring Security não pode barrar.
- **Decisão (o playbook diz "somente as origens que realmente chamam a API"):** **hoje nenhum navegador chama esta API** (não há front-end), então a lista começa **vazia**, ou seja, sem CORS nenhum. As origens entram por configuração: `cors.allowed-origins`, vinda da variável de ambiente `CORS_ALLOWED_ORIGINS` (várias separadas por vírgula, espaços aparados). Diferente de um segredo, o valor vazio é um padrão **seguro**, então pode ter valor padrão. Na AWS não é preciso fazer nada por enquanto; quando houver um front-end, basta uma variável na Task Definition com o domínio dele.
- **Implementação:** `WebConfig.addCorsMappings` (como o playbook pede), para `/**`: métodos `GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS`; cabeçalhos `Authorization, Content-Type, Accept`; **sem credenciais** (`allowCredentials(false)`: a autenticação é pelo cabeçalho, não por cookie); `Max-Age` de 3600 s (o navegador guarda o pré-voo por uma hora); e o cabeçalho **`Location` exposto**, porque o navegador só deixa o JavaScript ler cabeçalhos "seguros" e o `POST /alunos` devolve a URL do aluno criado nesse cabeçalho. A `SecurityConfig` já tinha `.cors(Customizer.withDefaults())` e `OPTIONS` liberado desde o passo 32.
- **Validação na subida (a aplicação não sobe com configuração perigosa ou errada):** `*` é recusado em qualquer forma (`*`, `https://*`, `https://*.exemplo.com`), com a mensagem `CORS_ALLOWED_ORIGINS não pode usar '*'`; a origem precisa ser `http(s)://domínio[:porta]`, **sem barra no fim e sem caminho** (é como o navegador a envia). Recusadas: `https://x.com/`, `https://x.com/a`, `x.com`, `ftp://x.com`, `https://`, porta que não é número, espaço, `javascript:`.
- **Comportamentos do Spring que descobri rodando (não supus):**
  1. Origem **não** permitida, método fora da lista, ou **só** cabeçalhos fora da lista → `403` com o texto simples `Invalid CORS request` (não é o nosso JSON: quem o escreve é o filtro de CORS, e o navegador nem entrega o corpo ao JavaScript).
  2. Pré-voo que mistura cabeçalho permitido e proibido (`authorization,x-outro`) → **`200`** e a lista devolvida em `Access-Control-Allow-Headers` traz **só os permitidos** (`authorization`). Quem barra é o navegador, que comparou o que pediu com o que foi liberado e não envia a requisição de verdade.
  3. A lista de cabeçalhos volta em minúsculas e com espaço depois da vírgula (`authorization, content-type`).
  4. **O aviso do playbook é mais sutil do que parece.** `.cors()` ligado e `OPTIONS` liberado são duas proteções que se cobrem: esquecer **uma** só não derruba o pré-voo (medido). Esquecer só o `.cors()` quebra outra coisa: as respostas **geradas pela segurança** (o 401 e o 403) ficam sem os cabeçalhos de CORS, e o navegador esconderia a mensagem de erro e mostraria só "erro de CORS". Esquecer só o `OPTIONS` livre não aparece nos testes de CORS, só em `SecurityFilterChainTests`. Esquecer **as duas** é que faz o pré-voo virar 401.
- **Testes novos (44 mais os ajustes; 22 + 22 de CORS):**
  - `CorsTests` (14, integração, **cadeia de segurança ligada**): pré-voo da origem permitida passa **sem token** (200, `Allow-Origin` igual à origem pedida, métodos, cabeçalhos, `Max-Age`, **sem** `Allow-Credentials`); nunca devolve `*`; todos os métodos da API; origem não permitida → 403 sem cabeçalho de CORS; origens parecidas barradas (`app.exemplo.com.site-malicioso.com`, subdomínio, `http` no lugar de `https`, porta diferente, `null`); método fora da lista → 403; cabeçalho fora da lista → 403; mistura permitido/proibido → só o permitido volta; requisição real da origem permitida recebe `Allow-Origin`; `Location` exposto no `POST`; requisição de origem não permitida é barrada **mesmo com token válido** e sem vazar dados; o **401 de uma origem permitida também traz CORS**; o CORS não abre rota nenhuma (o pré-voo do `DELETE` passa, mas o `DELETE` sem token continua 401); sem `Origin`, sem cabeçalhos de CORS;
  - `WebConfigCorsTests` (6, unitários): `*` recusado em 4 formas; 8 formatos de origem inválida recusados; origens válidas aceitas e espaços aparados; entradas vazias ignoradas; sem origens não se registra nada; com origens se registra exatamente a configuração acima (inclusive sem curinga e sem credenciais);
  - `CorsDisabledTests` (2): com a lista vazia (o padrão), nenhuma origem recebe cabeçalho de CORS e a rota continua protegida.
- **Os testes pegam falha de verdade (sabotagens, depois restauradas, arquivos idênticos aos originais):** (1) sem `.cors()` → 1 teste quebrou (o do 401 com CORS); (2) sem `OPTIONS` liberado → 1 quebrou (`SecurityFilterChainTests`); (3) `allowedOrigins("*")` → 7 quebraram; (4) `allowCredentials(true)` → 3 quebraram; (5) validação do `*` removida → 1 quebrou; (6) **sem `.cors()` e sem `OPTIONS` ao mesmo tempo** (o cenário do aviso) → 11 quebraram, todos com o pré-voo virando 401.
- **Prova num navegador de verdade (Chrome, aplicação no MySQL local com `CORS_ALLOWED_ORIGINS=http://localhost:5173`, dois mini-sites de teste, um na origem permitida e outro numa porta não permitida):**
  - da origem **permitida** (`http://localhost:5173`): `GET /alunos` com token → 200 e o JavaScript leu os 408 alunos; `GET` sem token → 401 e o JavaScript **leu a mensagem JSON** (`Não autenticado`); `POST` com `Authorization` e JSON (com pré-voo) → 201 e o `Location` foi **legível**; `DELETE` com pré-voo → 204; um cabeçalho não liberado (`X-Teste`) → **bloqueado pelo navegador** (`TypeError: Failed to fetch`);
  - da origem **não permitida** (`http://localhost:5174`): `GET` com token, `GET` sem token e `POST` com token → os três **bloqueados** (`Failed to fetch`).
  - Conferido no banco depois: o `POST` bloqueado **nunca criou nada** (0 alunos "Nao Deve Criar"), o aluno criado e apagado pelo teste não sobrou, e ficaram 408 alunos e só o usuário `admin`. O token de teste (montado em Node, de vida curta) e o segredo daquela execução foram descartados.
- **Placar:** 234 testes (eram 212), `mvn -B verify` verde com `JWT_SECRET`, `DB_PASSWORD` e `CORS_ALLOWED_ORIGINS` fora do ambiente (como na esteira), também em ordem aleatória de classes; `docker compose build` ok.
- **Limites:** (1) o `403` de CORS é texto simples, e não o `ErrorResponse` em JSON (personalizar exigiria trocar o processador de CORS do Spring, sem ganho prático, já que o navegador não mostra o corpo); (2) CORS **não é segurança de servidor**: ele só instrui o navegador. `curl` e qualquer programa ignoram CORS, e quem protege a API continua sendo a autenticação e a autorização; (3) a lista de origens é por ambiente, então produção e desenvolvimento têm listas diferentes.
- **Tropeços do caminho:** assumi que um pré-voo com cabeçalho proibido misturado seria barrado com 403, e o Spring responde 200 com a lista reduzida (corrigi o teste e a anotação); assumi o formato `authorization,content-type` e vem com espaço.

## 39. Segurança, parte 10: credenciais iniciais e endurecimento (passo 37 do playbook)

- **Status:** concluído na branch `feature/endurecimento`, a última da pilha: PR #29 (já mesclado) → PR #30 `feature/autorizacao` → `feature/cors` → `feature/endurecimento`. **Falta a parte do console da AWS** (roteiro abaixo), que é do responsável.
- **O "pronto quando" do passo, e por que não estava pronto:** "não existe login conhecido que funcione no ambiente publicado". O usuário `admin` da `V5` tinha uma senha que foi **mostrada no chat**, ou seja, era um login conhecido que iria para o RDS na próxima release.
- **Três itens do playbook:**
  1. **Segredo JWT forte no gerenciador de segredos:** o código já não tem valor padrão (`security.jwt.secret: ${JWT_SECRET}`; sem a variável a aplicação não sobe, e um guarda de teste impede que alguém coloque um valor padrão). Falta criar o segredo no Secrets Manager (roteiro abaixo).
  2. **Remover o login conhecido e criar o primeiro admin por um meio controlado:** veja abaixo.
  3. **Swagger e `v3/api-docs`:** **adiado**. O projeto ainda não tem Swagger; por decisão do responsável, só se valida quando ele existir. Tirei esses caminhos dos testes novos. (Continua o teste do passo 34, que só confirma que as rotas de documentação não estão entre as públicas; quando o Swagger entrar, decidir se fica protegido ou desligado em produção.)
- **Migration `V6__remove_seeded_admin.sql`** (a `V5` **não** foi editada: nunca se edita uma migration aplicada): `DELETE FROM users WHERE user_name = 'admin' AND password = '<o hash exato da V5>'`. A condição pela senha é de propósito: **só remove o usuário se ele ainda tiver a senha semeada**. Se alguém já trocou a senha dele, ele é legítimo e fica (provado). As ligações em `user_permission` saem junto (`ON DELETE CASCADE`). A V5 continua criando as permissões `ADMIN` e `USER`, de que o sistema precisa. Efeito num ambiente novo ou no RDS: a V5 cria o admin e a V6 o apaga na mesma subida do Flyway, antes de a aplicação responder a qualquer requisição.
- **Criação controlada do primeiro administrador (`AdminBootstrapRunner`, um `ApplicationRunner`):** a ideia do playbook, "variável de ambiente numa execução única". Como usar: definir `ADMIN_BOOTSTRAP_PASSWORD` (e, se não quiser `admin`, `ADMIN_BOOTSTRAP_USERNAME`), subir uma vez e **remover a variável**. Regras:
  - **desligado por padrão** (sem a variável não faz nada);
  - **só cria se ainda não existir nenhum ADMIN**, então reiniciar com a variável ainda definida não recria nem troca a senha (só avisa para removê-la);
  - a senha precisa ter **12 caracteres a 72 bytes** e **não pode ter espaço ou quebra de linha nas pontas** (o erro clássico de gravar a variável com `echo`); o nome segue `[A-Za-z0-9._@-]{3,100}`;
  - se já existir um usuário **comum** com o nome pedido, a subida **falha** em vez de promovê-lo sem aviso;
  - a senha **nunca aparece no log** e só é gravada como hash `{bcrypt}`;
  - quem escolhe a senha é o responsável, e ela nunca passa pelo repositório. Na AWS ela deve vir do Secrets Manager, como a do banco, nunca escrita no `task-definition.json`.
  - Limite conhecido: o `ApplicationRunner` roda depois de o servidor web subir; se a configuração estiver errada, a aplicação cai cerca de 0,4 s depois do `Started`. É suficiente (o contêiner não fica de pé), mas não é "antes de abrir a porta".
- **Guardas de teste novos** (rodam em toda a esteira, para o endurecimento não se perder):
  - `ConfigHygieneTests` (6, sem Spring, **leem os arquivos reais do projeto**): o `application.yml` tem `secret: ${JWT_SECRET}` e `password: ${DB_PASSWORD}` **sem valor padrão**; nenhuma chave que termine em `password`, `secret`, `token` ou `key` tem valor escrito (só `${VARIÁVEL}` ou `${VARIÁVEL:}` vazio; as durações `access-token-expiration` e `refresh-token-expiration` não são confundidas com segredo); CORS e o bootstrap do admin são desligados por padrão; **na `task-definition.json` nenhuma variável com cara de segredo está em `environment`** (tem que estar em `secrets`, e com ARN do Secrets Manager); **só a `V5` pode inserir em `users`** (uma migration futura que semear outro login falha o teste), a `V6` tem o **mesmo hash** da V5 (se divergirem, a V6 não removeria nada) e todo usuário semeado tem hash `{bcrypt}`;
  - `SensitivePathsTests` (5, filtros ligados, banco de verdade): 23 caminhos sensíveis (actuator, `heapdump`, console do H2, `.env`, `.git/config`, `application.yml`, `WEB-INF`, `/error`...) dão 401 para anônimo e **403 até para o ADMIN**; métodos perigosos (`POST /actuator/shutdown`) também; cabeçalhos de segurança do Spring Security presentes (`nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-cache`, sem `X-Powered-By`, sem `WWW-Authenticate`); os erros (400, 401, 403, 404, 415, JSON quebrado, login) **nunca vazam pilha, nome de classe, pacote nem caminho de arquivo**;
  - `AdminBootstrapRunnerTests` (10, Mockito) e `AdminBootstrapIntegrationTests` (4, aplicação inteira com saída de log capturada): cria com hash e as duas permissões; o login funciona com a senha escolhida e falha com outra; a senha não está no log; rodar de novo não recria nem troca nada; todas as validações.
- **Os guardas pegam falha de verdade (sabotagens, depois restauradas):** runner: senha escrita no log → 2 testes quebraram; sem a checagem de "já existe ADMIN" → vários; senha curta aceita → 1; senha gravada em texto → 2. Higiene: `JWT_SECRET` com valor padrão → 2; segredo em `environment` da Task Definition → 1; hash da V6 diferente → 1; migration V7 semeando outro usuário → 1; actuator liberado → 3.
- **Provado no banco de verdade (MySQL local):**
  - **banco novo e vazio** (V1 a V6 do zero, o caminho que o RDS fará): termina com **0 usuários**, as permissões `ADMIN` e `USER` e os 408 alunos; o login `admin` com a senha que foi mostrada no chat dá **401**; `GET /alunos` sem token dá 401;
  - **regra da V6** (SQL direto): `admin` com senha trocada **fica**; `admin` com a senha semeada **sai**;
  - **primeira execução com `ADMIN_BOOTSTRAP_PASSWORD`**: cria o admin com `ADMIN` e `USER`; login com a senha escolhida 200 e com a senha "conhecida" 401; a senha não aparece no log; **reinício com a variável ainda definida**: avisa "já existe um administrador", mesmo hash, nada recriado, login continua 200; **reinício sem a variável**: o login continua funcionando;
  - **configuração errada** (a aplicação cai com mensagem clara e não cria nada): senha com quebra de linha no fim, senha curta, nome inválido, e usuário comum já existente com o nome pedido (não é promovido: 0 ligações);
  - banco local ao fim: `V1` a `V6`, **0 usuários**, 408 alunos. Quem quiser entrar localmente passa a usar `ADMIN_BOOTSTRAP_PASSWORD` com uma senha que escolher.
- **Placar:** 259 testes (eram 234), `mvn -B verify` verde com `JWT_SECRET`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS` e as variáveis do bootstrap fora do ambiente, também em ordem aleatória; `docker compose build` ok.
- **Incidente do caminho (lição para o playbook):** uma instância antiga da aplicação, do teste no navegador, ficou viva ligada ao **banco local real** (o `timeout` do bash encerra o *wrapper* mas não a JVM no Windows). Com o DevTools reiniciando a cada arquivo novo em `src/main/resources`, ela **aplicou no `mysql-dev` as migrations das minhas sabotagens**: a `V6` verdadeira (o que apagou o admin local, fora do meu controle) e a `V7` falsa (que inseriu um usuário `outro`). Conserto: apaguei o usuário `outro` e a linha da `V7` do histórico do Flyway (era só um `INSERT`), conferi `V1` a `V6` e 408 alunos, e encerrei os 4 processos meus que sobraram. **Regras:** (1) antes de mexer em `src/main/resources` (sabotagens), garantir que nenhuma instância esteja ligada ao banco real; (2) encerrar a aplicação com um script que mata a JVM (`parar-app.ps1`), não confiar em `timeout`; (3) mutações de migration só contra um banco descartável. Também errei um detector: considerei que a aplicação "subiu" ao ver `Started`, que o Spring escreve **antes** de rodar os `ApplicationRunner`; refiz esperando a falha aparecer.
- **ROTEIRO DA AWS (do console; nada disto foi feito) antes da primeira release com segurança.** Sem isso a tarefa **não sobe**:
  1. **Segredo do JWT:** Secrets Manager → *Store a new secret* → *Other type of secret* → *Plaintext* → um valor aleatório de pelo menos 32 caracteres (gerar no computador com `openssl rand -hex 32` e **não** reaproveitar o da máquina local) → nome sugerido `secrets_crud_alunos/jwt-secret`.
  2. **Senha do primeiro admin:** outro segredo, `secrets_crud_alunos/admin-bootstrap-password`, com uma senha que você escolher (12+ caracteres), guardada também num gerenciador de senhas. É só para a primeira execução.
  3. **Permissão de leitura:** IAM → Roles → `ecsTaskExecutionRole` → a política inline `LerSecretDB_CrudAlunos` → acrescentar à lista de `Resource` os dois ARNs novos (com o `-*` no fim de cada um, como no do banco).
  4. **Task Definition:** em `task-definition.json`, na lista `secrets`, acrescentar `JWT_SECRET` (`valueFrom` = ARN do segredo do JWT) e, **só para o primeiro deploy**, `ADMIN_BOOTSTRAP_PASSWORD` (`valueFrom` = ARN do segredo do admin). Os ARNs só existem depois de criar os segredos, por isso eu não editei o arquivo agora (um ARN inventado quebraria o deploy). `CORS_ALLOWED_ORIGINS` não é necessário (o padrão vazio é seguro).
  5. **Ligar o ambiente:** RDS `Available` e Service com *Desired tasks* = 1 (a esteira não mexe nisso). O Flyway aplica `V4`, `V5` e `V6` no RDS e a aplicação cria o admin.
  6. **Depois do primeiro start com sucesso:** tirar o `ADMIN_BOOTSTRAP_PASSWORD` do `task-definition.json` e fazer um novo deploy (a senha não deve ficar configurada). Opcional: apagar o segredo `admin-bootstrap-password`.
  7. **Conferir o "pronto quando":** `POST /auth/signin` com `admin` e a senha escolhida → 200; com a senha que foi mostrada no chat → 401; `GET /alunos` sem token → 401.
- **Limitações que continuam:** não há limite de tentativas de login nem bloqueio de conta (uma senha fraca pode ser adivinhada por força bruta); o `admin` criado tem `ADMIN` e `USER`; trocar a senha de um usuário ainda exige mexer no banco (não há endpoint para isso).

## 40. Release 1.3.0: a segurança vai para a produção (2026-11-03)

- **O que entrou:** PRs #30 (autorização), #31 (CORS) e #32 (endurecimento) na `develop`; PR #33 `release/1.3.0` → `main` (merge `2adbd91`); tag `v1.3.0`; back-merge no PR #34. Cada merge com o "pode" do usuário.
- **Antes da release (feito pelo usuário no console):** segredos `secrets_crud_alunos/jwt-secret` e `secrets_crud_alunos/admin-bootstrap-password`; os dois ARNs na política `LerSecretDB_CrudAlunos` da `ecsTaskExecutionRole`; `JWT_SECRET` e `ADMIN_BOOTSTRAP_PASSWORD` em `secrets` do `task-definition.json` (a mão da CLI não tem permissão de IAM nem de Secrets Manager).
- **Deploy:** esteira verde, Service na task definition `:8`, rollout `COMPLETED` com 1 tarefa. O RDS estava Available (confirmado pelo usuário).
- **Verificado na AWS sem a senha do admin:** `GET /alunos` sem token → 401; `POST /auth/signin` com a senha antiga que foi mostrada no chat → 401; `/actuator/env` → 401.
- **Ainda por fazer:** (1) o usuário testar `POST /auth/signin` com `admin` e a senha do segredo (deve dar 200); (2) **remover `ADMIN_BOOTSTRAP_PASSWORD` do `task-definition.json`** e fazer novo deploy (release de correção `1.3.1` ou similar); opcionalmente apagar o segredo `admin-bootstrap-password`.

## Próximos passos
> **Atualização 2026-11-03:** o usuário criou o segredo `secrets_crud_alunos/jwt-secret` e liberou a leitura na `ecsTaskExecutionRole`; `JWT_SECRET` já está em `secrets` do `task-definition.json` (branch `feature/endurecimento`, ainda não implantado). O segredo da senha do primeiro admin também foi criado e `ADMIN_BOOTSTRAP_PASSWORD` entrou em `secrets` (confirmar que a política de leitura tem o ARN). Depois do 1º deploy, REMOVER essa variável do arquivo e fazer novo deploy.

Atualizado no fim do dia 2026-11-02. A seção "COMO RETOMAR" do `memoria.md` tem a versão curta.

### 1. Mesclar a pilha de PRs da segurança (a ordem importa; cada merge só com o "pode" explícito do usuário)

A `develop` já tem a segurança até a **autenticação** (PRs #27 e #29, 200 testes). Faltam três branches empilhadas, cada uma sobre a anterior:

| Ordem | Branch | O que traz | Estado |
| --- | --- | --- | --- |
| 1 | `feature/autorizacao` (PR #30) | quem pode o quê, regra final `denyAll` | PR aberto, `build` verde |
| 2 | `feature/cors` | CORS por origens configuradas, vazio por padrão | branch no GitHub |
| 3 | `feature/endurecimento` | `V6`, primeiro admin por variável de ambiente, guardas de teste | branch no GitHub |

Cada uma já contém as anteriores (foram juntadas com `merge`, sem reescrever história). Depois de mesclar a #30, abrir o PR da próxima; as anotações (`PassoAPasso.md` e `memoria.md`) podem dar conflito entre elas: manter as duas versões.

### 2. NÃO fazer release da segurança antes disto (a tarefa na AWS não sobe sem o `JWT_SECRET`)

O roteiro completo está no passo 39. Resumo: segredo do JWT e segredo da senha do primeiro admin no Secrets Manager; permissão de leitura na `ecsTaskExecutionRole`; `JWT_SECRET` (e, só no primeiro deploy, `ADMIN_BOOTSTRAP_PASSWORD`) em `secrets` no `task-definition.json`, **só depois de existirem os ARNs**; depois do primeiro start, tirar o bootstrap. Tudo isso é do console da AWS. O perfil `crud-alunos` da CLI não tem permissão de Secrets Manager nem de IAM.

### 3. Continuar o playbook

O usuário cola o próximo passo, um de cada vez (a numeração do playbook é diferente da nossa). O que vem depois do CORS e do endurecimento é o "Passo 38 do playbook" (testes de segurança que faltam). O 403 de rotas de dados, o 401 de tokens ruins e o login já têm teste.

### 4. Pendências de antes

- **Ambiente da AWS LIGADO** (RDS e Service) desde a release 1.2.0; ao terminar, *Desired tasks* = 0 e RDS *Stop temporarily*. O RDS parado religa sozinho após 7 dias (previsto por volta de **2026-11-09**).
- Swagger e `v3/api-docs`: adiado até o Swagger existir (decisão do usuário).
- Seções 12 a 14 do tutorial da AWS (rotina, problemas comuns, limpeza): ainda não executadas.
- Sobras na AWS: `rest-with-claude-app-sg`, cluster `rest-with-claude-spring`, repositório `respositorio-crud-alunos` (sem o "i") e a política `LerSecretDB_CrudAlunos` que ficou numa role errada.
- Branches remotas já mescladas que podem ser apagadas **quando o usuário pedir**: `feature/autenticacao`. A `teste-ci-quebrado` nunca foi mesclada, de propósito.
- Limitações de segurança conhecidas: sem limite de tentativas de login nem bloqueio de conta; não há endpoint para trocar senha nem para dar `ADMIN` a outro usuário (só pelo banco).
