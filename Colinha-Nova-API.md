# Colinha: como criar uma nova API (ordem macro)

Lista curta de etapas, **na ordem em que eu faria uma próxima API**, com o que aprendemos no projeto de alunos. Para consultar rápido. O "como fazer" de cada etapa está no `Playbook.md` (fase indicada); o registro completo, com datas e saídas reais, no `PassoAPasso.md`.

**Como usar:** copie as etapas para o seu projeto como checklist, marque conforme avança e só passe para a próxima quando o "feito quando" estiver provado (teste passando ou resposta real, não "acho que funciona").

> ★ = coisa que fizemos **tarde** neste projeto e que vale fazer **mais cedo** na próxima.

---

## Antes de começar (5 perguntas)

1. Quem usa a API e quem pode fazer o quê? (define autenticação e permissões)
2. Haverá navegador chamando? (define CORS; se não, deixe a lista vazia)
3. Quais formatos? (JSON basta; XML/YAML e HAL só se alguém pedir)
4. Onde vai rodar e quanto pode custar? (define a infraestrutura e o que desligar)
5. Dados de exemplo podem ir para produção? (se não, nunca em migration de produção)

---

## As etapas

| # | Etapa | Feito quando | Detalhe |
| --- | --- | --- | --- |
| 0 | **Combinar as regras:** anotações vivas, repositório público = sem segredo, um passo por vez, Git Flow | as regras estão escritas num arquivo de memória | Playbook, Fase 0 |
| 1 | **Criar o projeto** (Initializr) com as versões casadas: JDK = `pom.xml` = Dockerfile = esteira | `./mvnw clean package` passa | Fase 1 |
| 2 ★ | **Repositório + Git Flow + proteção das branches + CI mínimo** (build e testes em todo PR) | um PR com teste quebrado fica bloqueado | Fase 7 |
| 3 | **Camadas e erros:** pacotes (`controller`, `service`, `repository`, `model`, `dto`, `mapper`, `exception`, `config`, `security`), um único `ErrorResponse` e um `GlobalExceptionHandler` | 400, 404, 409, 415 e 500 saem no mesmo formato JSON | Fase 1 |
| 4 | **Escrever o contrato antes do código:** rotas, campos, regras e erros em tabela | o contrato está num arquivo e alguém o leu | Fase 2 |
| 5 | **Primeiro CRUD:** controller, service, DTO (entrada e saída), mapper, validação; testes **unitário** (Mockito) e **web** (MockMvc) | todos os endpoints cobrem 200/201/204 e 400/404/409 | Fase 2 |
| 6 | **Banco local em Docker** (cuidado com a porta ocupada) e **datasource por variáveis**, senha **sem valor padrão**, com mensagem clara se faltar | sem a variável a aplicação para com texto claro; com ela, sobe | Fase 3 |
| 7 | **Migrations (Flyway) + entidade + repository** com `ddl-auto: validate` | a subida falha se a entidade e o banco divergirem (prove trocando uma coluna) | Fase 3 |
| 8 ★ | **Testes com banco real desde já:** Testcontainers (MySQL) + REST Assured, com uma classe base e um contêiner único; H2 só para os testes rápidos | o fluxo completo roda contra o MySQL real, com as migrations de verdade | Fase 9 |
| 9 | **Listagem:** paginação com limites, filtros, **ordenação só por campos permitidos** (lista fixa) e **datas estritas** | `size` gigante é reduzido; `sort` estranho dá 400; `2000-02-30` dá 400 | Fases 4 e 11 |
| 10 | **Formato de resposta:** JSON como padrão e data/ordem dos campos padronizadas; HAL e XML/YAML só se precisar | o texto exato do JSON está testado | Fase 2 |
| 11 | **Segurança:** hash de senha (BCrypt), login com **a mesma resposta para qualquer falha**, JWT (access curto e refresh), filtro, 401/403 em JSON, **autorização por rota com `denyAll` no fim**, CORS **vazio por padrão**, primeiro admin por variável de uma execução, **testes com filtros ligados** | rota nova nasce fechada; anônimo 401, sem permissão 403; admin do bootstrap entra | Fase 8 |
| 12 ★ | **Guardas na esteira:** teste que lê os arquivos reais e proíbe segredo com valor padrão, segredo em `environment`, usuário semeado e `*` no CORS | quebrar a regra de propósito faz um teste falhar | Fase 8 (8.10) |
| 13 ★ | **Documentação viva:** Swagger **desligado por padrão** (ligado por variável), interfaces `*Docs`, esquema Bearer; coleção do Postman com login automático | o botão Authorize aceita o token e o Execute devolve 200 | Fase 10 |
| 14 ★ | **Saúde e falha de infraestrutura:** `health` público (use o *liveness* para orquestrador), **banco fora do ar = 503 rápido** (timeout curto, `Retry-After`), logs sem segredo | derrubar o banco de verdade devolve 503 em poucos segundos e a API volta sozinha | Fases 10 e 11 |
| 15 | **Contêiner:** Dockerfile em duas fases, usuário que não é root, `docker-compose` só para dar nome à imagem | a imagem roda local e responde | Fase 5 |
| 16 | **Infraestrutura na nuvem**, nesta ordem: banco → registro de imagens → usuário da esteira → cluster → **segredos (e a política na role certa)** → firewall → definição da tarefa → primeira imagem → serviço → teste pelo IP | a API responde no endereço público | Fase 6 |
| 17 | **Esteira de deploy:** build, testes e imagem em todo PR; **deploy só em push na `main`** | o merge na `main` troca a tarefa e a API nova responde | Fase 7 |
| 18 | **QA de pré-produção:** testar a **imagem de produção** com um banco real descartável (autorização, tokens, injeção, limites, concorrência, queda do banco, logs, reinício); corrigir e **refazer** | zero respostas 5xx para entrada inválida; nenhum segredo no log | Fase 11 |
| 19 | **Release:** checklist (banco `Available`, testes verdes, merge na `main`, **verificar pelo comportamento**, tag, back-merge) | a rota nova responde em produção | Fase 12 |
| 20 | **Operar e anotar:** ligar e desligar o ambiente, custo, rollback, o que deu errado e como se resolveu | o próximo release sai sem surpresa | Fase 12 |

