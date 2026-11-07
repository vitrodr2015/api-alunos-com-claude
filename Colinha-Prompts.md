# Colinha de prompts: criar uma nova API com Claude Code ou Codex

Um prompt para cada etapa da `Colinha-Nova-API.md`, na mesma numeração (0 a 20). Servem para o Claude Code e para o Codex: nada aqui depende de recurso específico de uma ferramenta.

---

## Como usar

1. **Cole o prompt de abertura uma vez** e salve como o arquivo de contexto do projeto: `CLAUDE.md` para o Claude Code, `AGENTS.md` para o Codex (o conteúdo é o mesmo; um pode apontar para o outro). Ele passa a valer em todas as conversas.
2. **Um prompt por vez.** Troque os `<marcadores>`, cole, revise o que o agente fez e só então passe para o próximo.
3. **Revise o diff e rode os testes você mesmo.** O agente diz "pronto"; a prova é sua.
4. **Etapas com "PLANO":** o prompt manda o agente propor o plano e esperar o seu OK antes de escrever código.
5. **Nunca cole segredo** (senha, chave, token) num prompt. Use nomes de variáveis.
6. **Efeitos fora do repositório** (merge na `main`, deploy, console da nuvem) ficam com você: os prompts mandam o agente parar e pedir o seu "pode".

O molde de cada prompt: **Objetivo**, **Entregáveis**, **Restrições**, **Pronto quando** (a prova), **Sabotagem** (quebrar de propósito para ver o teste falhar) e **Pare e pergunte se**. As regras comuns (anotar, provar, não mexer na nuvem) estão no prompt de abertura e não se repetem.

---

## Prompt de abertura

```text
Você vai me ajudar a construir uma API profissional, uma etapa por vez, seguindo a Colinha-Nova-API.md.

Stack de referência: Java, Spring Boot, Maven, MySQL, Flyway, Docker, GitHub Actions e AWS (troque se for outra). Antes de seguir qualquer receita, confirme a versão e a API reais da biblioteca: nomes e pacotes mudam entre versões.

Regras do projeto:
1. Uma etapa por vez. Não adiante etapas nem misture duas.
2. Só diga "pronto" com prova: teste passando ou resposta real. Mostre a saída.
3. Depois da prova, sabote: quebre o código de um jeito conhecido, mostre o teste falhando e restaure (o arquivo deve voltar idêntico).
4. O repositório é público. Nunca escreva senha, chave, token ou dado pessoal em arquivo versionado, em log ou neste chat. Use variáveis de ambiente e marcadores (<id-da-conta>, <IP>).
5. Negar por padrão: rota nova nasce fechada; segredo nunca tem valor padrão.
6. Nunca edite uma migration já aplicada; crie outra.
7. Git: trabalhe em branch e PR. Nunca faça push na main ou na develop, nem merge, nem deploy, nem mexa na nuvem sem eu dizer "pode" naquela mensagem.
8. Registre cada etapa nas anotações (PassoAPasso.md, memoria.md e, se envolver a nuvem, Duvida-CICD.md): o que foi feito, os comandos, o que deu errado e a prova.
9. Se faltar informação ou houver duas opções razoáveis, pare e pergunte em vez de decidir sozinho.
10. A cada endpoint novo ou mudança, atualize o Swagger junto (quando ele existir).

Ao terminar cada etapa, resuma: o que mudou, a prova, a sabotagem e o que ficou pendente.
```

---

## 1. Fundação

### Etapa 0: regras de trabalho

```text
Etapa 0: regras de trabalho.
Objetivo: criar os arquivos de contexto e de anotação do projeto.
Entregáveis: memoria.md (regras, decisões, estado, pendências), PassoAPasso.md (registro cronológico) e, se houver nuvem, Duvida-CICD.md; o CLAUDE.md ou AGENTS.md com o prompt de abertura; .gitignore na raiz que ignore application-local.yml, .env* e arquivos de segredo.
Restrições: nenhum segredo; marcadores no lugar de IPs e IDs.
Pronto quando: os arquivos existem e uma sessão nova consegue retomar o projeto só lendo-os.
Pare e pergunte se: houver dúvida sobre o que é público e o que é privado.
```

### Etapa 1: projeto, repositório e CI

