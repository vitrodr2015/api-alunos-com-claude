# Playbook2: construir uma API profissional

Guia genérico, para qualquer API. Não conta a história de um projeto: diz **o que fazer, em que ordem, por quê e como provar que ficou certo**. Onde há detalhe de tecnologia, ele vem no **perfil de referência** (Java, Spring Boot, MySQL, Docker, GitHub Actions e AWS), que pode ser trocado por outra pilha sem mudar a ordem nem os princípios.

Para a lista curta das etapas, veja a `Colinha-Nova-API.md`; para um prompt por etapa, a `Colinha-Prompts.md`; para a história do projeto que originou tudo isto, o `Playbook.md` e o `PassoAPasso.md`.

---

## Parte A. Fundamentos

### A1. Princípios

1. **Prova antes de "pronto".** Teste passando ou resposta real. Nunca "acho que funciona".
2. **Sabotar para confirmar.** Quebre o código de um jeito conhecido, veja o teste falhar, restaure. Teste que nunca falhou não prova nada.
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

Um módulo só está pronto quando:
1. a prova do "Pronto quando" foi mostrada;
2. a **sabotagem** foi feita e desfeita;
3. a suíte inteira passa **com os segredos fora do ambiente** (como no CI);
4. a documentação da API, se mudou, está atualizada;
5. nada sensível entrou nos arquivos;
6. o módulo foi anotado.

---

## Parte B. Decisões iniciais

Cada linha é uma decisão a tomar **antes** de escrever código. Registre a escolha e o motivo.

| Decisão | Opções comuns | Critério | Consequência |
| --- | --- | --- | --- |
| Quem usa a API? | pública, interna, só serviços | exposição e risco | define autenticação e HTTPS |
| Autenticação | token JWT, sessão, OAuth/OIDC | quem emite as credenciais | define o módulo 10 e os segredos |
| Permissões | papéis fixos, permissões finas | quantos perfis reais existem | define a matriz do módulo 11 |
| Navegador chama? | sim, não | existe front-end? | CORS vazio se não |
| Formatos | só JSON, mais XML/YAML/HAL | alguém realmente pede? | cada formato extra é superfície a testar |
| Banco | relacional, documento | forma dos dados e consistência | define migrations e testes |
| Onde roda | PaaS, contêineres, VM | equipe e custo | define os módulos 15 a 18 |
| Dados de exemplo em produção? | sim, nunca | o ambiente é real? | seed só no que roda em teste |
| Dados pessoais | quais, por quanto tempo | lei de proteção de dados | logs, retenção e exclusão |

---

## Parte C. Os módulos

| Bloco | Módulos |
| --- | --- |
| Fundação | 0 regras, 1 projeto e CI, 2 contrato, 3 configuração e segredos |
| Dados e núcleo | 4 banco e migrations, 5 camadas e erros, 6 CRUD vertical, 7 testes com banco real, 8 listagem, 9 formato |
| Segurança | 10 autenticação, 11 autorização e CORS, 12 admin inicial e endurecimento, 13 limites de abuso |
| Operação | 14 falha de infraestrutura, 15 contêiner, 16 QA da imagem |
| Entrega | 17 nuvem, 18 esteira, 19 release, 20 operação |

**O molde de cada módulo:** Objetivo; Decisões; Como (princípio, depois perfil de referência); Pronto quando; Sabotagem; Armadilhas clássicas (sintoma, causa, correção); Entregáveis.

---

## Bloco 1. Fundação

### Módulo 0. Regras de trabalho

**Objetivo.** Combinar como o trabalho será feito e registrado, antes de qualquer código.

**Decisões.** O repositório é público ou privado? Quem aprova o quê? O que um colaborador (ou um agente) pode fazer sozinho: commit, merge, deploy? Onde ficam as anotações?

**Como.**
- *Princípio:* regras escritas; anotações vivas (registro cronológico, estado e pendências, diferenças da nuvem); um passo por vez; ação irreversível só com confirmação, e a confirmação de uma ação **não vale** para a próxima.
- *Perfil de referência:* `memoria.md` (regras, decisões, estado, pendências), `PassoAPasso.md` (registro), `Duvida-CICD.md` (nuvem); o arquivo de contexto do agente (`CLAUDE.md` ou `AGENTS.md`) com as regras; Git Flow; `.gitignore` na raiz ignorando arquivos locais e de segredo.

**Pronto quando.** Uma sessão nova (ou outra pessoa) retoma o projeto só lendo os arquivos.

**Sabotagem.** Peça a alguém, ou a uma sessão nova, que continue **sem a conversa**. As perguntas que ela fizer são as lacunas a anotar.

**Armadilhas clássicas.**
- *Sintoma:* ninguém sabe em que ponto o projeto está. *Causa:* o estado vivia só na conversa, que se perdeu.
- *Sintoma:* valores sensíveis aparecem no repositório público. *Causa:* o histórico guarda tudo; apagar depois não resolve. *Correção:* marcadores (`<id-da-conta>`, `<IP>`) desde o começo.
- *Sintoma:* as anotações dizem o contrário da realidade. *Causa:* o "estado" não foi relido. *Correção:* a cada passo, reler "estado" e "pendências".

**Entregáveis.** Os arquivos de anotação, o arquivo de contexto do agente e o `.gitignore`.

---

### Módulo 1. Projeto, repositório e CI

**Objetivo.** Um projeto que compila, num repositório protegido, com CI que bloqueia código quebrado.

**Decisões.** A versão da linguagem (a mesma no projeto, na imagem e no CI); o fluxo de branches; repositório público ou privado (as regras de proteção podem não valer em repositório privado de conta gratuita).

**Como.**
- *Princípio:* o CI roda em **todo** PR; ninguém grava direto nas branches principais; a proteção exige o check do CI, **que só existe depois de o workflow rodar uma vez**.
- *Perfil de referência:* Spring Initializr (Maven, Java, dependências mínimas), Maven Wrapper, um só `.gitignore` na raiz; workflow que roda `mvn -B verify` em push na `main` e em PR para `main`, `develop` e `release/**`; depois do primeiro run, um *ruleset* (PR obrigatório, check `build` obrigatório e atualizado, sem force push, sem apagar `main` e `develop`); `gh` para abrir PRs e acompanhar o build.

