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

- `AlunoService` usa `CopyOnWriteArrayList` e já sobe com 8 alunos fixos (ids 1 a 8; os 5 primeiros são os da carga do Postman, o 6 é Felipe Rocha, o 7 é Gustavo Alves e o 8 é Helena Barros). Os próximos criados começam no id 9. `AlunoControllerTests` é `@SpringBootTest` + `@AutoConfigureMockMvc`.

- Boot 4 / Spring 7 usa Jackson 3 (`tools.jackson`): não seguir literalmente receitas do Jackson 2 (`AbstractJackson2HttpMessageConverter`, `com.fasterxml`). Conversores de XML e YAML já existem no Spring; `WebConfig` define JSON como padrão e faz o YAML aceitar também `application/x-yaml`.
- Erros da API (`ErrorResponse`) ficam sempre em JSON, mesmo com `Accept` de XML/YAML.
- Em ambiente com permissão automática, `npx` baixando pacote externo (ex.: newman) é bloqueado; usar scripts locais.

- Serialização padronizada com `@JsonPropertyOrder`/`@JsonFormat` nos DTOs (datas `yyyy-MM-dd`; `dataHora` do erro em UTC com milissegundos). Sem `@JsonSerialize` por enquanto. Lista no XML sai como `<content><item>`.

- `Duvida-CICD.md` (raiz) registra dúvidas, erros e diferenças do tutorial `AWS-Esteira CICD.md`, para virar um tutorial novo. O arquivo segue o formato do tutorial: seções com o mesmo número e título (ex.: `## 1. Criar o banco MySQL (RDS)`), passo a passo corrigido, tabelas e avisos; dúvidas sem resposta ficam na tabela "Dúvidas abertas". Atualizar a seção correspondente a cada dúvida, erro ou atualização da AWS (não criar entradas soltas por data). O usuário segue o tutorial por conta própria e pede ajuda quando travar. O arquivo deve ter **só o que já foi executado** (hoje: seção 1, RDS); itens de seções futuras ficam aqui no `memoria.md` até serem executados.
- Nomes reais do usuário na AWS (todos diferentes do tutorial): banco `database-crud-alunos`, repositório ECR `respositorio-crud-alunos` (sem o "i"; decisão manter/recriar pendente), usuário IAM `GITHUB_ACTIONS_CRUD_ALUNOS`, cluster `cluster-crud-alunos`, secret `secrets_crud_alunos/db-password`. Seções 1 a 5 feitas (política inline `LerSecretDB_CrudAlunos` na Task execution role; confirmar na seção 7 que a Task Definition usa essa mesma role); a política tem Resource `arn:aws:secretsmanager:us-east-1:<conta>:secret:secrets_crud_alunos/db-password-*` (manter o `-*`).
- Seção 7.1 feita: Task Definition `crud-alunos-task-definition:1`, contêiner `container-crudalunos`, porta 8080, variáveis `DB_USER`, `TZ`, `DB_PASSWORD` (deve ser ValueFrom) e `DB_URL`. O database dentro do RDS será `crud_alunos` (não o `rest_udemy` do tutorial), criado pela aplicação via `createDatabaseIfNotExist=true`. A aplicação precisa ler `DB_URL`, `DB_USER` e `DB_PASSWORD` no `application.yml` (ainda não implementado). JSON da revisão `:1` conferido e correto (secrets/ValueFrom, DB_URL, Fargate 1 vCPU/3 GB, role de execução `ecsTaskExecutionRole`, imagem sem tag = latest). Falta confirmar que a `ecsTaskExecutionRole` tem a política `LerSecretDB_CrudAlunos` e `logs:CreateLogGroup`. O console está sendo usado como usuário raiz (anotado em Dúvidas abertas). Próximos: 7.2 (`task-definition.json`), 8 (imagem no ECR; precisa de Dockerfile), 9 (Service), 10 a 15.
- `Dockerfile` e `.dockerignore` criados em `apicrud-alunos/` (2 fases, Java 25) e testados localmente (imagem local `crud-alunos-local`, 521 MB). Docker 29.8.1 e AWS CLI 2.38.4 estão instalados na máquina. Seção 8 feita: imagem `latest` enviada para `respositorio-crud-alunos` com o perfil `crud-alunos` da AWS CLI (existe também um perfil `default` de conteúdo desconhecido; não usar). Seção 9 feita: `service-crud-alunos` com 1 tarefa **Running** (em 2026-10-30 ~20:08, IP público <IP-publico>, que muda a cada tarefa nova). A tarefa só subiu depois de criar a política inline `LerSecretDB_CrudAlunos` na `ecsTaskExecutionRole` (a primeira tinha ido para outra role, ainda não identificada) e forçar novo deploy. Seção 10 feita: API testada no Postman pelo IP público (sem Swagger nem autenticação). Seção 11 feita (workflow `.github/workflows/continuous-deployment.yml` criado pelo usuário; **não alterar workflow sem pedir**). Próximo: a conexão da aplicação com o MySQL. **Custo:** tarefa ligada ~US$ 1,30/dia e RDS ligado; desligar com Desired tasks = 0 e parar o RDS ao terminar (seção 15).
- Notas para as seções futuras do tutorial (ainda não executadas): (a) seção 6 feita: grupo da aplicação `security-group-crud-alunos` (`sg-...`); o `default` do RDS (`sg-...`) aceita a 3306 de ele, do IP do usuário (`Vitor-casa`) e do `rest-with-claude-app-sg` (`sg-...`, sobra de tentativa anterior; decidir apagar); (b) seção 7: a Task Definition do tutorial não passa o endereço do banco; o projeto ainda não tem datasource, Dockerfile, `task-definition.json` nem workflow; propor `DB_URL` por variável de ambiente; (c) a pasta `.github/` já está versionada e sem regra de ignore (resolvido).

