# Memória do projeto

Arquivo para retomar o contexto se a conversa for perdida. Ler junto com `PassoAPasso.md`.

## Como trabalhamos

- **Objetivo final do usuário:** montar um playbook com tudo o que foi feito, passo a passo, desde o início deste CRUD de alunos. Por isso, **a cada passo concluído, registrar no `PassoAPasso.md`** (ordem cronológica, decisões e o porquê) e, para a parte AWS, também no `Duvida-CICD.md` (diferenças de tela). Não depender da memória da conversa: os arquivos são a fonte.

- O usuário segue um playbook com passos numerados e cola o trecho de cada passo na conversa.
- Respostas em português. O usuário é iniciante em Spring, então explicar o que cada coisa é.
- Commit e push só quando o usuário pedir.

## Projeto

- API CRUD de alunos. Repositório: `github.com/vitrodr2015/api-alunos-com-claude`, branch `main`.
- Projeto Maven em `apicrud-alunos/` (a raiz do repositório é a pasta acima).
- Pacote base: `br.com.vitor.apicrud_alunos`.
- Java 25, Spring Boot 4.1.2. No Boot 4 o starter web é `spring-boot-starter-webmvc`.
- Windows 11; usar `.\mvnw.cmd` (ou `./mvnw` no Git Bash).
- Configuração em `src/main/resources/application.yml`.

## Decisões

- Só vale o `.gitignore` da raiz. O que veio do Initializr em `apicrud-alunos/.gitignore` foi apagado a pedido do usuário.
- Foi mantido o Maven Wrapper.
- O `HELP.md` do Initializr está fora do commit; ainda não foi decidido se apaga ou ignora.
- Os 9 pacotes de camadas foram criados vazios, sem nenhum arquivo dentro (pedido do usuário, sem `package-info.java` nem `.gitkeep`).
- Não numerar com os números do playbook: a numeração do `PassoAPasso.md` é a nossa, na ordem em que fazemos.
- Tratamento de erros: `GlobalExceptionHandler` estende `ResponseEntityExceptionHandler` e devolve sempre `ErrorResponse` em JSON. Exceções em inglês (`ResourceNotFoundException`, `InvalidRequestException`, `ConflictException`), mensagens em português.
- Código Java indentado com tabs, como o gerado pelo Initializr.
- Em Boot 4, `@WebMvcTest` está em `org.springframework.boot.webmvc.test.autoconfigure`.

- Contrato do CRUD de alunos no `PassoAPasso.md` (passo 5): `/alunos`, 7 endpoints (POST, GET lista paginada, GET por id, PUT, PATCH `/disable`, PATCH `/enable`, DELETE). Sem `PATCH` parcial genérico e sem versão na URL.
- CRUD mockado: armazenamento em memória dentro do `AlunoService` até existir `repository`/banco.
- Mapper escrito à mão (`AlunoMapper`), sem MapStruct/Dozer; o playbook sugere uma das duas libs, o usuário ainda não escolheu.
- Mensagens de validação (`@NotBlank` etc.) ficam fixas em português nas anotações, para não depender do idioma do JVM.

- Todas as mensagens de erro da API (`erro` e `mensagem`) são em português, inclusive as do Spring, traduzidas no `GlobalExceptionHandler`.
- Todos os endpoints foram testados com curl na aplicação rodando (porta 8080) em 2026-10-29; tudo conforme o contrato.

- Coleção e ambiente do Postman em `postman/` (raiz do repositório), com 10 alunos de exemplo. Validados contra a aplicação rodando (36 requisições ok).

- `AlunoService` usa `CopyOnWriteArrayList` e já sobe com 5 alunos fixos (ids 1 a 5, os 5 primeiros da carga do Postman). `AlunoControllerTests` é `@SpringBootTest` + `@AutoConfigureMockMvc`.

- Boot 4 / Spring 7 usa Jackson 3 (`tools.jackson`): não seguir literalmente receitas do Jackson 2 (`AbstractJackson2HttpMessageConverter`, `com.fasterxml`). Conversores de XML e YAML já existem no Spring; `WebConfig` define JSON como padrão e faz o YAML aceitar também `application/x-yaml`.
- Erros da API (`ErrorResponse`) ficam sempre em JSON, mesmo com `Accept` de XML/YAML.
- Em ambiente com permissão automática, `npx` baixando pacote externo (ex.: newman) é bloqueado; usar scripts locais.

- Serialização padronizada com `@JsonPropertyOrder`/`@JsonFormat` nos DTOs (datas `yyyy-MM-dd`; `dataHora` do erro em UTC com milissegundos). Sem `@JsonSerialize` por enquanto. Lista no XML sai como `<content><item>`.

