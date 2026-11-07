# Playbook2-Prompts: um prompt por módulo do Playbook2 (Spring Boot + MySQL + AWS)

Um prompt para cada módulo do `Playbook2.md` (0 a 20), para usar com Claude Code ou Codex. Nada aqui depende de recurso específico de uma ferramenta.

**Diferença para o `Colinha-Prompts.md`:** aquele é neutro quanto à stack e carrega as instruções em cada prompt. Este assume a stack fixa (Spring Boot, MySQL e AWS) e os prompts são **finos**: mandam o agente **ler o módulo** no `Playbook2.md` (onde está o "como") e repetem só o que não pode falhar (parâmetros, prova, sabotagem e o que não fazer). Assim o detalhe fica num lugar só e os prompts não ficam velhos.

---

## Como usar

1. **Preencha os parâmetros** (tabela abaixo) uma vez.
2. **Cole o prompt de abertura** e salve como o arquivo de contexto do projeto: `CLAUDE.md` (Claude Code) ou `AGENTS.md` (Codex). O conteúdo é o mesmo.
3. **Um prompt por vez,** na ordem dos módulos. Troque os marcadores pelos valores da tabela.
4. **Revise o diff e rode os testes você mesmo.** O agente diz "pronto"; a prova é sua.
5. **Nunca cole segredo** (senha, chave, token) num prompt. Use nomes de variáveis.
6. **Efeitos fora do repositório** (merge na `main`, deploy, console da AWS) ficam com você: os prompts mandam o agente parar e pedir o seu "pode", ou entregar um roteiro para você executar.

---

## Parâmetros do projeto

**Troque todos pelos seus valores.** Os exemplos são **fictícios** e formam um conjunto coerente (a API `loja-api`, com o recurso `Produto`).

| Marcador | O que é | Exemplo | Regra |
| --- | --- | --- | --- |
| `<api>` | nome da API (artefato, imagem, serviço) | `loja-api` | minúsculas, com hífen |
| `<banco>` | nome do banco (database) | `loja_api` | minúsculas, com sublinhado |
| `<grupo>` | pacote base do Java | `br.com.acme` | domínio invertido |
| `<Recurso>` | a entidade principal (classe) | `Produto` | singular, começa com maiúscula |
| `<recurso>` | o mesmo, em variável e tabela | `produto` | singular, minúsculas |
| `<recursos>` | o mesmo, na rota | `produtos` | plural, minúsculas |
| `<id-da-conta>` | ID da conta AWS | `123456789012` | 12 dígitos; o do exemplo é fictício; **nunca o real** em arquivo versionado |
| `<regiao>` | região da AWS | `us-east-1` | |
| `<repositorio>` | repositório de imagens (ECR) | `loja-api` | o nome não muda depois de criado |
| `<cluster>` / `<service>` | cluster e serviço do ECS | `loja-cluster` / `loja-service` | |
| `<prefixo>` | prefixo dos segredos | `loja` | os segredos viram `loja/db-password` e `loja/jwt-secret` |
| `<IP>` | o seu IP | `203.0.113.10` | faixa reservada para documentação; no firewall, com `/32` |
| `<dominio>` | domínio da API | `api.exemplo.com` | |
| `<porta>` | porta local do MySQL | `3307` | confira se a padrão (3306) está ocupada |
| `<pasta-do-projeto>` | pasta onde está o `pom.xml` | `loja-api` | |
| `<usuario>` | prefixo da imagem local | `acme` | a imagem fica `acme/loja-api` |
| `<nome-do-conteiner>` | contêiner dentro da tarefa | `loja-container` | |
| `<X.Y.Z>` | número da versão da release | `1.0.0` | |

**Exemplo de um prompt já preenchido** (módulo 4, com os valores do exemplo):

