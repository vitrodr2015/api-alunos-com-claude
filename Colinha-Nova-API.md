# Colinha: como criar uma nova API

A ordem em que eu faria a próxima API, com o que aprendemos no projeto de alunos. Cada etapa tem duas linhas: o que fazer e quando está pronta. O "como fazer" está no `Playbook.md` (fase indicada entre parênteses).

Regra de uso: só passe para a etapa seguinte quando o "pronto quando" estiver **provado** (teste passando ou resposta real, não "acho que funciona").

---

## Antes de começar

1. Quem usa a API e quem pode fazer o quê?
2. Haverá navegador chamando? (se não, CORS fica vazio)
3. Quais formatos? (JSON basta, na maioria dos casos)
4. Onde vai rodar e quanto pode custar?
5. Que dados pessoais ela guarda, e por quanto tempo?
6. Dados de exemplo podem ir para produção?

---

## 1. Fundação

**0. Regras de trabalho** (Fase 0)
Anotações vivas, repositório sem segredo, um passo por vez, Git Flow.
Pronto quando: as regras estão escritas num arquivo de memória do projeto.

**1. Projeto, repositório e CI** (Fases 1 e 7)
Criar o projeto com versões casadas (JDK, `pom.xml`, Dockerfile e esteira). Montar o workflow de build e testes, **rodar uma vez** e só então ativar a proteção das branches exigindo o check.
Pronto quando: um PR com teste quebrado fica bloqueado.

**2. Contrato da API** (Fase 2)
Rotas, campos, regras e códigos de erro, em tabela, antes do código.
Pronto quando: o contrato está num arquivo e foi revisado.

**3. Configuração e segredos** (Fases 3 e 8)
Tudo por variável de ambiente; segredo sem valor padrão; mensagem clara se faltar; primeiro teste de higiene que proíbe segredo no arquivo.
Pronto quando: sem a variável a aplicação para com texto claro.

---

## 2. Dados e núcleo da API

**4. Banco, migrations e entidade** (Fase 3)
Banco local em Docker, migrations versionadas, entidade e repository, `ddl-auto: validate`.
Pronto quando: trocar o nome de uma coluna na entidade faz a subida falhar.

**5. Camadas e erros padronizados** (Fase 1)
Pacotes por camada, um único formato de erro e um tratador global.
Pronto quando: 400, 404, 409, 415 e 500 saem no mesmo JSON.

**6. CRUD em fatia vertical** (Fases 2 e 10)
Controller, service, DTOs, mapper e validação **estrita** (inclusive datas), com Swagger desde o primeiro endpoint. Testes unitário e web.
Pronto quando: cada endpoint cobre o caso feliz e os erros 400, 404 e 409; `2000-02-30` dá 400.

**7. Testes com banco real** (Fase 9)
Testcontainers e REST Assured, com classe base e um contêiner único.
Pronto quando: o fluxo completo roda contra o banco real, com as migrations de verdade.

**8. Listagem** (Fases 4 e 11)
Paginação com limites, filtros e ordenação **só por campos permitidos**.
Pronto quando: tamanho gigante é reduzido e `sort` estranho dá 400.

**9. Formato da resposta** (Fase 2)
JSON como padrão, com ordem dos campos e datas padronizadas. HAL ou XML só se alguém pedir.
Pronto quando: o texto exato do JSON está num teste.

---

## 3. Segurança

**10. Autenticação** (Fase 8)
Usuários e permissões, hash BCrypt, login com a mesma resposta para qualquer falha, JWT (acesso curto e refresh), filtro, 401 e 403 em JSON.
Pronto quando: o login entra, e token vencido, adulterado ou do tipo errado dá 401.

**11. Autorização e CORS** (Fase 8)
Regra por rota com `denyAll` no fim; CORS vazio por padrão.
Pronto quando: rota nova nasce fechada; anônimo recebe 401 e sem permissão recebe 403.