## Atenção: repositório público

- O repositório `vitrodr2015/api-alunos-com-claude` é **público** (projeto de estudo, decisão do usuário em 2026-11-01). **Nunca** escrever em arquivo versionado: senha, Access Key, Secret Access Key, token. Nos `.md`, usar marcadores no lugar do IP de casa (`<seu-IP>/32`), do IP da tarefa (`<IP-publico>`) e de IDs de security group (`sg-...`).
- Já estão no histórico do Git (não somem ao editar): ID da conta AWS, endpoint do RDS, ARNs, IDs de security group e o IP de casa. Decisão: não reescrever o histórico. O `task-definition.json` mantém ID da conta, ARNs e endpoint porque a esteira precisa deles.
- Cuidados: senha forte no RDS e *Public access* do RDS ligado só quando necessário.

## Banco local (Passo 17 do playbook)

- Contêiner `mysql-dev` (`mysql:8.4`), database `crud_alunos`, `utf8mb4`/`utf8mb4_unicode_ci`, volume `mysql-dev-data`, **porta 3307** no computador (o serviço Windows `MySQL84` ocupa a 3306). URL local: `jdbc:mysql://localhost:3307/crud_alunos`, usuário `root`. A senha é só de desenvolvimento e **não vai para arquivo versionado** (repositório público). Se o Docker Desktop estiver desligado, abrir antes de `docker start mysql-dev`.
- Passos 18 e 19 feitos: JPA, driver, H2 de teste e datasource lendo `DB_URL`/`DB_USER`/`DB_PASSWORD` (padrão local `localhost:3307/crud_alunos`, usuário `root`, **senha sem padrão**). O Spring Boot não falha com `${DB_PASSWORD}` ausente (manda o texto literal), então o `main` tem um listener que para a subida com mensagem clara. Para rodar local: `export DB_PASSWORD=...` (senha de dev, nunca em arquivo) e `mysql-dev` ligado.
- Passo 20 feito: perfil `local` com `application-local.yml` (ignorado) e modelo `apicrud-alunos/application-local.yml.example`. O `application.yml` de teste substitui o de `main`. A variável de usuário do Windows `DB_PASSWORD` (senha de dev do Docker) foi criada a pedido do usuário; ela só enxerga processos abertos depois.
- Passo 21 feito: Flyway (`spring-boot-starter-flyway` + `flyway-mysql`; no Boot 4 o `flyway-core` sozinho não autoconfigura), migration `V1__create_table_aluno.sql`, `ddl-auto: validate`, Flyway desligado nos testes (H2). Sem seed ainda: os 8 alunos fixos seguem em memória; ao migrar o service para o banco, criar `V2__...` (nunca editar a `V1`). No primeiro deploy, o Flyway cria a tabela no RDS.
- Passo 22 feito: `Aluno` é `@Entity` e existe `AlunoRepository`; o `AlunoService` ainda usa a lista em memória. `ddl-auto: validate` foi provado de verdade (coluna errada derrubou a subida). Próximo: trocar o service pelo repository e criar a `V2__...` com os 8 alunos fixos.
- **Cuidado ao fazer release:** com o datasource, a aplicação real exige o banco no arranque. Antes do merge na `main` (deploy), o RDS `database-crud-alunos` tem de estar `Available`; se estiver parado, a tarefa cai ao subir.
- Próximo: `@Entity` para o `Aluno`, `repository` e trocar a lista em memória do `AlunoService` pelo banco, por uma `feature/*`.

## Pendências