```text
Execute o módulo 4 do Playbook2.md (Banco e migrations) para o recurso Produto, seguindo o contrato em docs/contrato.md.
Antes de escrever código: leia o módulo inteiro e me mostre, em até 5 linhas, o plano e as decisões que dependem de mim.
Parâmetros: banco loja_api, MySQL local na porta 3307, usuário root.
Prove: a aplicação sobe no MySQL real e aplica a V1; trocar o nome de uma coluna na entidade faz a subida falhar (depois desfaça).
Não faça: editar migration aplicada, colocar dado de exemplo em migration de produção, commit ou push sem eu pedir.
```

---

## Prompt de abertura (vira o `CLAUDE.md` ou o `AGENTS.md`)

```text
Projeto: <api>. Stack: Java 25, Spring Boot 4.1.x, Maven, MySQL 8.4, Flyway, Docker, GitHub Actions e AWS (ECR, ECS Fargate, RDS, Secrets Manager).
O "como" de cada etapa está no Playbook2.md (módulos 0 a 20): leia o módulo inteiro antes de executar a etapa. As decisões iniciais e os parâmetros estão no próprio Playbook2.md e neste projeto.
Confirme a versão real do Spring Boot e das bibliotecas antes de seguir uma receita: pacotes mudam entre versões (o Boot 4 usa Jackson 3 e o Spring Data 4).

Regras:
1. Uma etapa por vez. Não adiante etapas nem misture duas.
2. Só diga "pronto" com prova: teste passando ou resposta real. Mostre a saída.
3. Depois da prova, sabote: quebre o código de um jeito conhecido, mostre o teste falhando e restaure (o arquivo deve voltar idêntico).
4. O repositório é público: nunca escreva senha, chave, token ou dado pessoal em arquivo versionado, em log ou neste chat. Use variáveis de ambiente e marcadores.
5. Negar por padrão: rota nova nasce fechada; segredo nunca tem valor padrão.
6. Nunca edite uma migration já aplicada; crie outra.
7. Git: trabalhe em branch e PR. Nunca faça push na main ou na develop, nem merge, nem deploy, nem mexa na AWS sem eu dizer "pode" naquela mensagem. O console da AWS é meu: entregue um roteiro.
8. Registre cada etapa nas anotações (PassoAPasso.md, memoria.md e, se envolver a nuvem, Duvida-CICD.md): o que foi feito, os comandos, o que deu errado e a prova.
9. Se faltar informação ou houver duas opções razoáveis, pare e pergunte.
10. A cada endpoint novo ou mudança, atualize o Swagger junto.

Ao terminar cada etapa, resuma: o que mudou, a prova, a sabotagem e o que ficou pendente.
```

---

## Bloco 1. Fundação

### Módulo 0. Regras de trabalho

```text
Execute o módulo 0 do Playbook2.md (Regras de trabalho).
Antes de criar arquivos: leia o módulo e me diga o que precisa que eu decida (repositório público ou privado, quem aprova o quê).
Entregue: memoria.md, PassoAPasso.md e Duvida-CICD.md; o CLAUDE.md ou AGENTS.md com o prompt de abertura; o .gitignore único na raiz.
Prove: uma sessão nova consegue retomar o projeto só lendo esses arquivos.
Não faça: nenhum segredo nos arquivos; use marcadores no lugar de IPs e IDs.
```

### Módulo 1. Projeto, repositório e CI

```text
Execute o módulo 1 do Playbook2.md (Projeto, repositório e CI).
Antes de começar: leia o módulo e me mostre o plano em até 5 linhas.
Parâmetros: grupo <grupo>, artefato <api>, Java 25, Spring Boot 4.1.x, fluxo Git Flow.
Faça nesta ordem: projeto, repositório, branches main e develop, workflow de CI, RODAR o workflow uma vez e só então criar o ruleset exigindo o check build.
Prove: um PR com teste quebrado de propósito fica com o build vermelho e o merge bloqueado (feche sem mesclar).
Não faça: push direto na main ou na develop; um segundo .gitignore.
Pare e pergunte se: o repositório for privado em conta gratuita (os rulesets não valem).
```