- `Duvida-CICD.md` (raiz) registra dúvidas, erros e diferenças do tutorial `AWS-Esteira CICD.md`, para virar um tutorial novo. O arquivo segue o formato do tutorial: seções com o mesmo número e título (ex.: `## 1. Criar o banco MySQL (RDS)`), passo a passo corrigido, tabelas e avisos; dúvidas sem resposta ficam na tabela "Dúvidas abertas". Atualizar a seção correspondente a cada dúvida, erro ou atualização da AWS (não criar entradas soltas por data). O usuário segue o tutorial por conta própria e pede ajuda quando travar. O arquivo deve ter **só o que já foi executado** (hoje: seção 1, RDS); itens de seções futuras ficam aqui no `memoria.md` até serem executados.
- Nomes reais do usuário na AWS (todos diferentes do tutorial): banco `database-crud-alunos`, repositório ECR `respositorio-crud-alunos` (sem o "i"; decisão manter/recriar pendente), usuário IAM `GITHUB_ACTIONS_CRUD_ALUNOS`, cluster `cluster-crud-alunos`, secret `secrets_crud_alunos/db-password`. Seções 1 a 5 feitas (política inline `LerSecretDB_CrudAlunos` na Task execution role; confirmar na seção 7 que a Task Definition usa essa mesma role); a política tem Resource `arn:aws:secretsmanager:us-east-1:<conta>:secret:secrets_crud_alunos/db-password-*` (manter o `-*`).
- Seção 7.1 feita: Task Definition `crud-alunos-task-definition:1`, contêiner `container-crudalunos`, porta 8080, variáveis `DB_USER`, `TZ`, `DB_PASSWORD` (deve ser ValueFrom) e `DB_URL`. O database dentro do RDS será `crud_alunos` (não o `rest_udemy` do tutorial), criado pela aplicação via `createDatabaseIfNotExist=true`. A aplicação precisa ler `DB_URL`, `DB_USER` e `DB_PASSWORD` no `application.yml` (ainda não implementado). JSON da revisão `:1` conferido e correto (secrets/ValueFrom, DB_URL, Fargate 1 vCPU/3 GB, role de execução `ecsTaskExecutionRole`, imagem sem tag = latest). Falta confirmar que a `ecsTaskExecutionRole` tem a política `LerSecretDB_CrudAlunos` e `logs:CreateLogGroup`. O console está sendo usado como usuário raiz (anotado em Dúvidas abertas). Próximos: 7.2 (`task-definition.json`), 8 (imagem no ECR; precisa de Dockerfile), 9 (Service), 10 a 15.
- `Dockerfile` e `.dockerignore` criados em `apicrud-alunos/` (2 fases, Java 25) e testados localmente (imagem local `crud-alunos-local`, 521 MB). Docker 29.8.1 e AWS CLI 2.38.4 estão instalados na máquina. Seção 8 feita: imagem `latest` enviada para `respositorio-crud-alunos` com o perfil `crud-alunos` da AWS CLI (existe também um perfil `default` de conteúdo desconhecido; não usar). Seção 9 feita: `service-crud-alunos` com 1 tarefa **Running** (em 2026-10-30 ~20:08, IP público 3.235.95.155, que muda a cada tarefa nova). A tarefa só subiu depois de criar a política inline `LerSecretDB_CrudAlunos` na `ecsTaskExecutionRole` (a primeira tinha ido para outra role, ainda não identificada) e forçar novo deploy. Seção 10 feita: API testada no Postman pelo IP público (sem Swagger nem autenticação). Próximo: seção 11 (esteira; o usuário está criando o `CICD.md`/workflow dele, **não criar workflow sem pedir**) e depois a conexão com o MySQL. **Custo:** tarefa ligada ~US$ 1,30/dia e RDS ligado; desligar com Desired tasks = 0 e parar o RDS ao terminar (seção 15).
- Notas para as seções futuras do tutorial (ainda não executadas): (a) seção 6 feita: grupo da aplicação `security-group-crud-alunos` (`sg-0995364797259db79`); o `default` do RDS (`sg-0511fa5538e05f367`) aceita a 3306 de ele, do IP do usuário (`Vitor-casa`) e do `rest-with-claude-app-sg` (`sg-07ab47b5b7340252c`, sobra de tentativa anterior; decidir apagar); (b) seção 7: a Task Definition do tutorial não passa o endereço do banco; o projeto ainda não tem datasource, Dockerfile, `task-definition.json` nem workflow; propor `DB_URL` por variável de ambiente; (c) antes da seção 11: a pasta `.github/` aparece ignorada pelo Git (`git check-ignore -v .github`).

## Pendências

- Decidir o destino do `HELP.md`.
- Commitar estrutura de camadas, tratamento de erros e os 7 endpoints (hoje só locais).
- Escolher MapStruct ou Dozer, se o usuário quiser trocar o mapper manual.
- Rodar a aplicação de verdade e chamar os endpoints.
- `.github/` aparece como ignorado no `git status --ignored`, sem regra no `.gitignore` do repositório. Investigar antes de criar a esteira de CI.
- Erros que faltam: 401/403 em JSON (entram com a segurança).

## Estado

- **Ambiente AWS (2026-10-30, fim do dia):** esteira do GitHub Actions rodou e fez o deploy (Task Definition `:2`, aluno 6 incluído). O Service `service-crud-alunos` foi colocado em **Desired tasks = 0** (sem tarefas rodando). O **RDS `database-crud-alunos` foi parado pelo usuário no console (Stopped temporarily, 2026-10-30; a AWS o religa sozinha por volta de 2026-11-06 se não for usado)**, e o banco de sobra `vitrodr-db-server` (us-east-1d) foi apagado pelo usuário. Os perfis `default` e `crud-alunos` da AWS CLI são o mesmo usuário `GITHUB_ACTIONS_CRUD_ALUNOS`, sem permissão de RDS (só dá para ver ECS, ECR e EC2). O cluster ECS `rest-with-claude-spring` (sobra do tutorial original, com 1 Service e Container Insights enhanced) continua existindo com 0 tarefas. Para religar (seção 15.1 do tutorial): 1) RDS → Actions → Start e esperar Available; 2) ECS → Service → Update → Desired tasks = 1; 3) pegar o IP novo na aba Tasks. A esteira não altera o Desired tasks. O RDS parado religa sozinho após 7 dias.

- Último commit: `69152c9` – Configurações iniciais (já enviado ao GitHub).
- Sem commit: estrutura de camadas, pacote `exception`, os 7 endpoints (model, dto, mapper, service, controller), `pom.xml` com `spring-boot-starter-validation`, testes, `PassoAPasso.md`, `memoria.md`.
- `.\mvnw test`: 52 testes passando.
