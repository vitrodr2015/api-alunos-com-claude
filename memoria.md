# Memória do projeto

Arquivo para retomar o contexto se a conversa for perdida. Ler junto com `PassoAPasso.md`.

## Como trabalhamos

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

## Pendências

- Decidir o destino do `HELP.md`.
- Commitar estrutura de camadas, tratamento de erros e os 7 endpoints (hoje só locais).
- Escolher MapStruct ou Dozer, se o usuário quiser trocar o mapper manual.
- Rodar a aplicação de verdade e chamar os endpoints.
- `.github/` aparece como ignorado no `git status --ignored`, sem regra no `.gitignore` do repositório. Investigar antes de criar a esteira de CI.
- Erros que faltam: 401/403 em JSON (entram com a segurança).

## Estado

- Último commit: `69152c9` – Configurações iniciais (já enviado ao GitHub).
- Sem commit: estrutura de camadas, pacote `exception`, os 7 endpoints (model, dto, mapper, service, controller), `pom.xml` com `spring-boot-starter-validation`, testes, `PassoAPasso.md`, `memoria.md`.
- `.\mvnw test`: 34 testes passando.