### Módulo 2. Contrato da API

```text
Execute o módulo 2 do Playbook2.md (Contrato da API) para o recurso <Recurso>.
Entregue um arquivo docs/contrato.md com a tabela de campos e a de endpoints, a ordem de validação e o formato de erro.
Não escreva nenhum código antes de eu aprovar o contrato.
Pare e pergunte se: faltar regra de negócio (unicidade, obrigatoriedade, exclusão lógica ou física, versão na URL).
Prove: eu li e aprovei.
```

### Módulo 3. Configuração e segredos

```text
Execute o módulo 3 do Playbook2.md (Configuração e segredos).
Prove: sem DB_PASSWORD (ou sem JWT_SECRET) a aplicação para na subida com mensagem clara dizendo qual variável falta; com elas, sobe. Mostre também que o Spring, sem o seu listener, mandaria o texto literal ${DB_PASSWORD}.
Entregue: application.yml, o listener no main, application-local.yml.example, o application.yml de teste com valores FALSOS e um teste de higiene que lê o application.yml real e proíbe segredo com valor padrão.
Sabotagem: coloque um valor padrão num segredo e mostre o teste de higiene falhar.
Não faça: valor real de segredo em arquivo versionado.
```

---

## Bloco 2. Dados e núcleo

### Módulo 4. Banco e migrations

```text
Execute o módulo 4 do Playbook2.md (Banco e migrations) para o recurso <Recurso>, seguindo docs/contrato.md.
Antes de escrever código: leia o módulo e me mostre, em até 5 linhas, o plano e as decisões que dependem de mim (collation, dados iniciais).
Parâmetros: banco <banco>, MySQL local na porta <porta>, usuário root. Confira antes se a porta padrão está ocupada.
Prove: a aplicação sobe no MySQL real e aplica a V1; trocar o nome de uma coluna na entidade faz a subida falhar (depois desfaça).
Sabotagem extra: edite uma migration já aplicada e mostre o Flyway recusar (depois restaure).
Não faça: editar migration aplicada; dado de exemplo em migration de produção; commit ou push sem eu pedir.
```

### Módulo 5. Camadas e erros padronizados

```text
Execute o módulo 5 do Playbook2.md (Camadas e erros padronizados).
Parâmetros: pacote base <grupo>.<api>, mensagens em português.
Prove: com um controller falso, 400, 404, 405, 406, 409, 415 e 500 saem no MESMO formato JSON (ErrorResponse), inclusive os erros do próprio Spring.
Sabotagem: remova o tratador do 409 (ou do 500) e mostre o teste falhar.
Não faça: vazar pilha, classe, SQL ou caminho nas respostas.
```

### Módulo 6. CRUD em fatia vertical

```text
Execute o módulo 6 do Playbook2.md (CRUD em fatia vertical) para <Recurso>, na rota /<recursos>, seguindo docs/contrato.md.
Antes de escrever código: mostre o plano e o que o cliente NÃO pode definir (id, situação).
Prove: cada endpoint cobre o caso feliz e 400, 404 e 409; uma data inexistente (2000-02-30) dá 400; dois DELETE simultâneos do mesmo recurso dão 1 sucesso e o resto 404.
Sabotagem: deixe a leitura da data leniente e troque o apagar atômico por ler-e-apagar; mostre os testes falharem.
Inclua: Swagger desligado por padrão, com as anotações numa interface <Recurso>ControllerDocs.
Não faça: CRUD em memória; copiar o formato de data de uma receita do Jackson 2 sem confirmar.
```

### Módulo 7. Testes com banco real

```text
Execute o módulo 7 do Playbook2.md (Testes com banco real).
Antes: confirme se o CI tem Docker; leia o módulo e mostre o plano.
Entregue: Testcontainers (MySQL) e REST Assured com UM contêiner para todas as classes, o perfil testcontainers (migrations ligadas, ddl-auto validate, sem script de dados), testes do fluxo completo e um teste de concorrência (20 chamadas simultâneas iguais).
Prove: mvn verify passa com as migrations reais; mostre as versões do Testcontainers (as da BOM do Boot).
Sabotagem: desligue o Flyway no perfil e mostre o contexto não subir.
Pare e pergunte se: o CI não tiver Docker.
```