- Decidir o destino do `HELP.md`.
- Escolher MapStruct ou Dozer, se o usuário quiser trocar o mapper manual.
- Conectar a aplicação ao MySQL (JPA, driver, `application.yml` lendo `DB_URL`/`DB_USER`/`DB_PASSWORD`, `repository`).
- Seções 12 a 14 do tutorial da AWS (rotina, problemas comuns, limpeza) ainda não executadas.
- Sobras na AWS para decidir: `rest-with-claude-app-sg`, cluster `rest-with-claude-spring`, repositório `respositorio-crud-alunos` (sem o "i") e a política `LerSecretDB_CrudAlunos` que ficou na role errada.
- Erros que faltam: 401/403 em JSON (entram com a segurança).

## Estado

- **Ambiente AWS em 2026-11-01:** o usuário colocou o Service `service-crud-alunos` em **Desired tasks = 1** (revisão `crud-alunos-task-definition:2`, imagem com o hash do commit `8fdf837`), achou o IP novo na aba Tasks e testou no Postman, ok. Se o RDS foi religado nesse dia não foi confirmado (a API não usa o banco ainda). Ao terminar, voltar *Desired tasks* para 0 e parar o RDS. O campo *Desired tasks* fica na página do **Service**, não na da Task Definition (o usuário chegou a abrir a página errada).
- **Ambiente AWS (2026-10-30, fim do dia):** esteira do GitHub Actions rodou e fez o deploy (Task Definition `:2`, aluno 6 incluído). O Service `service-crud-alunos` foi colocado em **Desired tasks = 0** (sem tarefas rodando). O **RDS `database-crud-alunos` foi parado pelo usuário no console (Stopped temporarily, 2026-10-30; a AWS o religa sozinha por volta de 2026-11-06 se não for usado)**, e o banco de sobra `vitrodr-db-server` (us-east-1d) foi apagado pelo usuário. Os perfis `default` e `crud-alunos` da AWS CLI são o mesmo usuário `GITHUB_ACTIONS_CRUD_ALUNOS`, sem permissão de RDS (só dá para ver ECS, ECR e EC2). O cluster ECS `rest-with-claude-spring` (sobra do tutorial original, com 1 Service e Container Insights enhanced) continua existindo com 0 tarefas. Para religar (seção 15.1 do tutorial): 1) RDS → Actions → Start e esperar Available; 2) ECS → Service → Update → Desired tasks = 1; 3) pegar o IP novo na aba Tasks. A esteira não altera o Desired tasks. O RDS parado religa sozinho após 7 dias.

- Último commit: `d539331` – Adiciona aluno fixo 7 (Gustavo Alves). **Ainda sem push**: o push na `main` dispara a esteira (build, imagem no ECR, deploy no ECS) e troca o IP da tarefa; só fazer quando o usuário pedir.
- **Git Flow (desde 2026-11-01, pedido do usuário para estudar):** branches `main` (produção, com tags `vX.Y.Z`), `develop` (onde os PRs de feature se juntam), `feature/*` (saem da `develop`), `release/X.Y.Z` (saem da `develop`, fecham a versão) e `hotfix/*` (saem da `main`). Só o merge na `main` dispara o deploy (e troca o IP da tarefa). Merge de release na `main` e de volta na `develop`: **merge commit**, nunca squash. **Nunca fazer push direto em `main`, `develop` ou `release/*`**: branch, push, PR com `gh pr create`, esperar o `build` e mesclar com `gh pr merge --merge`. Antes de qualquer merge na `main`, avisar o usuário (deploy na AWS) e esperar o "pode". Registrado no `PassoAPasso.md` (passo 16). Ciclo da release `1.0.0` concluído em 2026-11-01 (PRs #3 a #6, tag `v1.0.0` em `7e10a2f`, deploy ok). Pendente: o usuário apagar no GitHub as branches já mescladas.
- **Proteção das branches:** rulesets `protect-main`, `protect-develop` e `protect-release` (`release/**`), todos ativos: PR obrigatório (0 aprovações), check `build` verde e atualizado, sem force push; `main` e `develop` também não podem ser apagadas. `gh` 2.103.3 instalado e logado como `vitrodr2015`. No PowerShell 5.1, passar JSON para `gh api` por arquivo (`--input arquivo.json`, sem BOM) e evitar `--jq` com aspas. O classificador de segurança já bloqueou um `git push origin --delete`: apagar branches remotas pelo botão do GitHub ou pedir ao usuário. Teste anterior do passo 15 (referência): Teste de bloqueio confirmado: o PR da `teste-ci-quebrado` ficou bloqueado e foi fechado sem mesclar. O repositório é público, então a regra vale.
- **Bloco `permissions: contents: read`** no workflow (Passo 15 do playbook): vai pelo PR da branch `ci-permissions`.
- `PassoAPasso.md` (passos 13 e 14), `Duvida-CICD.md` (seções 11 e 15) e este arquivo foram atualizados em 2026-11-01. Push que muda só `.md` não dispara a esteira.
- `.\mvnw test`: 52 testes passando.
