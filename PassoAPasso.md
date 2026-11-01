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
2. ~~Seção 8: enviar a imagem para o ECR~~ (feito).
3. ~~Seção 9: criar o Service~~ (feito; a tarefa só subiu depois de dar à `ecsTaskExecutionRole` a permissão de ler o secret).
4. ~~Seção 10: testar a API pelo IP público (Postman)~~ (feito; sem Swagger nem login, o teste é `GET /alunos`).
5. Conexão com o MySQL na aplicação: JPA, driver, `application.yml` lendo `DB_URL`/`DB_USER`/`DB_PASSWORD`, `repository`.
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
- Pendente: apagar no GitHub as branches já mescladas (`release/1.0.0`, `feature/ci-gitflow`, `feature/aluno-8`, `ci-permissions`, `teste-ci-quebrado`). O sistema bloqueou `git push origin --delete`, então é pelo botão **Delete branch**.

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

## Próximos passos

- Conectar a aplicação ao MySQL (passo 5 da lista "Falta fazer" do passo 11).
- Seções 12 a 14 do tutorial (rotina, problemas comuns, limpeza).
- Decidir sobre as sobras na AWS (`rest-with-claude-app-sg`, cluster `rest-with-claude-spring`, repositório `respositorio-crud-alunos`).