### Módulo 8. Listagem

```text
Execute o módulo 8 do Playbook2.md (Listagem) para /<recursos>.
Parâmetros: tamanho padrão 20 e máximo 50; campos ordenáveis: <lista de campos>.
Prove: um size gigante é reduzido ao máximo; sort=nome;drop table x dá 400 (não 500) e a tabela continua intacta; percorrer todas as páginas não repete nem perde itens; o filtro % não devolve tudo.
Sabotagem: remova a lista de campos ordenáveis e mostre o teste falhar.
Não faça: SQL concatenado; repetir o limite de paginação só no application.yml principal (repita no de teste).
```

### Módulo 9. Formato da resposta

```text
Execute o módulo 9 do Playbook2.md (Formato da resposta).
Antes: me pergunte se preciso de HAL, XML ou YAML; se eu não pedir, só JSON.
Prove: um teste compara o texto EXATO do JSON de uma resposta e de um erro.
Sabotagem: mude a ordem de um campo e mostre o teste falhar.
Confirme a biblioteca JSON da versão do Boot antes de usar anotações ou pacotes.
```

---

## Bloco 3. Segurança

### Módulo 10. Autenticação

```text
Execute o módulo 10 do Playbook2.md (Autenticação).
FAÇA PRIMEIRO UM PLANO e espere o meu OK. Esta etapa deixa a API toda protegida e quebra os testes de contrato por 401 até serem ajustados: trabalhe numa branch e só abra o PR com o build verde.
Prove: o login entra; token vencido, adulterado, de outro segredo, com alg=none, com papéis alterados e do tipo errado dão 401; desativar o usuário no banco derruba o token na requisição seguinte; mvn -B clean verify passa com os segredos FORA do ambiente.
Também prove: o login devolve a MESMA resposta (e tempo parecido) para qualquer falha.
Sabotagem: tire a conferência do tipo do token e mostre testes falharem.
Não faça: o refresh na URL; o filtro como bean; senha ou hash em resposta ou log.
```

### Módulo 11. Autorização e CORS

```text
Execute o módulo 11 do Playbook2.md (Autorização e CORS).
Parâmetros: papéis USER e ADMIN; DELETE só ADMIN; origens de CORS: nenhuma (lista vazia).
Prove: uma matriz de testes perfil x rota (anônimo, sem permissão, outra permissão, USER, ADMIN) passa; rota nova sem regra dá 403 a quem está logado e 401 ao anônimo; permissão revogada no banco vale na requisição seguinte.
Sabotagens (uma de cada vez, mostrando a falha): deixar o USER apagar; trocar denyAll por authenticated; esquecer o HEAD; remover .cors() e a liberação do OPTIONS juntos.
Não faça: ler a permissão do token em vez do banco; CORS com *.
```

### Módulo 12. Admin inicial e endurecimento

```text
Execute o módulo 12 do Playbook2.md (Admin inicial e endurecimento).
A senha do admin é escolhida por MIM e nunca passa pelo repositório nem por este chat.
Prove: um banco novo termina sem usuário conhecido; subir com ADMIN_BOOTSTRAP_PASSWORD cria o admin; subir de novo com a variável NÃO recria nem troca a senha; sem a variável o login continua funcionando.
Entregue também: os guardas que leem os arquivos reais (sem segredo com padrão, sem segredo em environment da Task Definition, só uma migration semeia usuário, * proibido no CORS) e os testes dos caminhos sensíveis (401 ao anônimo, 403 até ao ADMIN).
Sabotagem: escreva a senha no log e coloque um segredo em environment; mostre os testes falharem.
```

### Módulo 13. Limites de abuso e ciclo de vida do token