**Pronto quando.** Um PR com teste quebrado de propósito fica com o build vermelho e o merge bloqueado.

**Sabotagem.** É o próprio teste: crie a branch com um teste quebrado, abra o PR, confirme o bloqueio e feche **sem mesclar**.

**Armadilhas clássicas.**
- *Sintoma:* o check não aparece ao criar a regra. *Causa:* o workflow nunca rodou. *Correção:* rode uma vez e só então crie a regra.
- *Sintoma:* tudo passa no PC e quebra só ao publicar. *Causa:* versões de Java diferentes entre projeto, imagem e CI.
- *Sintoma:* a regra de proteção não vale. *Causa:* repositório privado em conta gratuita.

**Entregáveis.** Projeto, workflow de CI, regras de proteção, anotação do fluxo de branches.

---

### Módulo 2. Contrato da API

**Objetivo.** Escrever o que a API promete antes de implementar.

**Decisões.** O recurso e os campos; as regras (unicidade, obrigatoriedade); exclusão lógica ou física; versão na URL; o formato de erro.

**Como.**
- *Princípio:* o contrato é a fonte dos testes e da documentação. Define a **ordem de validação** (corpo, id, existência, conflito, alteração) e o código de cada erro (400, 404, 409). A entidade nunca sai pela API: use objetos de entrada e de saída.
- *Perfil de referência:* uma tabela de campos (tipo, regra, quem preenche) e uma de endpoints (verbo, rota, entrada, sucesso, erros), num arquivo versionado.

**Pronto quando.** O contrato foi revisado e aprovado **antes** de qualquer código.

**Sabotagem.** Peça a um revisor que tente implementar só pelo contrato. As perguntas dele são as lacunas.

**Armadilhas clássicas.**
- *Sintoma:* o contrato muda depois de pronto. *Causa:* decisões de paginação e formato ficaram para depois. *Correção:* decida cedo e registre a mudança quando acontecer.
- *Sintoma:* "id inválido" e "não existe" saem iguais. *Correção:* 400 para o primeiro, 404 para o segundo.
- *Sintoma:* dúvida sobre repetir uma operação (ativar duas vezes). *Correção:* declare no contrato se é idempotente.

**Entregáveis.** O contrato versionado e a decisão sobre versão e exclusão.

---

### Módulo 3. Configuração e segredos

**Objetivo.** Nenhum valor sensível no código, e a aplicação se recusa a subir mal configurada.

**Decisões.** Onde cada valor vive (variável de ambiente, gerenciador de segredos); quais valores têm padrão seguro e quais não têm; como se configura o ambiente local.

**Como.**
- *Princípio:* configuração por ambiente, fora do código. Todo **segredo é obrigatório e sem valor padrão**; o que não é segredo pode ter um padrão seguro. Se faltar um segredo, a aplicação para na subida com uma mensagem que diz **qual** variável falta.
- *Perfil de referência:* `${VAR}` no `application.yml` para segredos e `${VAR:padrão}` para o resto; um listener de `ApplicationEnvironmentPreparedEvent` no `main` que lança erro claro se faltar; `application-local.yml` ignorado pelo Git, com um `.example` versionado; o `application.yml` de **teste** com valores **falsos** e óbvios.

**Pronto quando.** Sem a variável, a aplicação para com texto claro; com ela, sobe. Um teste de higiene que **lê o arquivo real** proíbe segredo com valor padrão.

**Sabotagem.** Coloque um valor padrão num segredo e mostre o teste de higiene falhar.

**Armadilhas clássicas.**
- *Sintoma:* "Access denied" sem citar a senha. *Causa:* o framework não falha com `${VAR}` ausente e envia o texto literal como senha. *Correção:* verificar na subida.
- *Sintoma:* os testes passam local e quebram no CI (ou o contrário). *Causa:* o arquivo de configuração de teste **substitui** o principal, e algo ficou só num deles.
- *Sintoma:* "variável não definida" no IDE, embora ela exista no sistema. *Causa:* o IDE abriu **antes** de a variável ser criada. *Correção:* reabrir.
- *Sintoma:* senha "errada" que parece certa. *Causa:* quebra de linha no fim do valor (típico de `echo` ao gravar o segredo). *Correção:* validar espaço e quebra de linha nas pontas.

**Entregáveis.** `application.yml`, `application-local.yml.example`, teste de higiene, `.gitignore` conferido com `git check-ignore`.

---

## Bloco 2. Dados e núcleo

### Módulo 4. Banco e migrations

**Objetivo.** Esquema versionado, reproduzível e conferido contra o código.

**Decisões.** Qual banco e qual *collation* (maiúsculas e acentos contam?); ferramenta de migrations; política de dados de exemplo; nome das tabelas (evite palavras reservadas, como `user`).

**Como.**
- *Princípio:* o esquema só muda por **migration versionada**; a aplicação **confere** o banco, nunca o altera; unicidade e restrições valem **no banco**, não só no código (duas requisições simultâneas passam juntas pela checagem do código).
- *Perfil de referência:* MySQL em contêiner com volume e porta livre; Flyway (no Spring Boot 4 usa-se o starter próprio mais o módulo do MySQL); `V1__...sql` com `UNIQUE` onde o contrato manda; entidade com **nomes de coluna explícitos**; `ddl-auto: validate`; para os testes rápidos, banco em memória com o Flyway desligado e dados sem `id` explícito.

**Pronto quando.** A aplicação sobe no banco real e aplica a V1; ao trocar o nome de uma coluna na entidade, a subida **falha** com "missing column".

**Sabotagem.** (1) Renomeie a coluna na entidade e veja a subida falhar. (2) Edite uma migration já aplicada e veja a ferramenta recusar (checksum diferente).