**12. Admin inicial e endurecimento** (Fase 8)
Primeiro admin por variável usada uma vez; testes com os filtros ligados; guarda que proíbe segredo em `environment`, usuário semeado e `*` no CORS; caminhos sensíveis negados.
Pronto quando: quebrar cada regra de propósito faz um teste falhar.

**13. Limites de abuso e ciclo de vida do token**
Limite de tentativas de login; logout e revogação; troca de senha.
Pronto quando: tentativas em excesso recebem 429 e um token revogado deixa de valer.

---

## 4. Operação

**14. Saúde e falha de infraestrutura** (Fases 10 e 11)
`health` público (o *liveness* para o orquestrador); banco fora do ar vira 503 rápido, com `Retry-After`; logs sem segredo e sem dado pessoal.
Pronto quando: derrubar o banco de verdade devolve 503 em segundos, e a API volta sozinha.

**15. Contêiner** (Fases 5 e 7)
Dockerfile em duas fases, usuário que não é root; a esteira passa a construir a imagem.
Pronto quando: a imagem roda local e responde.

**16. QA da imagem de produção** (Fase 11)
Testar a imagem com banco real descartável: autorização, tokens, injeção, limites, concorrência, queda do banco, logs, reinício. Corrigir e **refazer**.
Pronto quando: nenhuma resposta 5xx para entrada inválida e nenhum segredo no log.

---

## 5. Entrega

**17. Nuvem** (Fase 6)
Banco, registro de imagens, usuário da esteira, cluster, **todos os segredos** (inclusive JWT e senha do admin inicial) com a política na role certa, firewall, definição da tarefa, serviço. Usuário de banco com privilégio mínimo; **HTTPS e domínio**.
Pronto quando: a API responde por HTTPS e o banco não é acessado como `root`.

**18. Esteira de deploy** (Fase 7)
Deploy só em push na `main`.
Pronto quando: o merge na `main` troca a tarefa e a rota nova responde.

**19. Release** (Fase 12)
Banco `Available`, testes verdes, merge, **verificar pelo comportamento**, tag, back-merge.
Pronto quando: a rota nova responde em produção.

**20. Operar e anotar** (Fase 12)
Alarmes, backups conferidos, custo, rollback, e o que deu errado com a solução.
Pronto quando: o próximo release sai sem surpresa.

---

## O que dá para pular

| Projeto | Faça | Pode pular |
| --- | --- | --- |
| Protótipo local | 0 a 6 e 15 | nuvem, esteira, segurança forte, QA |
| API interna | 0 a 12, 14, 15 e 18 | Swagger público, limites de abuso, QA completo |
| Produção com usuários reais | todas | nada |

---

## Dez regras de ouro

1. Prova antes de "feito": teste passando e resposta real.
2. Sabote de propósito para confirmar que o teste pega a falha.
3. Segredo nunca em arquivo versionado, e na nuvem sempre em `secrets`.
4. Negar por padrão: rota nova nasce fechada.
5. O banco manda, não o token: permissões são relidas a cada requisição.
6. Mesma resposta, e tempo parecido, para qualquer falha de login.
7. Banco fora do ar é 503, rápido, nunca 401 nem 500.
8. Nunca edite migration já aplicada; corrija com uma nova.
9. Confirme o banco `Available` antes de mesclar na `main` e verifique a versão pelo comportamento depois (um rollback deixa o código antigo no ar).
10. Anote enquanto faz: o que deu errado e como se resolveu é a parte mais valiosa.

---

## Levar para o próximo projeto

Arquivos-modelo: `Dockerfile`, `docker-compose.yml`, workflow da esteira, `task-definition.json` (sem senha), `application.yml` (só variáveis), `AbstractIntegrationTest`, `ConfigHygieneTests`, `SecurityConfig` e o tratador global de erros. Mais os três arquivos de anotação, vazios, mantidos vivos desde o primeiro dia.

Ainda não feito no projeto de alunos: etapas 13, HTTPS, usuário de banco mínimo, observabilidade e alarmes, varredura de dependências e da imagem, e os scripts de QA guardados no repositório.