```text
Etapa 1: projeto, repositório e CI.
Objetivo: criar o projeto, publicar o repositório e deixar um CI que roda build e testes em todo PR.
Entregáveis: projeto <grupo>/<artefato> gerado com dependências mínimas (web e devtools); Maven Wrapper mantido; um .gitignore só (na raiz); branches main e develop; workflow que roda `mvn -B verify` em push na main e em PR para main, develop e release/**.
Restrições: a mesma versão de Java no pom, no Dockerfile futuro e no CI. Faça nesta ordem: workflow, rodar uma vez, e SÓ ENTÃO criar o ruleset exigindo o check `build` (ele só aparece depois de rodar): PR obrigatório, sem force push, sem apagar main e develop.
Pronto quando: um PR com teste quebrado de propósito fica com o build vermelho e o merge bloqueado (feche-o sem mesclar).
Pare e pergunte se: o repositório for privado em conta gratuita (os rulesets não valem) ou se eu não tiver definido o fluxo de branches.
```

### Etapa 2: contrato da API

```text
Etapa 2: contrato da API.
Objetivo: escrever o contrato antes de qualquer código.
Entregáveis: tabela do recurso <recurso> (campos, tipos, regras, quem preenche); tabela de endpoints (verbo, rota, entrada, sucesso, erros); ordem de validação (corpo, id, existência, conflito, alteração); formato único de erro.
Restrições: sem versão na URL salvo pedido; a entidade nunca sai na API (DTOs de entrada e de saída); id inválido é 400, inexistente 404, duplicado 409; datas só em formato ISO e válidas.
Pronto quando: eu li e aprovei o contrato. Não gere código antes disso.
Pare e pergunte se: faltar regra de negócio (unicidade, campos obrigatórios, exclusão lógica ou física).
```

### Etapa 3: configuração e segredos

```text
Etapa 3: configuração e segredos.
Objetivo: toda configuração por variável de ambiente, e segredo sem valor padrão.
Entregáveis: application.yml com `${VAR}` sem padrão para senha e segredos e padrão seguro para o resto; verificação na subida que para a aplicação com mensagem clara se faltar um segredo (o framework pode enviar o texto literal `${VAR}` como valor sem falhar: prove isso e trate); application-local.yml ignorado e um .example versionado; teste de higiene que lê o application.yml real e proíbe segredo com valor padrão.
Restrições: o application.yml de teste substitui o principal: repita nele o que os testes exigem, com valores FALSOS e claramente de teste.
Pronto quando: sem a variável a aplicação para com texto claro; com ela, sobe.
Sabotagem: coloque um valor padrão num segredo e mostre o teste de higiene falhando.
Pare e pergunte se: houver segredo que eu ainda não tenha.
```

---

## 2. Dados e núcleo da API

### Etapa 4: banco, migrations e entidade

```text
Etapa 4: banco, migrations e entidade.
Objetivo: persistência com esquema versionado.
Entregáveis: MySQL local em Docker (verifique antes se a porta padrão está ocupada; volume nomeado; utf8mb4); datasource por variáveis; Flyway com a V1 criando a tabela de <recurso> com as restrições do contrato (único onde ele mandar); entidade com nomes de coluna explícitos; repository; `ddl-auto: validate`; testes rápidos com banco em memória (Flyway desligado neles, dados em data.sql SEM id explícito).
Restrições: confirme o starter correto do Flyway para a versão do framework; nunca edite migration aplicada; nenhum dado de exemplo em migration de produção sem eu aprovar.
Pronto quando: a aplicação sobe no MySQL real e o Flyway aplica a V1; troque o nome de uma coluna na entidade, mostre a subida falhar e desfaça.
Pare e pergunte se: precisar de dados iniciais (diga se iriam para produção).
```

### Etapa 5: camadas e erros padronizados

```text
Etapa 5: camadas e erros padronizados.
Objetivo: estrutura em camadas e um único formato de erro.
Entregáveis: pacotes controller, service, repository, model, dto, mapper, exception, config e security; ErrorResponse (data e hora em UTC, status, erro, mensagem); exceções de negócio (não encontrado, requisição inválida, conflito); tratador global que devolve SEMPRE JSON no mesmo formato para 400, 404, 405, 406, 409, 415 e 500; o 500 com mensagem genérica (o detalhe só no log).
Restrições: mensagens em <idioma>; nunca vazar pilha, classe, SQL ou caminho; os erros do próprio framework também passam pelo formato.
Pronto quando: testes com um controller falso cobrem cada status e o formato.
Sabotagem: remova o tratador do 409 (ou do 500) e mostre o teste falhar.
```