---

## Tamanhos de projeto (o que dá para pular)

| Se for... | Faça | Pode pular |
| --- | --- | --- |
| **Protótipo local** | 1, 3, 4, 5, 6, 7, 15 | nuvem, esteira, segurança forte, QA |
| **API interna** | tudo até a 15, mais 2, 8, 11 (pelo menos autenticação) e 17 | Swagger público, QA completo |
| **Produção com usuários reais** | **todas**, e na infraestrutura: **HTTPS**, usuário de banco **só com o que precisa** (nunca o `root`), mais de uma instância, alarmes e backups conferidos | nada |

---

## As 10 regras de ouro

1. **Prova antes de "feito":** teste passando e resposta real.
2. **Sabote para confirmar** que o teste pega a falha (quebre de propósito e veja falhar).
3. **Segredo nunca em arquivo versionado;** na nuvem, em `secrets`, nunca em `environment`; um teste garante.
4. **Negar por padrão** (`denyAll`): rota nova nasce fechada.
5. **O banco manda, não o token:** permissões e estado da conta são relidos a cada requisição.
6. **Mesma resposta (e tempo parecido) para qualquer falha de login.**
7. **Banco fora do ar é 503,** rápido, nunca 401 nem 500.
8. **Nunca editar migration já aplicada;** corrigir com uma nova.
9. **Confirmar o banco `Available` antes de mesclar na `main`;** depois do deploy, verificar a versão pelo comportamento (um rollback deixa o código antigo no ar).
10. **Anotar enquanto faz:** o que deu errado e como se resolveu é a parte mais valiosa.

---

## O que levar de um projeto para o outro

- **Arquivos-modelo:** `Dockerfile`, `docker-compose.yml`, `.github/workflows/continuous-deployment.yml`, `task-definition.json` (sem senha), `application.yml` (variáveis, sem segredo), `AbstractIntegrationTest` e o perfil `testcontainers`, `ConfigHygieneTests`, `SecurityConfig` (regras + `denyAll`), `GlobalExceptionHandler` + `ErrorResponse`.
- **Anotações:** copiar os três arquivos (`PassoAPasso.md`, `memoria.md`, `Duvida-CICD.md`) vazios, com os cabeçalhos, e manter vivos desde o dia 1.
- **Ideia ainda não feita:** guardar no repositório os scripts de QA (bateria HTTP e fase de banco/logs) para repetir antes de cada release.
