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

## Próximos passos

- Commitar o que está pendente (camadas, tratamento de erros, endpoints, `.md`).
- Rodar a aplicação de verdade e chamar os endpoints (curl ou Postman).
- Banco de dados e `repository`, no lugar do armazenamento em memória.