### Etapa 6: CRUD em fatia vertical

```text
Etapa 6: CRUD em fatia vertical.
Objetivo: os endpoints do contrato, ponta a ponta.
Entregáveis: controller (sem lógica), service (regras, transações, `readOnly` nas leituras), DTOs com validação, mapper e os endpoints do contrato; testes unitários do service (Mockito) e testes web (MockMvc); Swagger/OpenAPI desde já, com as anotações em interfaces `*Docs`, documentando 200/201/204 e 400/401/404/409, DESLIGADO por padrão e ligado por variável.
Restrições: validação ESTRITA de entrada (data inexistente como 2000-02-30 dá 400, não é corrigida: confirme como o parser do framework trata isso); id inválido é 400; apagar numa única instrução atômica (devolve as linhas apagadas; 0 vira 404), nunca ler e depois apagar; campos que o cliente não deve definir (id, situação) são ignorados.
Pronto quando: cada endpoint prova o caso feliz e os erros 400, 404 e 409; dois DELETE simultâneos do mesmo recurso dão 1 sucesso e o resto 404.
Sabotagem: deixe a data leniente e mostre o teste falhar.
```

### Etapa 7: testes com banco real

```text
Etapa 7: testes com banco real.
Objetivo: um nível de teste de integração contra o banco de verdade.
Entregáveis: Testcontainers (MySQL) e REST Assured; classe base com UM contêiner único (`@ServiceConnection`) e perfil `testcontainers` (Flyway ligado, `ddl-auto: validate`, sem data.sql); testes do fluxo completo e dos erros 400, 404 e 409; um teste de concorrência (por exemplo, 20 chamadas simultâneas iguais).
Restrições: exige Docker (local e no CI; confirme que o runner do CI tem); use as versões do Testcontainers da BOM do framework e confirme os nomes dos artefatos; nenhum segredo real.
Pronto quando: `mvn verify` passa com as migrations reais rodando.
Sabotagem: desligue o Flyway no perfil e mostre o contexto não subir.
Pare e pergunte se: o CI não tiver Docker.
```

### Etapa 8: listagem

```text
Etapa 8: listagem.
Objetivo: paginação, filtros e ordenação seguras.
Entregáveis: `Pageable` com tamanho padrão e máximo configurados (repita no yml de teste); filtros por <campos>; ordenação SOMENTE por uma lista fixa de campos (qualquer outro valor, inclusive com `;` ou espaço, é 400); desempate por id; consultas derivadas (o framework escapa os curingas); testes de páginas seguidas sem repetir nem pular, tamanho gigante reduzido, página além do fim, filtros com `%`, `_` e aspas.
Restrições: nada de SQL concatenado; o erro não revela detalhes internos.
Pronto quando: `sort=nome;drop table x` dá 400 (não 500) e a tabela continua intacta.
Sabotagem: remova a lista de campos e mostre o teste falhar.
```

### Etapa 9: formato da resposta

```text
Etapa 9: formato da resposta.
Objetivo: respostas previsíveis.
Entregáveis: JSON como padrão; ordem dos campos e formato de datas padronizados por anotação. Só se eu pedir: HAL (links) e XML ou YAML.
Restrições: confirme a biblioteca JSON da versão do framework (pacotes mudaram entre versões); os erros são sempre JSON.
Pronto quando: um teste compara o texto exato do JSON de uma resposta e de um erro.
Pare e pergunte se: eu não tiver dito se preciso de HAL ou de outros formatos.
```

---

## 3. Segurança

### Etapa 10: autenticação