**Armadilhas clássicas.**
- *Sintoma:* o contêiner não sobe por "porta em uso". *Causa:* um serviço do sistema ocupa a porta padrão. *Correção:* mapear outra porta.
- *Sintoma:* o e-mail "A@x" e "a@x" são o mesmo no banco, mas não nos testes. *Causa:* a *collation* do banco real ignora maiúsculas e o banco de teste não; o mesmo vale para **acentos** (um login com nome acentuado pode achar outro usuário).
- *Sintoma:* os testes rápidos passam e a migration está errada. *Causa:* eles não rodam as migrations. *Correção:* o módulo 7.
- *Sintoma:* dados de exemplo aparecem em produção. *Causa:* seed numa migration comum. *Correção:* seed só no que roda em teste; se já foi, só outra migration desfaz.

**Entregáveis.** Migrations, entidade, repository, `docker run` documentado, anotação da decisão sobre *collation*.

---

### Módulo 5. Camadas e erros padronizados

**Objetivo.** Estrutura previsível e um único formato de erro.

**Decisões.** O idioma das mensagens; os campos do erro; quais exceções de negócio existem.

**Como.**
- *Princípio:* cada camada com uma responsabilidade (o controller só fala HTTP; o service guarda as regras e as transações; o repository acessa dados). Um **único formato de erro**, sempre em JSON, inclusive para os erros do próprio framework. O erro 500 é genérico; o detalhe vai só para o log.
- *Perfil de referência:* pacotes `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `exception`, `config`, `security`; `ErrorResponse` (data e hora em UTC, status, erro, mensagem); exceções de negócio; um `@RestControllerAdvice` que estende `ResponseEntityExceptionHandler`; teste com um **controller falso** dentro do próprio teste.

**Pronto quando.** 400, 404, 405, 406, 409, 415 e 500 saem no mesmo formato.

**Sabotagem.** Remova o tratador do 409 (ou do 500) e mostre o teste falhar.

**Armadilhas clássicas.**
- *Sintoma:* o erro do framework sai em outro formato. *Causa:* o tratador não cobre as exceções do próprio framework. *Correção:* estender a classe-base do framework.
- *Sintoma:* o 500 mostra pilha, classe ou SQL. *Correção:* mensagem genérica e detalhe só no log.
- *Sintoma:* os erros 401 e 403 saem sem formato. *Causa:* nascem nos filtros de segurança, antes do tratador global. *Correção:* o módulo 10.

**Entregáveis.** O formato de erro, as exceções, o tratador global e os testes.

---

### Módulo 6. CRUD em fatia vertical

**Objetivo.** Os endpoints do contrato, ponta a ponta, já com o banco.

**Decisões.** As validações de cada campo; o que o cliente **não** pode definir; as fronteiras das transações.

**Como.**
- *Princípio:* faça a fatia completa de uma vez (banco, service, controller e teste), e não camada por camada; a validação de entrada é **estrita** (o que é inválido é recusado, não corrigido); a operação destrutiva é **atômica**; a documentação da API nasce com o primeiro endpoint, desligada por padrão.
- *Perfil de referência:* `@Transactional` no service (`readOnly` nas leituras); DTOs com validação; mapper; apagar com `delete ... where id` que devolve quantas linhas apagou (0 vira 404); data lida em modo estrito (`@JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)`); testes unitários (Mockito) e web (MockMvc); OpenAPI em interfaces `*Docs`.

**Pronto quando.** Cada endpoint prova o caso feliz e os erros 400, 404 e 409; uma data inexistente (`2000-02-30`) dá 400; dois `DELETE` simultâneos do mesmo recurso dão 1 sucesso e o resto 404.

**Sabotagem.** Deixe a leitura da data leniente; troque o apagar atômico por ler e depois apagar. Os testes devem falhar.

**Armadilhas clássicas.**
- *Sintoma:* o service é reescrito quando o banco chega. *Causa:* o CRUD foi feito primeiro em memória.
- *Sintoma:* `2000-02-30` vira `2000-02-29` sem erro. *Causa:* o parser corrige datas. *Correção:* modo estrito (e, nele, o ano deve ser `uuuu`; `yyyy` não funciona).
- *Sintoma:* 500 quando dois `DELETE` chegam juntos. *Causa:* ler e depois apagar. *Correção:* uma instrução só.
- *Sintoma:* o cliente consegue definir `id` ou a situação do registro. *Causa:* o DTO aceita campos demais (*mass assignment*). *Correção:* ignorar o que não é do cliente.

**Entregáveis.** Controller, service, DTOs, mapper, testes, documentação da API.

---

### Módulo 7. Testes com banco real

**Objetivo.** Provar o comportamento contra o banco de verdade, com as migrations de verdade.

**Decisões.** Manter ou não o banco em memória para os testes rápidos; se o CI tem Docker.

**Como.**
- *Princípio:* pirâmide de testes (unitário, web, segurança, integração); o nível de integração usa o **mesmo banco da produção**; um contêiner por execução, reutilizado por todas as classes.
- *Perfil de referência:* Testcontainers (MySQL) e REST Assured; classe base com contêiner único (`@ServiceConnection`) e um perfil `testcontainers` (migrations ligadas, `validate`, sem script de dados); o primeiro admin nasce pelo mesmo caminho da produção; teste de concorrência (por exemplo, 20 chamadas simultâneas iguais).

**Pronto quando.** `mvn verify` passa com as migrations reais; o fluxo completo e os erros 400, 404, 409, 401 e 403 estão provados contra o banco real.

**Sabotagem.** Desligue as migrations no perfil e veja o contexto não subir.

**Armadilhas clássicas.**
- *Sintoma:* tudo verde e o erro aparece em produção. *Causa:* o banco em memória trata maiúsculas e acentos de outro jeito e não roda as migrations.
- *Sintoma:* o CI não roda os testes de integração. *Causa:* o runner não tem Docker.
- *Sintoma:* a versão do Testcontainers não casa com o framework. *Correção:* usar a versão da BOM e conferir o nome dos artefatos.

**Entregáveis.** Classe base, perfil, testes de fluxo e de concorrência.

---

### Módulo 8. Listagem

**Objetivo.** Paginação, filtros e ordenação seguras.

**Decisões.** Tamanho padrão e máximo; filtros; campos ordenáveis.

**Como.**
- *Princípio:* **nunca** confie no que o cliente manda para ordenar ou filtrar: limites configurados, ordenação só por uma **lista fixa** de campos e desempate por id para a paginação ser estável.
- *Perfil de referência:* `Pageable` com os limites no `application.yml` (repita no de teste); uma lista fixa de campos ordenáveis (qualquer outro valor, inclusive com `;`, dá 400); consultas derivadas, que escapam os curingas.

**Pronto quando.** Um tamanho gigante é reduzido; `sort` estranho dá 400 (não 500); percorrer todas as páginas não repete nem perde itens.

**Sabotagem.** Remova a lista de campos e mostre o teste falhar.

**Armadilhas clássicas.**
- *Sintoma:* `size` enorme derruba a API. *Causa:* sem máximo configurado.
- *Sintoma:* `sort=nome;drop table x` dá 500. *Causa:* o valor chega ao banco (que o recusa) e vira erro interno. *Correção:* validar contra a lista fixa.
- *Sintoma:* o filtro `%` devolve tudo. *Causa:* consulta montada à mão. *Correção:* consultas derivadas ou parametrizadas.
- *Sintoma:* a página dois repete itens da um. *Causa:* sem desempate por id.

**Entregáveis.** Listagem, lista de campos, testes de paginação.

---

### Módulo 9. Formato da resposta

**Objetivo.** Respostas previsíveis para quem consome.

**Decisões.** Só JSON ou mais formatos; se haverá links (HAL).

**Como.**
- *Princípio:* o **texto exato** da resposta é contrato; cada formato extra é superfície a testar e manter; os erros são sempre JSON.
- *Perfil de referência:* ordem dos campos e formato de datas por anotação; confirmar a biblioteca JSON da versão do framework (pacotes mudam entre versões); HAL e XML/YAML só se alguém pedir.

**Pronto quando.** Um teste compara o texto exato de uma resposta e de um erro.

**Sabotagem.** Mude a ordem de um campo e veja o teste falhar.

**Armadilhas clássicas.**
- *Sintoma:* erro de compilação ou de execução por pacote que não existe. *Causa:* receita de uma versão antiga da biblioteca de JSON.
- *Sintoma:* 500 em toda listagem depois de adicionar links. *Causa:* o gerador de links cria um proxy do tipo de retorno e um `record` é final. *Correção:* montar os links de outro jeito.
- *Sintoma:* `Accept` de um formato não suportado devolve erro estranho. *Correção:* 406, em JSON.

**Entregáveis.** Anotações de formato e o teste de texto exato.

---

## Bloco 3. Segurança

### Módulo 10. Autenticação

**Objetivo.** Saber quem chama, sem abrir brecha.

**Decisões.** O mecanismo (token, sessão, OIDC); as expirações; a política de senha. **Faça um plano e aprove antes de codificar:** esta etapa deixa a API toda protegida e quebra os testes de contrato por 401 até serem ajustados.

**Como.**
- *Princípio:* senha só como **hash** de algoritmo adaptativo, com o identificador do algoritmo no próprio hash (permite trocar depois); **a mesma resposta e o mesmo tempo para qualquer falha de login**; token de acesso curto e *refresh* mais longo, com o **tipo** do token conferido; o usuário é **recarregado do banco** a cada validação; segredo sem padrão e com tamanho mínimo; 401 e 403 no mesmo formato de erro.
- *Perfil de referência:* usuários, permissões e ligação em tabelas (`users`, no plural); BCrypt por um encoder delegante com **só o BCrypt** registrado; JWT HS256 (acesso 15 min, *refresh* 24 h, claims de emissor, sujeito, papéis, tipo, emissão e expiração); filtro **que não é bean** (criado com `new` na cadeia); sem sessão, CSRF desligado (não há cookie), sem formulário nem Basic; `signin` e `refresh` públicos (o *refresh* só no corpo, nunca na URL).

**Pronto quando.** O login entra; token vencido, adulterado, de outro segredo, com `alg=none`, com papéis alterados e do tipo errado dão 401; desativar o usuário no banco derruba o token na requisição seguinte.

**Sabotagem.** Tire a conferência do tipo do token; troque o tempo de resposta do login por casos diferentes. Os testes devem falhar.

**Armadilhas clássicas.**
- *Sintoma:* o *refresh* aparece em logs e histórico. *Causa:* foi enviado na URL.
- *Sintoma:* erro estranho com senhas longas. *Causa:* o BCrypt só aceita 72 bytes. *Correção:* validar no cadastro e responder 400.
- *Sintoma:* o filtro roda duas vezes. *Causa:* foi registrado como bean.
- *Sintoma:* dá para descobrir quais usuários existem. *Causa:* respostas ou tempos diferentes para "usuário inexistente" e "senha errada".
- *Sintoma:* o `refresh` vale como acesso. *Causa:* o tipo do token não é conferido.
- *Sintoma:* dezenas de testes de contrato falham com 401. *Correção:* rodá-los sem os filtros (com um aviso no arquivo de que não testam segurança) e testar a segurança à parte, com os filtros ligados.

**Entregáveis.** Entidades, hash, provedor de token, filtro, configuração, endpoints de autenticação, testes.

---

### Módulo 11. Autorização e CORS

**Objetivo.** Definir **quem pode o quê** e **quais origens** podem chamar a API pelo navegador.

**Decisões.** O modelo de permissões; quais operações são destrutivas e irreversíveis; quais origens (se houver).

**Como.**
- *Princípio:* autenticação diz **quem** é; autorização diz **o que pode**. Regras num único lugar legível. A regra final é **negar**; a fonte da permissão é o **banco** (relida a cada requisição). A autorização vem **antes** da checagem de existência (quem não pode recebe 403 até para um id que não existe, e não descobre o que existe). CORS não protege o servidor: só instrui o navegador.
- *Perfil de referência:* regras por rota no `SecurityConfig`, permissões em constantes, `anyRequest().denyAll()` no fim, `HEAD` com regra própria (o framework compara o método exato); CORS com lista de origens vinda de variável, **vazia por padrão**, sem credenciais, recusando `*` e formatos inválidos na subida.

**Pronto quando.** Uma **matriz** de testes perfil x rota (anônimo, sem permissão, outra permissão, perfil comum, administrador) passa; uma rota nova sem regra dá 403 a quem está logado e 401 ao anônimo; permissão revogada no banco vale na requisição seguinte.

**Sabotagem.** (1) Deixe o perfil comum apagar. (2) Troque a regra final para "qualquer autenticado". (3) Esqueça a regra do `HEAD`. (4) Remova ao mesmo tempo o `.cors()` e a liberação do `OPTIONS`. Cada caso deve quebrar algum teste.

**Armadilhas clássicas.**
- *Sintoma:* rota nova acessível a qualquer logado. *Causa:* regra final "autenticado" em vez de "negar".
- *Sintoma:* `HEAD` devolve 403 enquanto `GET` funciona. *Causa:* o framework trata `HEAD` como `GET`, mas a segurança compara o método exato.
- *Sintoma:* o navegador mostra "erro de CORS" quando o servidor devolveu 401. *Causa:* sem a configuração de CORS ligada na cadeia de segurança, os erros de segurança saem **sem** cabeçalhos de CORS e o navegador esconde a mensagem.
- *Sintoma:* o pré-voo de CORS vira 401. *Causa:* faltaram **os dois**: CORS ligado e `OPTIONS` liberado (um cobre o outro; a falta de um só passa despercebida).
- *Sintoma:* o administrador revogado continua mandando. *Causa:* a permissão foi lida do token em vez do banco.

**Entregáveis.** Regras de segurança, matriz de testes, configuração de CORS, anotação da tabela "quem pode o quê".

---

### Módulo 12. Admin inicial e endurecimento

**Objetivo.** Nenhum login conhecido e nenhum caminho sensível aberto.

**Decisões.** Como nasce o primeiro administrador; quais caminhos sensíveis conferir.

**Como.**
- *Princípio:* ninguém é semeado com senha conhecida; o primeiro administrador é criado por uma **execução controlada**; as proteções viram **testes que leem os arquivos reais**.
- *Perfil de referência:* um `ApplicationRunner` ativado por variável usada **uma vez** (desligado por padrão; só cria se não existir ADMIN; senha de 12+ caracteres, sem espaço ou quebra de linha nas pontas; **nunca** registra a senha; falha, em vez de promover, se já existir usuário comum com o nome); uma migration que remove o usuário semeado **só se** a senha ainda for a semeada; guardas na esteira: sem segredo com valor padrão, sem segredo em `environment` da definição da tarefa, só uma migration pode semear usuário, `*` proibido no CORS; caminhos sensíveis (actuator, console de banco, `.env`, `.git`, arquivos de configuração) dão 401 ao anônimo e 403 até ao ADMIN.

**Pronto quando.** Um banco novo termina sem usuário conhecido; subir com a variável cria o administrador; subir de novo com a variável **não** recria nem troca a senha; sem a variável o login continua funcionando.

**Sabotagem.** Escreva a senha no log; coloque um segredo em `environment`; mostre os testes falhando.

**Armadilhas clássicas.**
- *Sintoma:* existe um login conhecido em produção. *Causa:* o usuário semeado numa migration teve a senha mostrada ou guardada em arquivo.
- *Sintoma:* a aplicação cai poucos instantes depois de "Started". *Causa:* o `ApplicationRunner` roda depois de o servidor abrir; configuração errada o derruba logo em seguida (suficiente para não ficar de pé, mas não é "antes de abrir a porta").
- *Sintoma:* a senha do administrador "não confere". *Causa:* quebra de linha no valor da variável.

**Entregáveis.** O runner, a migration de remoção, os guardas, os testes de caminhos sensíveis.

---

### Módulo 13. Limites de abuso e ciclo de vida do token

**Objetivo.** Reduzir força bruta e dar controle sobre as sessões. *(Neste módulo o projeto de referência ficou devendo: foi anotado como limitação conhecida.)*

**Decisões.** Os limites e o desbloqueio; revogação por rotação ou por lista; troca de senha. **Faça um plano e aprove antes de codificar:** há decisões de produto.

**Como.**
- *Princípio:* o limite **não pode revelar** se o usuário existe; o administrador legítimo não pode ficar bloqueado para sempre; um token roubado precisa poder ser invalidado antes de vencer.
- *Perfil de referência:* limite de tentativas de login por usuário e por origem (429 com `Retry-After`); *refresh* com rotação ou lista de revogação; logout; troca de senha que invalida os tokens antigos.

**Pronto quando.** Tentativas em excesso recebem 429; um *refresh* revogado dá 401; trocar a senha invalida os tokens antigos.

**Sabotagem.** Remova a revogação e mostre o teste do *refresh* revogado falhar.

**Armadilhas clássicas.**
- *Sintoma:* o limite funciona em teste e não em produção. *Causa:* contador em memória com mais de uma instância. *Correção:* armazenamento compartilhado.
- *Sintoma:* um atacante bloqueia o usuário legítimo de propósito. *Causa:* bloqueio apenas por usuário. *Correção:* combinar com a origem e ter desbloqueio.
- *Sintoma:* sem logout, um token roubado vale até vencer. *Correção:* revogação.

**Entregáveis.** Limite, revogação, troca de senha, testes e documentação da API.

---

## Bloco 4. Operação

### Módulo 14. Falha de infraestrutura

**Objetivo.** Quando uma dependência cai, a API responde **rápido** e com o **código certo**, e volta sozinha.

**Decisões.** O tempo máximo de espera por uma conexão; o que o orquestrador consulta para saber se a aplicação está viva; quais exceções contam como "dependência fora do ar".

**Como.**
- *Princípio:* "dependência fora do ar" é **503** (com `Retry-After`), nunca 401 nem 500; o verificador de **vivacidade** não depende do banco (senão o orquestrador reinicia a aplicação em laço enquanto o banco estiver fora); só o verificador de **prontidão** olha dependências. Logs sem segredo e sem dado pessoal.
- *Perfil de referência:* Actuator expondo **só** o `health` (público, somente leitura, sem detalhes), com *liveness* e *readiness*; `hikari.connection-timeout` de poucos segundos; um classificador que reconhece pela **cadeia de causas** (sem conexão, sem transação, tempo esgotado) e é usado no tratador global de erros, **no filtro de autenticação** e no login; erros que não são do banco continuam 500.

**Pronto quando.** Derrubar o banco de verdade (contêiner descartável) devolve 503 em poucos segundos, o *liveness* segue UP, e a API volta sozinha quando o banco volta.

**Sabotagem.** Volte o timeout ao padrão e mostre o teste falhar; remova o tratamento do filtro de autenticação e veja o 401 enganoso reaparecer.

**Armadilhas clássicas.**
- *Sintoma:* 401 (parece token vencido) com o banco fora, depois de 30 s. *Causa:* o filtro recarrega o usuário do banco e trata qualquer falha como "token inválido"; o timeout padrão do pool é longo. *Correção:* classificar e responder 503, com timeout curto.
- *Sintoma:* o orquestrador derruba a aplicação quando o banco cai. *Causa:* o verificador consultado inclui o banco. *Correção:* apontar para o *liveness*.
- *Sintoma:* `healthCheck` dentro do contêiner falha. *Causa:* a imagem não tem `curl` nem `wget`. *Correção:* usar o verificador do balanceador, ou instalar a ferramenta.
- *Sintoma:* o `health` devolve detalhes (URL do banco). *Correção:* detalhes desligados e um teste que confere o corpo.

**Entregáveis.** O classificador de falhas, a configuração do `health`, o teste de queda do banco, a anotação do timeout escolhido.

---

### Módulo 15. Contêiner

**Objetivo.** Uma imagem enxuta e segura.

**Decisões.** A imagem base; o usuário do processo; como a saúde é verificada.

**Como.**
- *Princípio:* duas fases (build com o compilador; execução só com o *runtime*); o processo **não roda como root**; os testes rodam **antes** do build da imagem, no CI, e não dentro dele.
- *Perfil de referência:* Dockerfile com camada de dependências em cache, remoção de CRLF do wrapper do Maven, usuário comum, porta exposta; `.dockerignore`; `docker-compose.yml` só para dar nome à imagem; o CI passa a construir a imagem em todo PR.

**Pronto quando.** A imagem roda local, responde e o processo não é root (`docker exec ... id`).

**Sabotagem.** Remova a linha do usuário e confira que o `id` mostra root; restaure.

**Armadilhas clássicas.**
- *Sintoma:* o script do wrapper não roda no Linux. *Causa:* quebra de linha do Windows (CRLF). *Correção:* removê-la no build.
- *Sintoma:* diferença de comportamento entre a máquina e a imagem. *Causa:* versões de Java diferentes.
- *Sintoma:* o registro de imagens lista 3 itens por envio. *Causa:* o Docker mais novo acrescenta o índice e o atestado de proveniência. *Correção:* é normal.

**Entregáveis.** Dockerfile, `.dockerignore`, compose, ajuste do CI.

---

### Módulo 16. QA da imagem de produção

**Objetivo.** Atacar o artefato que vai para a produção **antes** de ele ir.

**Decisões.** O que cobrir; como isolar o ambiente.

**Como.**
- *Princípio:* teste unitário prova que o código faz o que você **pensou**, não o que um atacante ou a concorrência fazem. Teste o **artefato real**, com um banco real **descartável**, numa rede isolada, com as mesmas variáveis da produção, sem tocar na nuvem nem no banco de desenvolvimento.
- *Cobertura:* matriz de autorização por perfil; ataques ao token; cadastro (limites e campos extras); injeção; limites de todos os campos; ids estranhos; paginação, ordenação e filtros; negociação de conteúdo; corpo gigante; CORS; erros sem vazar detalhes; concorrência; persistência e reinício; troca do segredo do token; segredos ausentes; banco derrubado e recuperação; desligamento limpo; segredos e tokens fora dos logs.
- *Método:* uma bateria de requisições HTTP (inclusive em nível de *socket*, para o que o cliente HTTP comum não envia: cabeçalhos duplicados, métodos raros, URLs malformadas) e uma fase de banco, logs, reinício e queda do banco. Classifique cada falha como **defeito real** ou **expectativa errada**.

**Pronto quando.** Nenhuma resposta 5xx para entrada inválida e nenhum segredo nos logs. Os defeitos achados foram corrigidos (cada um com teste e sabotagem) e **o QA foi refeito** na imagem corrigida.

**Sabotagem.** O próprio QA é a sabotagem do conjunto: se ele não achar nada, desconfie da bateria.

**Armadilhas clássicas.**
- *Sintoma:* a bateria "passa" tudo. *Causa:* as expectativas foram escritas só para o que já funciona. *Correção:* inclua casos hostis.
- *Sintoma:* o QA mexeu no banco de desenvolvimento. *Causa:* o ambiente de teste não era isolado.
- *Sintoma:* sobram contêineres, redes e arquivos com senha. *Correção:* limpeza obrigatória no fim.
- *Sintoma:* um "defeito" que é erro do script de teste (por exemplo, o fuso horário da data de ontem). *Correção:* investigue antes de corrigir o código.

**Entregáveis.** O relatório de defeitos (severidade, como reproduzir, causa), as correções e o QA refeito.

---

## Bloco 5. Entrega

### Módulo 17. Nuvem

**Objetivo.** Infraestrutura mínima, segura e com custo controlado.

**Decisões.** O serviço de contêineres; o banco gerenciado; onde ficam os segredos; domínio e HTTPS; quem executa (o console é de uma pessoa, não do agente).

**Como.**
- *Princípio:* **a ordem importa e privilégio mínimo**: segredos num gerenciador (nunca em variável comum), lidos pela **role de execução** da tarefa; o banco só aceita a aplicação (e o seu IP, nunca "qualquer origem"); a aplicação usa um usuário de banco com acesso **só ao próprio schema**; o tráfego passa por HTTPS; o custo é conhecido antes de criar.
- *Perfil de referência (AWS), nesta ordem:* banco (uma zona, instância pequena, conferir a estimativa de custo); repositório de imagens; usuário da esteira com poucas permissões; cluster; **todos os segredos** (senha do banco, segredo do token, senha do administrador inicial) com a política de leitura na role de execução (o `Resource` termina em `-*`); firewall; definição da tarefa (segredos em `secrets` com `valueFrom` = ARN real; **nunca** em `environment`); primeira imagem enviada **antes** de criar o serviço; serviço (sem sub-redes de zona local; IP público só enquanto não houver balanceador); teste pelo IP; depois, balanceador com certificado e domínio, e o verificador de saúde apontando para o *liveness*.

**Pronto quando.** A API responde por HTTPS e o banco **não** é acessado como `root`.

**Sabotagem.** De fora da rede permitida, tente alcançar a porta do banco: deve falhar.

**Armadilhas clássicas.**
- *Sintoma:* a tarefa para com `AccessDeniedException ... GetSecretValue`. *Causa:* a política foi criada em outra role. *Correção:* criá-la na role de **execução** da tarefa e forçar novo deploy.
- *Sintoma:* a tarefa nem sobe. *Causa:* faltou um segredo novo na definição, ou um ARN inventado.
- *Sintoma:* a senha chega à aplicação como o próprio ARN. *Causa:* o tipo da variável é `Value` em vez de `ValueFrom`.
- *Sintoma:* `CannotPullContainerError`. *Causa:* o serviço foi criado antes de a imagem existir.
- *Sintoma:* custo alto. *Causa:* a criação do banco com os padrões (instância grande, disco grande). *Correção:* conferir a estimativa antes.
- *Sintoma:* senha e token em texto puro na rede. *Causa:* só HTTP. *Correção:* HTTPS.

**Entregáveis.** O roteiro numerado (para a pessoa executar), a definição da tarefa **sem senha**, e as diferenças de tela anotadas.

---

### Módulo 18. Esteira de deploy

**Objetivo.** O merge na branch principal publica a imagem e troca a tarefa.

**Decisões.** Quais eventos disparam o quê; onde ficam as credenciais da nuvem.

**Como.**
- *Princípio:* PR roda build e testes **sem acesso a segredos**; deploy **só** em push na branch principal e **só** depois de testes verdes; a imagem é marcada com o commit; o deploy espera o serviço estabilizar; permissões mínimas no workflow.
- *Perfil de referência:* passos de login na nuvem e no registro, `docker tag` com o commit e `latest`, `amazon-ecs-render-task-definition` e `amazon-ecs-deploy-task-definition` com espera pela estabilidade; `permissions: contents: read`; `paths-ignore` para mudanças só de documentação; as credenciais nos *secrets* do repositório.

**Pronto quando.** Um merge de teste na branch principal troca a tarefa e a rota nova responde (só com o ambiente ligado e o banco disponível).

**Sabotagem.** Abra um PR e confirme que os passos de deploy **não** rodam e que os segredos não aparecem.

**Armadilhas clássicas.**
- *Sintoma:* a esteira termina verde e a API não responde. *Causa:* o número de tarefas desejadas é 0 (a esteira não o altera), ou o IP mudou.
- *Sintoma:* o IP da API muda a cada deploy. *Causa:* o IP público é da tarefa. *Correção:* balanceador com nome estável.
- *Sintoma:* alguém mudou o workflow e o deploy parou. *Correção:* mudanças no workflow só com pedido e revisão.

**Entregáveis.** O workflow, os *secrets* cadastrados, a anotação do que dispara o quê.

---

### Módulo 19. Release

**Objetivo.** Levar uma versão para a produção sem surpresa.

**Decisões.** O número da versão; o que entra; a janela.

**Como.**
- *Princípio:* a release é um **procedimento**, não um improviso. O que importa: confirmar a dependência de dados disponível **antes** de mesclar, e **verificar a versão pelo comportamento** depois.
- *Passos (Git Flow):* criar `release/X.Y.Z` a partir de `develop`; PR para a `main`; esperar o build; **confirmar com o responsável: banco disponível e serviço com uma tarefa**; mesclar com *merge commit*; acompanhar o deploy até o fim; **verificar pelo comportamento** (uma rota nova responde, as protegidas dão 401, o que deve estar desligado dá 401); tag anotada no commit da `main`; *back-merge* da `main` na `develop` por PR; registrar nas anotações.

**Pronto quando.** A rota nova responde em produção, e a tag e o *back-merge* existem.

**Sabotagem.** Antes da primeira release real, ensaie o rollback e o procedimento do runbook.

**Armadilhas clássicas.**
- *Sintoma:* o deploy falha com *circuit breaker* e tarefas saindo com código 1. *Causa:* o banco estava parado ou subindo (a aplicação exige o banco no arranque). *Correção:* esperar o banco e reexecutar o job.
- *Sintoma:* "tudo verde" e a rota nova não existe. *Causa:* o rollback deixou a **versão anterior** no ar. *Correção:* verificar pelo comportamento.
- *Sintoma:* o PR de *back-merge* fica `BLOCKED` com o build "verde". *Causa:* o check do commit da `main` é o deploy que falhou. *Correção:* reexecutar o job; o PR destrava.
- *Sintoma:* a execução ficou em outro estado do que o responsável imaginava. *Causa:* confiou-se numa leitura parcial (o número de tarefas "rodando" pode ser de uma tarefa que cai e é recriada). *Correção:* pedir a confirmação.

**Entregáveis.** A release, a tag, o *back-merge* e a anotação do que aconteceu.

---

### Módulo 20. Operação

**Objetivo.** A operação deixa de depender de memória.

**Decisões.** O que alarmar; por quanto tempo guardar logs; qual a política de backup; quem é avisado.

**Como.**
- *Princípio:* quem opera precisa saber que algo quebrou **antes** do usuário; **backup que nunca foi restaurado não existe**; o custo é monitorado; o runbook permite a outra pessoa operar sem a conversa.
- *Perfil de referência:* alarmes (tarefa caindo, respostas 5xx, banco); logs centralizados com retenção; backups do banco **com teste de restauração**; alerta de custo; varredura de dependências e da imagem na esteira; um runbook (ligar e desligar, deploy falhou, rollback, banco fora, rotação de segredos).

**Pronto quando.** Outra pessoa consegue operar só com o runbook, e o próximo release sai sem surpresa.

**Sabotagem.** Derrube o banco de propósito em ambiente de teste e confira que o alarme dispara.

**Armadilhas clássicas.**
- *Sintoma:* o ambiente "ligou sozinho" e gerou custo. *Causa:* alguns serviços parados religam sozinhos depois de um prazo (por exemplo, 7 dias). *Correção:* anotar a data e o procedimento de desligar de novo.
- *Sintoma:* o backup existe e a restauração falha. *Causa:* nunca foi testada.
- *Sintoma:* o mesmo erro volta. *Causa:* a lição não foi anotada.

**Entregáveis.** Alarmes, política de logs e de backup, runbook, anotações.

---

## Parte D. Apêndices

### D1. Modelos de arquivos (o que cada um traz)

| Arquivo | Conteúdo essencial |
| --- | --- |
| `Dockerfile` | duas fases, usuário não root, cache de dependências, remoção de CRLF |
| Workflow de CI/CD | build e testes em PR; deploy só em push na principal; permissões mínimas |
| `application.yml` | só variáveis; segredo sem padrão; limites de paginação; saúde só `health` |
| `AbstractIntegrationTest` + perfil | contêiner único, migrations ligadas, `validate` |
| Teste de higiene | lê os arquivos reais e proíbe segredo com padrão, `*` no CORS, usuário semeado |
| `SecurityConfig` | regras por rota, `denyAll` no fim, CORS vazio, filtro criado com `new` |
| Tratador global de erros | formato único, 500 genérico, classificador de dependência fora |
| Definição da tarefa | segredos em `secrets` com ARN; nada sensível em `environment` |

### D2. Checklist de release

- [ ] Suíte inteira verde com os segredos **fora** do ambiente; também em ordem aleatória.
- [ ] Imagem constrói.
- [ ] Migration nova validada no banco real.
- [ ] Se a definição da tarefa ou as variáveis mudam: os segredos e as políticas já existem.
- [ ] Documentação da API atualizada.
- [ ] Responsável confirmou: **banco disponível** e serviço com uma tarefa.
- [ ] Depois do deploy: rota nova responde; protegidas dão 401; desligado dá 401; `health` UP.
- [ ] Tag, *back-merge* e anotações.

### D3. Runbook de incidentes

**Deploy falhou com "rolled back by the circuit breaker"**
1. Olhe os eventos do serviço e as tarefas paradas (motivo e código de saída). Código 1 logo na subida: a aplicação não conseguiu iniciar.
2. Causa mais comum: uma dependência indisponível (banco parado ou subindo). Confirme que ela está disponível.
3. O rollback deixa a **versão anterior** no ar. Confirme a versão por uma rota que só existe na nova.
4. Com o ambiente certo, reexecute só o job que falhou. Um PR de *back-merge* bloqueado destrava quando o job passar.

**Banco fora do ar**
1. A API deve responder 503 em poucos segundos e o *liveness* deve seguir UP. Se não, veja o módulo 14.
2. Religue o banco e espere ficar disponível. A API volta sozinha; se não voltar, reinicie a tarefa.
3. Registre a causa e o tempo de recuperação.

**Rotação de um segredo**
1. Crie o segredo novo, atualize a referência na definição da tarefa e libere a leitura.
2. Faça o deploy; os tokens emitidos com o segredo antigo deixam de valer.
3. Apague o segredo antigo.

### D4. Baseline de segurança (para o QA)

Anônimo recebe 401 em todas as rotas protegidas; sem permissão recebe 403; rota sem regra é negada; `alg=none`, outro segredo, papéis alterados, vencido e tipo errado são recusados; o login responde igual para qualquer falha e em tempo parecido; nenhum campo extra do cliente vira privilégio; injeção em login, filtros e ordenação não tem efeito; erros não vazam pilha, classe, SQL nem caminho; segredos, senhas e tokens não aparecem nos logs; CORS de origem não liberada não recebe cabeçalhos de permissão; caminhos sensíveis são negados até ao administrador; o `health` não mostra detalhes; a aplicação não sobe com segredo ausente ou curto.

### D5. Controle de custo

- Conferir a estimativa antes de criar qualquer recurso (os padrões costumam ser caros).
- O que cobra parado: armazenamento do banco, imagens, segredos.
- Como pausar: zerar as tarefas do serviço e parar o banco; anotar que o banco pode religar sozinho após um prazo.
- Alerta de orçamento configurado.

---

## Como validar este playbook

1. Crie uma **API pequena** (dois recursos) seguindo **só** este documento.
2. Cada vez que você precisar de algo que não está escrito, ou que está escrito errado, **anote a lacuna** e corrija o módulo.
3. Repita o QA do módulo 16 na API nova: se ele achar defeitos que o playbook deveria ter evitado, a armadilha correspondente está faltando.