```text
Execute o módulo 13 do Playbook2.md (Limites de abuso e ciclo de vida do token).
FAÇA PRIMEIRO UM PLANO e espere o meu OK: há decisões de produto (limites, desbloqueio, rotação ou lista de revogação, instâncias).
Prove: tentativas em excesso recebem 429 com Retry-After; refresh revogado dá 401; trocar a senha invalida os tokens antigos; o limite não revela se o usuário existe.
Atualize o Swagger e os testes.
Observação: este módulo não foi feito no projeto de referência; trate o texto do Playbook2 como ponto de partida e confirme as escolhas comigo.
```

---

## Bloco 4. Operação

### Módulo 14. Falha de infraestrutura

```text
Execute o módulo 14 do Playbook2.md (Falha de infraestrutura).
Parâmetros: timeout de conexão com o banco de 5 s.
Prove, com um banco real DESCARTÁVEL que você derruba de verdade: GET autenticado, login e health completo devolvem 503 com Retry-After em poucos segundos; o liveness continua UP; a API volta sozinha quando o banco volta.
Sabotagem: volte o timeout ao padrão e mostre o teste falhar; remova o tratamento do filtro de autenticação e mostre o 401 enganoso reaparecer.
Não faça: tratar qualquer erro genérico como "banco fora"; expor detalhes no health; tocar no meu banco de desenvolvimento.
```

### Módulo 15. Contêiner

```text
Execute o módulo 15 do Playbook2.md (Contêiner).
Parâmetros: imagem <usuario>/<api>, pasta do projeto <pasta-do-projeto>.
Prove: docker build e docker run respondem; o processo NÃO é root (docker exec ... id); o CI constrói a imagem em todo PR.
Sabotagem: remova a linha do usuário, confira que o id mostra root e restaure.
Não faça: rodar os testes dentro do build da imagem; propor healthCheck interno sem conferir se a imagem tem curl ou wget.
```

### Módulo 16. QA da imagem de produção

```text
Execute o módulo 16 do Playbook2.md (QA da imagem de produção).
Use um banco real DESCARTÁVEL numa rede isolada, com as mesmas variáveis da produção e o admin pelo bootstrap. Não toque na AWS nem no meu banco de desenvolvimento.
Cubra tudo o que o módulo lista (autorização, tokens, injeção, limites, ids estranhos, paginação, concorrência, reinício, segredos ausentes, banco derrubado, logs).
Entregue: relatório de defeitos (severidade, como reproduzir, causa), as correções (cada uma com teste e sabotagem) e o QA REFEITO na imagem corrigida.
No fim: remova contêineres, rede, imagem e arquivos de senha.
Pare e pergunte se: algum defeito exigir mudança de contrato.
```

---

## Bloco 5. Entrega

### Módulo 17. Nuvem (AWS)

```text
Gere o roteiro numerado do módulo 17 do Playbook2.md (Nuvem) para EU executar no console da AWS. NÃO execute nada na AWS e não peça credenciais.
Parâmetros: região <regiao>; repositório <repositorio>; cluster <cluster>; serviço <service>; prefixo dos segredos <prefixo>; meu IP <IP>.
Entregue: o roteiro na ordem do módulo (com a conferência do custo ANTES de criar o banco); a política de leitura dos segredos na role de EXECUÇÃO da tarefa; o task-definition.json sem senha, com os ARNs como marcadores até eu fornecê-los (um ARN inventado quebra o deploy); e o Duvida-CICD.md com as diferenças de tela que eu relatar.
Inclua no roteiro: usuário de banco só com privilégio no schema da aplicação, HTTPS com domínio <dominio>, e como ligar e desligar o ambiente (o RDS parado religa sozinho em 7 dias).
Só use comandos de LEITURA da AWS (apêndice D6) quando eu mandar rodar.
Pare e pergunte se: faltar um ARN, nome ou região.
```

### Módulo 18. Esteira de deploy