```text
Etapa 10: autenticação.
Faça primeiro um PLANO e espere o meu OK. Esta etapa deixa a API toda protegida e os testes de contrato antigos quebram por 401 até serem ajustados: trabalhe numa branch e só abra o PR com o build verde.
Objetivo: usuários, hash de senha, JWT, filtro, login e refresh.
Entregáveis: migration de usuários, permissões e ligação (tabela `users`, no plural); entidades que implementam UserDetails e GrantedAuthority; hash BCrypt por encoder delegante com SÓ o BCrypt registrado (recusa algoritmos inseguros; senha acima de 72 bytes é recusada); provedor de JWT (HS256; acesso 15 min; refresh 24 h; claims de emissor, sujeito, papéis, tipo, emissão e expiração; valida assinatura, emissor, expiração e TIPO; recarrega o usuário do banco a cada validação; segredo sem padrão, mínimo 32 bytes); filtro que NÃO é bean (criado com `new` na cadeia); configuração stateless, CSRF desligado, sem formulário nem Basic; 401 e 403 em JSON no formato de erro; POST /auth/signin e /auth/refresh públicos (o refresh só no corpo, nunca na URL); cadastro só para ADMIN.
Restrições: o login devolve a MESMA resposta para qualquer falha (usuário inexistente, senha errada, conta desativada) e com tempo parecido (meça); senha e hash nunca saem na resposta nem no log; a resposta com token leva `Cache-Control: no-store`.
Pronto quando: o login entra; token vencido, adulterado, de outro segredo, com `alg=none`, com papéis alterados e do tipo errado dão 401; desativar o usuário no banco derruba o token na requisição seguinte; os testes de contrato foram ajustados (filtros desligados, com aviso) e a suíte inteira passa com os segredos FORA do ambiente, como no CI.
Sabotagem: tire a conferência do tipo do token e mostre testes falhando.
Pare e pergunte se: houver escolha de algoritmo, tempo de expiração ou regra de senha que eu não tenha definido.
```

### Etapa 11: autorização e CORS

```text
Etapa 11: autorização e CORS.
Objetivo: definir quem pode o quê e quem pode chamar pelo navegador.
Entregáveis: regras por rota num lugar só (uma tabela legível), permissões em constantes; REGRA FINAL `denyAll()` (não `authenticated()`); HEAD com regra própria; apagar só ADMIN (ou como o contrato mandar); CORS com a lista de origens vinda de variável, VAZIA por padrão, sem credenciais, e que recusa `*` e formatos inválidos na subida; matriz de testes perfil x rota (anônimo, sem permissão, outra permissão, USER, ADMIN).
Restrições: o banco manda, não o token (permissão revogada no banco vale na requisição seguinte); a autorização vem antes do 404 (não revelar quais ids existem); o pré-voo do CORS passa sem token.
Pronto quando: rota nova sem regra dá 403 a quem está logado e 401 ao anônimo; a matriz passa; origem permitida passa e origem não permitida é barrada.
Sabotagem: deixe o USER apagar; volte a regra final para `authenticated()`; remova `.cors()` e a liberação do OPTIONS ao mesmo tempo; mostre testes falhando em cada caso.
```

### Etapa 12: admin inicial e endurecimento

```text
Etapa 12: admin inicial e endurecimento.
Objetivo: nenhum login conhecido no ambiente publicado.
Entregáveis: nenhum usuário semeado com senha conhecida (se uma migration semeou, outra o remove SÓ se a senha ainda for a semeada); criação do primeiro admin por variável usada numa única execução (desligada por padrão; só cria se não existir ADMIN; senha de 12+ caracteres, sem espaço ou quebra de linha nas pontas; falha se já existir usuário comum com o nome; nunca registra a senha no log); testes de caminhos sensíveis (actuator, console de banco, .env, .git, arquivos de configuração) que dão 401 ao anônimo e 403 até ao ADMIN; testes de que os erros não vazam pilha nem classes; guardas que leem os arquivos reais: sem segredo com valor padrão, sem segredo em `environment` da definição da tarefa, só uma migration pode semear usuário, `*` proibido no CORS.
Restrições: a senha do admin é escolhida por MIM e nunca passa pelo repositório nem por este chat.
Pronto quando: um banco novo termina sem usuário conhecido; subir com a variável cria o admin; subir de novo com a variável não recria nem troca a senha; sem a variável o login continua funcionando.
Sabotagem: escreva a senha no log, depois um segredo em `environment`, e mostre os testes falhando.
```

### Etapa 13: limites de abuso e ciclo de vida do token

```text
Etapa 13: limites de abuso e ciclo de vida do token.
Faça primeiro um PLANO e espere o meu OK (há decisões de produto).
Objetivo: reduzir força bruta e dar controle sobre as sessões.
Entregáveis: limite de tentativas de login (por usuário e por origem) com 429 e `Retry-After`; logout e revogação do refresh token (rotação ou lista de revogação); troca de senha pelo próprio usuário; testes de cada um; Swagger atualizado.
Restrições: o limite não pode revelar se o usuário existe; o admin legítimo não pode ficar bloqueado para sempre (defina o desbloqueio).
Pronto quando: tentativas em excesso recebem 429; refresh revogado dá 401; senha trocada invalida os tokens antigos.
Pare e pergunte se: precisar de armazenamento novo (cache ou tabela) ou se isso mudar o contrato.
```