```text
Execute o módulo 18 do Playbook2.md (Esteira de deploy).
Parâmetros: região <regiao>; repositório <repositorio>; cluster <cluster>; serviço <service>; contêiner <nome-do-conteiner>.
Entregue: os passos de deploy SÓ em push na main (PR nunca acessa segredos), com permissions: contents: read e paths-ignore para documentação. Os secrets da AWS no GitHub eu cadastro.
Prove: um PR NÃO roda os passos de deploy nem mostra segredos. O merge de teste na main só com o meu "pode" e com o RDS Available.
Não faça: alterar o workflow depois que estiver funcionando, sem eu pedir.
```

### Módulo 19. Release

```text
Execute o módulo 19 do Playbook2.md (Release) para a versão <X.Y.Z>.
Passos: release/<X.Y.Z> a partir da develop; PR para a main; esperar o build; PARE e me peça o "pode" e a confirmação de que o RDS está Available e o serviço tem uma tarefa; só então mescle (merge commit); acompanhe o deploy; verifique PELO COMPORTAMENTO (uma rota nova responde, as protegidas dão 401, o que deve estar desligado dá 401); tag anotada; back-merge por PR; registre nas anotações.
Se o deploy falhar: olhe primeiro os eventos do serviço e as tarefas paradas (motivo e código de saída). O rollback deixa o código ANTIGO no ar. Só reexecute o job (gh run rerun <id> --failed) depois que o ambiente estiver certo e eu disser "pode".
Prove: a rota nova responde em produção e a tag e o back-merge existem.
```

### Módulo 20. Operação

```text
Execute o módulo 20 do Playbook2.md (Operação), entregando ROTEIROS para eu executar (o console é meu) e os arquivos do repositório.
Entregue: alarmes (tarefa caindo, respostas 5xx, banco, custo), retenção de logs, backups do banco conferidos COM teste de restauração, varredura de dependências e da imagem na esteira e o runbook (ligar e desligar, deploy falhou, rollback, banco fora, rotação de segredos).
Prove: outra pessoa consegue operar só com o runbook.
Observação: este módulo não foi feito no projeto de referência; confirme as escolhas comigo.
```

---

## Prompts de apoio

### Fechar a etapa

```text
Antes de encerrar esta etapa: mostre a saída dos testes (suíte inteira, com os segredos fora do ambiente), mostre a sabotagem que você fez e a restauração, rode git status e git diff --stat, confirme que não há segredo nos arquivos alterados e atualize PassoAPasso.md, memoria.md e (se envolver a nuvem) Duvida-CICD.md. Não faça commit, push, merge nem deploy: me peça.
```

### Retomar uma sessão

```text
Retome o projeto: leia o CLAUDE.md (ou AGENTS.md), a memoria.md e a seção "Próximos passos" do PassoAPasso.md. Diga em que módulo do Playbook2.md estamos, o que está pendente e qual é o próximo passo. Não altere nada ainda.
```

### Diagnosticar uma falha

```text
Algo falhou: <descreva o sintoma e cole a mensagem, sem segredos>. Diagnostique SEM alterar nada: liste as hipóteses em ordem de probabilidade, o que cada uma prevê e o comando de leitura que a confirma ou descarta (veja a tabela de armadilhas do módulo e o runbook D8 do Playbook2.md). Depois de eu escolher, proponha a correção com o teste que a prova e a sabotagem.
```

### Revisão crítica

```text
Revise criticamente o módulo <N> como um QA experiente: o que ainda pode quebrar, que caso não foi testado, que regra do prompt de abertura foi cumprida só na aparência. Liste os achados por severidade, com como reproduzir, e NÃO corrija nada sem eu escolher.
```

### Atualizar o Swagger

```text
Atualize o Swagger para a mudança que acabamos de fazer: interface <Recurso>ControllerDocs, respostas (200/201/204 e 400/401/403/404/409/503) e exemplos dos corpos de entrada (nunca com senha de exemplo), e confirme no OpenApiDocsTests. Mostre o que mudou.
```