---

## 4. Operação

### Etapa 14: saúde e falha de infraestrutura

```text
Etapa 14: saúde e falha de infraestrutura.
Objetivo: a API sabe dizer que está viva e se comporta bem quando uma dependência cai.
Entregáveis: Actuator expondo SÓ o `health`, público e só de leitura, com liveness e readiness e sem detalhes (o resto do actuator negado); timeout curto de conexão com o banco (por exemplo, 5 s); classificação das exceções de "dependência fora do ar" (sem conexão, sem transação, tempo esgotado) para responder 503 com `Retry-After`, inclusive no filtro de autenticação e no login; logs sem segredo e sem dado pessoal; guarda de higiene da exposição do actuator.
Restrições: não trate erro genérico como "banco fora"; o liveness NÃO depende do banco (é o que o orquestrador consulta).
Pronto quando: derrubar o banco de verdade (contêiner descartável) devolve 503 em poucos segundos, o liveness continua UP e a API volta sozinha quando o banco volta.
Sabotagem: volte o timeout ao padrão e mostre o teste falhar.
```

### Etapa 15: contêiner

```text
Etapa 15: contêiner.
Objetivo: imagem enxuta e segura.
Entregáveis: Dockerfile em duas fases (build com JDK; execução só com JRE), camada de dependências em cache, remoção de CRLF do wrapper, usuário que NÃO é root, porta exposta; `.dockerignore`; docker-compose só para dar nome à imagem; o CI passa a construir a imagem.
Restrições: mesma versão de Java do pom e do CI; os testes rodam antes (no CI), não dentro do build da imagem; confirme se a imagem tem curl ou wget antes de propor um healthCheck interno.
Pronto quando: a imagem roda local, responde, e o processo não é root.
```

### Etapa 16: QA da imagem de produção

```text
Etapa 16: QA da imagem de produção.
Objetivo: atacar o artefato que vai para a produção antes de ele ir.
Faça: construa a imagem e suba com um banco real DESCARTÁVEL numa rede isolada, com as mesmas variáveis de produção e o admin pelo bootstrap. Não toque na nuvem nem no meu banco de desenvolvimento.
Cubra: matriz de autorização por perfil; ataques ao token; cadastro (limites e campos extras); injeção; limites de todos os campos; ids estranhos; paginação, ordenação e filtros; negociação de conteúdo; corpo gigante; CORS; erros sem vazar detalhes; concorrência; persistência e reinício; troca do segredo do JWT; segredos ausentes; banco derrubado e recuperação; desligamento limpo; segredos e tokens fora dos logs.
Entregáveis: relatório com cada defeito (severidade, como reproduzir, causa); correções, cada uma com teste e sabotagem; o QA REFEITO na imagem corrigida; no fim, remova contêineres, rede, imagem e arquivos de senha.
Pronto quando: nenhuma resposta 5xx para entrada inválida e nenhum segredo no log.
Pare e pergunte se: algum defeito exigir mudança de contrato.
```

---

## 5. Entrega

### Etapa 17: nuvem

```text
Etapa 17: nuvem.
NÃO execute nada na nuvem. O console é meu. Prepare um ROTEIRO numerado para eu executar, e os arquivos do repositório.
Objetivo: infraestrutura mínima e segura.
Roteiro, nesta ordem: banco (uma zona, instância pequena; confira a estimativa de custo antes de criar); repositório de imagens; usuário da esteira com poucas permissões; cluster; TODOS os segredos (senha do banco, segredo do JWT e senha do admin inicial) com a política de leitura na role de EXECUÇÃO da tarefa (o Resource termina em `-*`); firewall (o banco só para a aplicação e para o meu IP, nunca 0.0.0.0/0); definição da tarefa (segredos em `secrets` com `valueFrom` = ARN real, nunca em `environment`); primeira imagem; serviço (sem sub-redes de zona local; IP público só enquanto não houver balanceador); teste pelo IP.
Também no roteiro: usuário de banco com privilégio SÓ no schema da aplicação (nunca root); HTTPS e domínio (balanceador e certificado), com o verificador de saúde apontando para o liveness; como ligar e desligar o ambiente e o custo diário.
Entregáveis: o roteiro; o task-definition.json SEM senha (só depois de eu fornecer os ARNs: ARN inventado quebra o deploy); Duvida-CICD.md com as diferenças de tela.
Pare e pergunte se: faltar um ARN, nome ou região.
```

### Etapa 18: esteira de deploy

```text
Etapa 18: esteira de deploy.
Objetivo: o merge na main publica a imagem e troca a tarefa.
Entregáveis: no workflow, passos de deploy SÓ em push na `main` (PR nunca acessa segredos): login na nuvem e no registro, tag da imagem com o commit e `latest`, render da definição da tarefa, deploy esperando estabilizar; `permissions: contents: read`; `paths-ignore` para mudanças só de documentação.
Restrições: os segredos da nuvem ficam nos secrets do repositório (eu os cadastro); depois de funcionar, não altere o workflow sem eu pedir.
Pronto quando: um merge de teste na main troca a tarefa e a rota nova responde. Só execute com o meu "pode" e com o banco `Available`.
Pare e pergunte se: o ambiente estiver desligado.
```

### Etapa 19: release

```text
Etapa 19: release.
Objetivo: levar a versão para a produção sem surpresa.
Passos: crie `release/X.Y.Z` a partir da develop; abra o PR para a main; espere o build; PARE e peça o meu "pode" e a confirmação de que o banco está `Available` e o serviço tem uma tarefa; só então mescle (merge commit); acompanhe o deploy; verifique PELO COMPORTAMENTO (uma rota nova responde, as rotas protegidas dão 401, o que deve estar desligado dá 401); crie a tag anotada; faça o back-merge por PR para a develop; registre nas anotações.
Se o deploy falhar: olhe primeiro os eventos do serviço e as tarefas paradas (motivo e código de saída). Lembre que o rollback deixa o código ANTIGO no ar. Só reexecute (`gh run rerun <id> --failed`) depois que o ambiente estiver certo e eu disser "pode".
Pronto quando: a rota nova responde em produção e a tag e o back-merge existem.
```

### Etapa 20: operar e anotar

```text
Etapa 20: operar e anotar.
Objetivo: a operação deixa de depender de memória.
Entregáveis: alarmes (tarefa caindo, respostas 5xx, banco); logs centralizados com retenção definida; backups do banco conferidos, com um teste de restauração; alerta de custo; runbook (ligar e desligar, deploy falhou, rollback, banco fora, rotação de segredos); varredura de dependências e da imagem na esteira.
Restrições: itens de console são meus: entregue o roteiro e não crie recursos na nuvem.
Pronto quando: o runbook permite a outra pessoa operar sem a conversa, e o próximo release sai sem surpresa.
```

---

## Prompts de apoio

### Fechar a etapa

```text
Antes de encerrar esta etapa: mostre a saída dos testes (suíte inteira, com os segredos fora do ambiente), mostre a sabotagem que você fez e a restauração, rode `git status` e `git diff --stat`, confirme que não há segredo nos arquivos alterados, e atualize PassoAPasso.md, memoria.md e (se envolver a nuvem) Duvida-CICD.md. Não faça commit, push, merge nem deploy: me peça.
```

### Retomar uma sessão

```text
Retome o projeto: leia CLAUDE.md (ou AGENTS.md), memoria.md e a seção "Próximos passos" do PassoAPasso.md. Diga em que etapa da Colinha-Nova-API.md estamos, o que está pendente, e qual é o próximo passo. Não altere nada ainda.
```

### Diagnosticar uma falha

```text
Algo falhou: <descreva o sintoma e cole a mensagem, sem segredos>. Diagnostique SEM alterar nada: liste as hipóteses em ordem de probabilidade, o que cada uma prevê e o comando de leitura que a confirma ou descarta. Depois de eu escolher, proponha a correção com o teste que a prova e a sabotagem.
```

### Revisão crítica do que foi feito

```text
Revise criticamente a etapa <N> como um QA experiente: o que ainda pode quebrar, que caso não foi testado, que regra do prompt de abertura foi cumprida só na aparência. Liste os achados por severidade, com como reproduzir, e NÃO corrija nada sem eu escolher.
```
