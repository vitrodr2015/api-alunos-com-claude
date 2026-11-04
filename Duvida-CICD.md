# Duvida - CICD

# 🔧 Dúvidas e atualizações do tutorial: Deploy na AWS (RDS + ECR + ECS Fargate)

> 💡 **Objetivo:** registrar o que está **diferente** do `AWS-Esteira CICD.md` no console atual da AWS, na mesma ordem das seções do tutorial. Aqui entra **somente o que já foi executado**. No fim, serve de base para um tutorial novo e corrigido.

> 💡 *Como usar:* a cada seção do tutorial que for executada, acrescente aqui a seção com o mesmo número e título, com o passo a passo como está no console hoje. Dúvidas sem resposta vão em **Dúvidas abertas**.

> 🔓 **Ponto de atenção: repositório público.** Este repositório é público (projeto de estudo). Antes de colar qualquer valor aqui, troque por um marcador: seu IP de casa por `<seu-IP>/32`, IDs de security group por `sg-...`, o IP da tarefa por `<IP-publico>`. **Nunca** coloque senha, Access Key, Secret Access Key ou token. O ID da conta AWS, os ARNs e o endpoint do RDS não são credenciais, mas ficam visíveis no `task-definition.json` (a esteira precisa deles) e no histórico do Git, que não some ao editar os arquivos. Por isso a senha do RDS tem de ser forte, e o **Public access** do RDS só deve ficar ligado enquanto for preciso usar o computador local.

---

## Valores usados (nossos, diferentes do tutorial)

| **Item** | **Tutorial** | **Nosso** |
| --- | --- | --- |
| **Banco RDS** | `vitrodr-db-server` | `database-crud-alunos` |
| **Usuário do banco** | `root` | `root` (aceito pelo console) |
| **Database (schema) dentro do servidor** | `rest_udemy` | `crud_alunos` (criado pela aplicação, ver seção 1) |
| **Repositório ECR** | `rest-with-claude` | `respositorio-crud-alunos` (nome com erro de digitação, ver Dúvidas abertas) |
| **Usuário IAM da esteira** | `GITHUB_ACTIONS` | `GITHUB_ACTIONS_CRUD_ALUNOS` |
| **Cluster ECS** | `rest-with-claude-spring` | `cluster-crud-alunos` |
| **Secret da senha do banco** | `rest-with-claude/db-password` | `secrets_crud_alunos/db-password` |
| **Política de leitura do secret** | `LerSecretDB` | `LerSecretDB_CrudAlunos` |
| **Security group da aplicação** | `rest-with-claude-app-sg` | `security-group-crud-alunos` |
| **Task Definition (family)** | `rest-with-claude-spring-taskdefinition` | `crud-alunos-task-definition` (`:1` manual; `:2` em diante criadas pela esteira) |
| **Contêiner** | `Container-vitrodr` | `container-crudalunos` |
| **Log group** | `/ecs/rest-with-claude-spring-taskdefinition` | `/ecs/crud-alunos-task-definition` |
| **Role de execução da task** | `ECS-role-bd3e720b` ou `ecsTaskExecutionRole` | `ecsTaskExecutionRole` |
| **Service (ECS)** | `rest-with-claude-spring-taskdefinition-service` | `service-crud-alunos` |
| **Perfil da AWS CLI** | (não usa; `aws configure` direto) | `crud-alunos` |
| **Imagem local** | `rest-with-claude` | `crud-alunos-local` |
| **Classe da instância** | `db.t4g.micro` | `db.t4g.micro` |
| **Workflow da esteira** | `.github/workflows/continuous-deployment.yml` | mesmo nome de arquivo, conteúdo adaptado (ver seção 11) |
| **Imagem gerada pelo `docker compose`** | (não informada) | `vitrodr/crud-alunos` (`apicrud-alunos/docker-compose.yml`) |

## Andamento

| **Seção** | **Situação** |
| --- | --- |
| 1. Criar o banco MySQL (RDS) | ✅ Feita (banco **Available**) |
| 2. Criar o repositório de imagens (ECR) | ✅ Feita (repositório criado) |
| 3. Criar os usuários de acesso (IAM) | ✅ Feita (usuário e chave de acesso criados) |
| 4. Criar o cluster (ECS) | ✅ Feita (cluster criado) |
| 5.1 Criar o secret (Secrets Manager) | ✅ Feita (secret criado) |
| 5.2 Permitir a leitura do secret | ✅ Feita (política `LerSecretDB_CrudAlunos` criada) |
| 6.1 Criar o security group da aplicação | ✅ Feita (`security-group-crud-alunos`) |
| 6.2 Liberar a aplicação no banco | ✅ Feita (regra 3306 salva no grupo `default`) |
| 7.1 Criar a Task Definition | ✅ Feita e conferida pelo JSON (`crud-alunos-task-definition:1`) |
| 7.2 Exportar o JSON para a esteira | ✅ Feita (`apicrud-alunos/task-definition.json`) |
| 8. Enviar a primeira imagem para o ECR | ✅ Feita (imagem `latest` enviada para `respositorio-crud-alunos`) |
| 9. Criar o Service (ECS) | ✅ Feita (`service-crud-alunos`, tarefa **Running** após corrigir a permissão do secret) |
| 10. Testar a aplicação | ✅ Feita (API respondendo pelo IP público, testada no Postman) |
| 11. Ligar a esteira (GitHub Actions) | ✅ Feita (primeiro deploy: Task Definition `:2`; release 1.1.0 com o banco: `:6`, ver seção 11) |
| 15.1 Ligar o ambiente | ✅ Feita em 2026-11-01 e em 2026-11-02, duas vezes (antes das releases 1.1.0 e 1.2.0). **Ambiente ligado agora, desligar pendente.** |
| 15.2 Desligar o ambiente | ✅ Feita em 2026-10-30 e de novo em 2026-11-02 (Desired tasks = 0 e RDS `Stopped temporarily`) |
| Segurança 1/2: segredo `JWT_SECRET` (Secrets Manager `secrets_crud_alunos/jwt-secret`, política `LerSecretDB_CrudAlunos` da `ecsTaskExecutionRole` com o ARN novo, `JWT_SECRET` em `secrets` do `task-definition.json`) | ✅ Feita em 2026-11-03 pelo usuário no console (segredo criado e política salva); o `task-definition.json` foi atualizado no repositório, mas **ainda não implantado** (só entra na release) |
| Segurança 2/2: segredo da senha do primeiro admin (`ADMIN_BOOTSTRAP_PASSWORD`, só no 1º deploy) | ⏳ **Ainda não executada** (roteiro no `PassoAPasso.md`, passo 39) |

---

## 1. Criar o banco MySQL (RDS)

**Passo a passo como está no console hoje** (validado; o banco foi criado):

1. Busque **Aurora and RDS** → **Databases** → **Create database**.
2. Em **Choose a database creation method**, escolha **Full configuration**. *(Antes: "Standard create". Hoje o padrão vem em **Easy create**.)*
3. Em **Engine options**, clique em **MySQL**. Deixe a versão sugerida (MySQL Community 8.4.11). *(O engine que vem selecionado é **Aurora**: não use.)*
4. Em **Templates**, escolha **Sandbox**. *(Antes: "Free tier". As opções agora são Production, Dev/Test e Sandbox.)*
5. Em **Availability and durability**, escolha **Single-AZ DB instance deployment (1 instance)**. *(Seção nova. As outras duas opções criam réplicas e multiplicam o custo.)*
6. Em **Settings**:
    - **DB instance identifier:** o nome do banco (tutorial: `vitrodr-db-server`; o nosso: `database-crud-alunos`).
    - **Master username:** `root`.
    - **Credentials management:** **Self managed**. *(A opção "Managed in AWS Secrets Manager" vem primeiro e tem custo extra.)*
    - **Master password:** crie uma senha forte e anote fora do Git.
7. Em **Instance configuration**, marque o filtro **Burstable classes (includes t classes)** e escolha **`db.t4g.micro`**. *(O padrão é `db.m7g.large`.)*
8. Em **Storage**, deixe **gp3** e troque **Allocated storage** de 200 para **20** GiB.
9. Em **Connectivity**:
    - **Compute resource:** **Don't connect to an EC2 compute resource**. *(Opção nova.)*
    - **Public access:** **Yes**.
    - **VPC security group:** **Choose existing** e o grupo **default**.
    - **RDS Proxy:** deixe desmarcado (custo extra).
10. Em **Monitoring**, escolha **Database Insights - Standard** (grátis, 7 dias). Deixe **Enhanced Monitoring** e **Log exports** desmarcados.
11. Role até o fim e abra a seção recolhida **Additional configuration** (a que lista "Database options, encryption turned on, backup turned on..."). Em **Initial database name**, digite o nome do database. *(No nosso caso o database **não foi criado** neste passo: ele é criado pela própria aplicação na primeira conexão, com `createDatabaseIfNotExist=true` na `DB_URL`, e o nome escolhido foi `crud_alunos`. Ver seção 7.)*
12. Confira o **Estimated monthly costs** no fim da página e clique em **Create database**. Aguarde o **Status** ficar **Available**.
13. Clique no nome do banco → aba **Connectivity & security**. No painel **Connect using**, clique em **Endpoints** para ver o **Endpoint** e a porta `3306`. *(Ele também aparece dentro do comando `mysql -h <endpoint> -P 3306 -u root -p ...`. Não há mais o campo "Endpoint & port" em destaque.)*

> 💰 **Custo:** com os valores padrão (`db.m7g.large` e 200 GiB) a estimativa era de **145,64 USD/mês** (instância 122,64 + disco 23,00). O Aurora Dev/Test da primeira tela custa 0,276 USD **por hora**. Confira sempre o *Estimated monthly costs* antes de criar.

> ⚠️ Se tiver criado um cluster Aurora em tentativas anteriores, confira em **Databases** que não sobrou nenhum ligado.

> 🔒 O endpoint não é segredo por si só, mas **não o grave no código**: ele entra por variável de ambiente. A senha **nunca** vai para o Git nem para mensagens.

**Para o tutorial novo:** trocar "Standard create" por "Full configuration"; "Free tier" por "Sandbox"; incluir a escolha Single-AZ; mandar trocar o disco de 200 para 20 GiB; mostrar onde fica o endpoint e o "Initial database name"; incluir Compute resource, RDS Proxy e Database Insights; deixar claro que o nome do banco é livre.

---

## 2. Criar o repositório de imagens (ECR)

**Passo a passo executado** (nenhuma diferença de tela relatada em relação ao tutorial):

1. Busque **Elastic Container Registry** e clique.
2. No menu da esquerda, clique em **Repositories** → **Create repository**.
3. **Visibility settings:** **Private**.
4. **Repository name:** o nome do repositório (tutorial: `rest-with-claude`; o nosso: `respositorio-crud-alunos`).
5. Deixe o resto como está e clique em **Create repository**.
6. Anote o **URI**: `<id-da-conta>.dkr.ecr.us-east-1.amazonaws.com/<nome-do-repositório>`.

> ⚠️ O nome do repositório **não pode ser alterado** depois de criado. Ele é usado igual na Task Definition (seção 7), nos comandos `docker tag`/`docker push` (seção 8) e no workflow da esteira. Se o nome mudar, mude em todos esses lugares.

**Para o tutorial novo:** avisar que o nome do repositório é livre mas fixo, e listar os lugares onde ele reaparece.

---

## 3. Criar os usuários de acesso (IAM)

**Passo a passo executado** (validado; nenhuma diferença de tela relatada em relação ao tutorial):

> ⚠️ A esteira precisa de um usuário próprio, com poucas permissões. **Nunca** use o administrador na esteira.

### 3.1 Criar o usuário da esteira

1. Busque **IAM** e clique.
2. No menu da esquerda, clique em **Users** → **Create user**.
3. **User name:** o nome do usuário (tutorial: `GITHUB_ACTIONS`; o nosso: `GITHUB_ACTIONS_CRUD_ALUNOS`). **Não** marque "Provide user access to the AWS Management Console". Clique em **Next**.
4. Escolha **Attach policies directly**.
5. Na caixa de busca, procure e **marque** estas duas:
    - `AmazonEC2ContainerRegistryFullAccess`
    - `AmazonECS_FullAccess`
6. Clique em **Next** → **Create user**.

### 3.2 Gerar a chave de acesso

1. Na lista de usuários, clique no usuário criado.
2. Abra a aba **Security credentials**.
3. Role até **Access keys** e clique em **Create access key**.
4. Escolha **Application running outside AWS**, marque a caixa de confirmação e clique em **Next**.
5. (Opcional) Descreva como `github-actions` e clique em **Create access key**.
6. **Copie agora** a **Access key** e a **Secret access key** (clique em *Show*). A secret não aparece de novo. Guarde-as para a seção 11, onde são cadastradas no GitHub.

> 🔒 **Nunca** cole essas chaves no código, em mensagens ou em arquivos do projeto. Elas não estão registradas neste arquivo.

> 💡 O nome do usuário é livre. Só lembre dele na seção 14 (limpeza), onde as chaves sem uso devem ser apagadas.

**Para o tutorial novo:** as duas políticas são do tipo *FullAccess*, mais amplas do que o aviso de "poucas permissões" sugere; avaliar políticas mais restritas. A AWS também costuma recomendar alternativas a chaves de longa duração para o GitHub Actions; avaliar no tutorial novo se faz sentido.

---

## 4. Criar o cluster (ECS)

**Passo a passo executado** (validado; nenhuma diferença de tela relatada em relação ao tutorial):

1. Busque **Elastic Container Service** e clique.
2. No menu da esquerda, clique em **Clusters** → **Create cluster**.
3. **Cluster name:** o nome do cluster (tutorial: `rest-with-claude-spring`; o nosso: `cluster-crud-alunos`).
4. Em **Infrastructure**, deixe marcado **AWS Fargate (serverless)**.
5. (Opcional) Em **Monitoring**, o *Container Insights* dá métricas, mas aumenta o custo. Para estudo, deixe desligado.
6. Clique em **Create**. Aguarde a mensagem de sucesso.

> ⚠️ O nome do cluster aparece de novo nas seções 9 (criar o Service), 15 (ligar e desligar) e no workflow da esteira. Use sempre o nome real do cluster.

**Para o tutorial novo:** lembrar que o nome do cluster é livre e listar onde ele reaparece.

---

## 5. Guardar a senha do banco (Secrets Manager)

> A senha não pode ficar no código nem na imagem. A aplicação a recebe do Secrets Manager.

### 5.1 Criar o secret

**Passo a passo executado** (validado; nenhuma diferença de tela relatada em relação ao tutorial):

1. Busque **Secrets Manager** e clique.
2. Clique em **Store a new secret**.
3. **Secret type:** **Other type of secret**.
4. Na parte **Key/value pairs**, clique na aba **Plaintext** e **apague** o conteúdo. Digite **apenas a senha** do RDS (sem aspas, sem chaves, sem espaço ou quebra de linha no fim).
5. Clique em **Next**.
6. **Secret name:** o nome do secret (tutorial: `rest-with-claude/db-password`; o nosso: `secrets_crud_alunos/db-password`). Clique em **Next** duas vezes (deixe a rotação desligada) e depois em **Store**.
7. Abra o secret e anote o **Secret ARN**, que será usado na Task Definition (seção 7).

> 🔒 A senha **não** está registrada neste arquivo, nem deve estar em qualquer arquivo do projeto.

> ⚠️ O nome do secret reaparece no JSON da política de leitura (5.2) e no ARN de `DB_PASSWORD` na Task Definition (seção 7). Se o nome mudar, mude nos dois lugares.

### 5.2 Permitir que a aplicação leia o secret

**Passo a passo executado** (validado; nenhuma diferença de tela relatada em relação ao tutorial):

1. Busque **IAM** → **Roles**.
2. Na busca da lista, digite o nome da **Task execution role** (por exemplo `ecsTaskExecutionRole`) e clique nela.
3. Na aba **Permissions**, clique em **Add permissions** → **Create inline policy**.
4. Clique na aba **JSON**, apague tudo e cole (só a linha `Resource` muda em relação ao tutorial):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": "secretsmanager:GetSecretValue",
    "Resource": "arn:aws:secretsmanager:us-east-1:<id-da-conta>:secret:secrets_crud_alunos/db-password-*"
  }]
}
```

5. Clique em **Next**. Em **Policy name**, digite o nome da política (tutorial: `LerSecretDB`; o nosso: `LerSecretDB_CrudAlunos`). Clique em **Create policy**.

> ⚠️ Troque `<id-da-conta>` pelo ID da sua conta e mantenha o `-*` no final do `Resource`: a AWS acrescenta um sufixo aleatório ao ARN do secret.

> 💡 A política precisa estar na **Task execution role** (a que o ECS usa para buscar o secret antes de iniciar o contêiner), e não na *Task role*.

**Para o tutorial novo:** dizer que o nome do secret é livre e listar onde ele reaparece; avisar que não pode sobrar espaço ou quebra de linha na senha; deixar claro que a política vai na Task execution role e que só a linha `Resource` precisa ser trocada.

---

## 6. Criar o security group da aplicação

> O security group é o "firewall" que libera as portas.

### 6.1 Criar o grupo

**Passo a passo executado** (validado):

1. Busque **EC2** e clique.
2. No menu da esquerda, em **Network & Security**, clique em **Security Groups** → **Create security group**.
3. **Security group name:** o nome do grupo (tutorial: `rest-with-claude-app-sg`; o nosso: `security-group-crud-alunos`). Preencha também a **Description**.
4. **VPC:** deixe a **default**.
5. Em **Inbound rules**, clique em **Add rule**:
    - **Type:** Custom TCP
    - **Port range:** `8080`
    - **Source:** Anywhere-IPv4 (`0.0.0.0/0`) no tutorial; **My IP** é mais seguro enquanto a API não tiver autenticação.
6. Clique em **Create security group**.

> ⚠️ O nome do grupo reaparece no 6.2 e na seção 9 (criar o Service). Use o nome real.

### 6.2 Liberar a aplicação no banco

**Passo a passo executado** (validado):

1. Em **Security Groups**, ache o grupo **default** que o RDS usa (confira o ID: o do nosso banco é o `sg-...`, porque pode haver mais de um grupo chamado `default`) e clique nele.
2. Aba **Inbound rules** → **Edit inbound rules** → **Add rule**:
    - **Type:** MYSQL/Aurora (porta `3306`)
    - **Source:** **Custom**, depois **clique no grupo da aplicação na lista suspensa** (ou cole o ID `sg-...` dele). *(Só digitar o nome não funciona: o console mostra o erro "CIDR block, a security group ID or a prefix list has to be specified".)*
    - **Description:** opcional (usamos `Crud-Alunos`).
3. (Opcional) Acesso do seu computador (HeidiSQL, por exemplo): regra **MYSQL/Aurora** com **Source: My IP**. *(No nosso caso essa regra já existia, com a descrição `Vitor-casa`, e não foi preciso criar outra.)*
4. **Remova qualquer regra com origem `0.0.0.0/0` na porta 3306.** *(Não havia nenhuma.)*
5. Clique em **Save rules**. A mensagem esperada é "Inbound security group rules successfully modified".

Ao final, a porta 3306 do grupo `default` ficou com estas origens:

| **Origem** | **Para quê** |
| --- | --- |
| `security-group-crud-alunos` | a aplicação no ECS |
| `<seu-IP>/32` (`Vitor-casa`) | o computador do usuário |
| `rest-with-claude-app-sg` | sobra de uma tentativa anterior (ver Dúvidas abertas) |

> ⚠️ **Segurança:** Nunca deixe a porta 3306 aberta para `0.0.0.0/0`. *Seu IP muda?* Edite a regra e escolha **My IP** de novo (descubra o IP em `https://checkip.amazonaws.com`).

**Para o tutorial novo:** avisar que o campo *Source* só aceita o grupo se for escolhido na lista suspensa (ou colado como `sg-...`); avisar que o console pode já ter criado regras no `default` (IP do usuário); avisar que a 8080 aberta para o mundo expõe a API se ela não tiver autenticação.

---

## 7. Criar a Task Definition (ECS)

> A Task Definition descreve como o contêiner roda: imagem, porta, CPU, memória e variáveis.

### 7.1 Criar

**Passo a passo executado** (a tela de criação é um formulário único, na mesma ordem do tutorial, mas **muitos campos vêm com valores padrão que precisam ser trocados**):

1. Em **Elastic Container Service**, menu da esquerda: **Task definitions** → **Create new task definition**.
2. **Task definition family:** vem preenchida com um nome aleatório (ex.: `subtle-app-91r9n8`). Troque pelo nome desejado (tutorial: `rest-with-claude-spring-taskdefinition`; o nosso: `crud-alunos-task-definition`).
3. **Launch type:** **AWS Fargate**. *(A tela agora tem também **Managed Instances**.)* **OS/Architecture:** Linux/X86_64. **Network mode:** `awsvpc`.
4. **CPU:** `1 vCPU`. **Memory:** `3 GB` (já vêm assim).
5. **Task execution role:** escolher a role **existente** que recebeu a política de leitura do secret (5.2), e não "Create default role" nem "Create new role". **Task role:** pode ficar como está.
6. Em **Container - 1**:
    - **Name:** vem `Main`; troque (o nosso: `container-crudalunos`).
    - **Image URI:** vem vazio; digite a URI do repositório com a tag, ex.: `<id-da-conta>.dkr.ecr.us-east-1.amazonaws.com/respositorio-crud-alunos:latest`. *(Há o botão **Browse ECR images**.)*
    - **Container port:** vem `80`; troque para `8080`. **Protocol:** TCP. **Port name** e **App protocol (HTTP):** pode deixar.
7. Em **Environment variables** (*Add individually*), **Add environment variable**:
    - `DB_USER` | **Value** | `root`
    - `TZ` | **Value** | `America/Sao_Paulo`
    - `DB_PASSWORD` | **ValueFrom** | o **Secret ARN completo** do secret (com o sufixo aleatório no final)
    - `DB_URL` | **Value** | `jdbc:mysql://<endpoint-do-banco>:3306/crud_alunos?createDatabaseIfNotExist=true` *(variável **a mais** que o tutorial não tem)*
8. Em **Logging**, deixe **Use log collection** marcado, destino Amazon CloudWatch. Confira que `awslogs-group` ficou `/ecs/<nome-da-family>`, `awslogs-region` = `us-east-1`, `awslogs-stream-prefix` = `ecs` e `awslogs-create-group` = `true`.
9. Clique em **Create**.

> ⚠️ **`DB_PASSWORD` precisa ser `ValueFrom`.** Com o tipo `Value`, o ARN seria entregue à aplicação como se fosse a senha, e a conexão com o banco falharia.

> 💡 A imagem não precisa existir no ECR para criar a Task Definition; ela só é necessária quando a tarefa subir (seção 8).

> ⚠️ Se, mais tarde, a tarefa falhar com `AccessDenied` ao criar o log group, faltou a permissão `logs:CreateLogGroup` na role de execução: dê a permissão ou crie o log group à mão em **CloudWatch → Log groups**.

**Para o tutorial novo:** dizer que o formulário traz valores aleatórios/padrão (family, nome do contêiner, porta 80); acrescentar a variável `DB_URL` (o tutorial supõe que o endereço do banco já está no código); avisar para escolher a role de execução existente; avisar do `ValueFrom`; avisar que o database pode ser criado pela aplicação (`createDatabaseIfNotExist=true`).

### 7.2 Exportar o JSON (para a esteira)

**Diferença do tutorial:** o tutorial diz que o `task-definition.json` "já existe no projeto". No nosso projeto **não existia**, então foi criado.

**Passo a passo executado:**

1. Abrir a Task Definition no ECS → revisão desejada → aba **JSON** (ou usar `aws ecs describe-task-definition`).
2. Salvar o conteúdo como `task-definition.json` dentro do projeto (o nosso: `apicrud-alunos/task-definition.json`).
3. Remover os campos que a AWS gera sozinha e que não podem ir ao registrar uma revisão: `taskDefinitionArn`, `revision`, `status`, `requiresAttributes`, `compatibilities`, `registeredAt` e `registeredBy`.
4. Na `image`, acrescentar a tag `:latest` (a esteira troca a imagem a cada deploy).

> ⚠️ O arquivo guarda valores da conta: ID da conta, ARN da role de execução, ARN do secret e o endereço do banco (`DB_URL`). Não há senha. Se recriar a role, o secret ou o contêiner, atualize o arquivo, senão a esteira registra uma Task Definition com valores antigos. Se o repositório for público, avalie se quer esses valores expostos.

**Para o tutorial novo:** explicar como criar o arquivo quando ele ainda não existe e quais campos remover.

---

## 8. Enviar a primeira imagem para o ECR

> O ECS precisa de uma imagem para iniciar. Você pode enviar a primeira manualmente ou deixar a esteira (seção 11) fazer isso.

**Passo a passo executado** (validado; `Login Succeeded` e o push terminou com `latest: digest: sha256:...`):

1. Confirme a AWS CLI: `aws --version`. *(Já estava instalada, versão 2.38.4. Atenção: é `--version`; `--verison` dá o erro "required: command".)*
2. Crie um **perfil próprio** com a chave do usuário da esteira (o tutorial manda rodar `aws configure` sem perfil):
    ```bash
    aws configure --profile crud-alunos
    ```
    Campos, nesta ordem: **AWS Access Key ID** (o código `AKIA...`), **AWS Secret Access Key** (a chave longa de 40 caracteres), região `us-east-1` e formato `json`. Confirme com `aws configure list-profiles`.
3. Gere a imagem, **na pasta do projeto** (onde está o `Dockerfile`):
    ```bash
    docker build -t crud-alunos-local .
    ```
    *(Já feito antes, ao testar o `Dockerfile` localmente.)*
4. Faça o login no ECR e **espere a resposta `Login Succeeded`** antes do próximo comando:
    ```bash
    aws ecr get-login-password --region us-east-1 --profile crud-alunos | docker login --username AWS --password-stdin <id-da-conta>.dkr.ecr.us-east-1.amazonaws.com
    ```
5. Marque e envie a imagem com o nome do repositório do ECR:
    ```bash
    docker tag crud-alunos-local:latest <id-da-conta>.dkr.ecr.us-east-1.amazonaws.com/respositorio-crud-alunos:latest
    docker push <id-da-conta>.dkr.ecr.us-east-1.amazonaws.com/respositorio-crud-alunos:latest
    ```
6. No console do **ECR**, abra o repositório e confirme que a imagem `latest` aparece na lista. *(Confirmado: a lista mostra **3 itens** para um único envio: o **Image Index** com a tag `latest`, a **Image** Linux x86_64 e um item **Other** de 0 MB, que é o atestado de proveniência que o Docker mais novo acrescenta. É normal. O tamanho aparece comprimido: ~139 MB no ECR contra 521 MB da imagem local.)*

> ⚠️ **Rode os comandos um de cada vez.** Colados juntos, o `docker push` roda mesmo que o login tenha falhado e usa uma sessão antiga já expirada.

> 🔒 No `aws configure`, o **nome do usuário** (`GITHUB_ACTIONS_CRUD_ALUNOS`) **não é digitado**: o que identifica o usuário é a Access Key ID. A Secret Access Key só aparece na criação da chave; se for perdida, crie um par novo e apague o antigo.

**Para o tutorial novo:** avisar que o ECR passa a listar 3 itens por envio (índice, imagem e atestado), sem problema; explicar o `--profile` e a ordem dos campos do `aws configure`; avisar para não colar todos os comandos de uma vez; avisar que o `docker build` pode ser pulado se a imagem já foi gerada; mostrar a mensagem de sucesso esperada.

---

## 9. Criar o Service (ECS)

> O Service mantém a aplicação rodando e a reinicia se cair.

**Passo a passo executado** (validado; a tarefa chegou a **Running**):

1. Em **Elastic Container Service** → **Clusters** → clique no cluster → aba **Services** → **Create**.
2. **Task definition family:** a da Task Definition (o nosso: `crud-alunos-task-definition`); **Revision:** em branco ou a mais recente.
3. **Service name:** o nome do Service (tutorial: `rest-with-claude-spring-taskdefinition-service`; o nosso: `service-crud-alunos`). *(Vem vazio; anote o nome, ele é fixo no workflow.)*
4. **Compute options:** **Capacity provider strategy** → **Use custom (Advanced)** → `FARGATE`, Base `0`, Weight `1`. *(O cluster não tem estratégia padrão configurada.)*
5. **Desired tasks:** `1`. Deixe o restante de **Deployment configuration** como está.
6. Abra **Networking** *(vem recolhido e é onde estão os campos principais)*:
    - **VPC:** a default.
    - **Subnets:** a lista traz as 8 subnets da VPC, **incluindo duas de Local Zone** (`us-east-1-atl-2a` e `us-east-1-atl-2b`). **Remova as duas** e deixe só as zonas `us-east-1a` a `us-east-1f`. O Fargate não funciona em Local Zone.
    - **Security group:** **Use an existing security group**. *(Vem preenchido com o `default`, que é o grupo do banco.)* Remova o `default` e deixe só o grupo da aplicação (o nosso: `security-group-crud-alunos`).
    - **Public IP:** **Turned on**.
7. Deixe as seções opcionais como estão (Service Connect, Service discovery, Gateway, Load balancing, VPC Lattice, Auto scaling, Volume, Troubleshooting). *(Gateway e VPC Lattice são novas; VPC Lattice tem custo.)*
8. Clique em **Create**. O Service fica **Active** com "0/1 Tasks running" por alguns minutos.
9. Acompanhe em **Cluster → aba Tasks**, com o filtro em **Any desired status**: o **Last status** deve virar **Running**. O **Public IP** aparece na linha da tarefa (e muda a cada nova tarefa).

> 🔴 **Erro encontrado na primeira tentativa:** as tarefas pararam em sequência com
> `ResourceInitializationError: ... AccessDeniedException: ... ecsTaskExecutionRole ... is not authorized to perform: secretsmanager:GetSecretValue ... because no identity-based policy allows the secretsmanager:GetSecretValue action`.
> **Causa:** a política de leitura do secret (seção 5.2) tinha sido criada em **outra role**, e não na `ecsTaskExecutionRole`, que é a role de execução usada pela Task Definition.
> **Solução:** em **IAM → Roles → `ecsTaskExecutionRole` → Permissions → Add permissions → Create inline policy**, colar a política com `secretsmanager:GetSecretValue` para o secret (ver 5.2), e depois, em **ECS → Services → Update**, marcar **Force new deployment**. A tarefa nova subiu em cerca de 2 minutos.

> 💰 Com 1 tarefa de 1 vCPU e 3 GB ligada, o custo é de cerca de US$ 1,30 por dia. Para pausar, coloque **Desired tasks = 0** (seção 15). O RDS também cobra enquanto está ligado.

**Para o tutorial novo:** avisar que o formulário traz o `default` pré-selecionado e as duas Local Zones na lista de subnets; explicar que a política da seção 5.2 tem de ir na role que a Task Definition usa como *Task execution role* (conferir o nome em Task definition → JSON → `executionRoleArn`); mostrar como ler o **Stopped reason** na aba Tasks.

---

## 10. Testar a aplicação

**Passo a passo executado** (validado no Postman):

1. No cluster, aba **Tasks**, ache a tarefa com **Last status = Running** e copie o **Public IP** (coluna da tabela, ou dentro do ID da tarefa → **Configuration**). *(O IP muda a cada tarefa nova.)*
2. No Postman (ou no navegador), chame `http://<IP-público>:8080/alunos`. A resposta é a lista dos alunos.
3. No Postman, guarde o endereço numa variável (`base_url`; no nosso ambiente é a `baseUrl`) e troque só o valor quando o IP mudar. Cuidado para não deixar barra dupla (`//alunos`).

**Diferenças do tutorial** (o tutorial foi escrito para outro projeto):

- O tutorial manda abrir `/swagger-ui/index.html`. **O nosso projeto não tem Swagger**; o teste é direto em `/alunos`.
- O tutorial espera **401** em `/` e manda fazer login em `/auth/signin` com um `accessToken`. **O nosso projeto não tem autenticação ainda**, então as chamadas funcionam sem token. Em `/`, a nossa API devolve 404 (não existe rota nessa raiz).
- O tutorial testa `/person`; o nosso recurso é `/alunos`.

> 🔒 Sem autenticação, quem souber o IP e a porta 8080 liberada consegue criar e apagar alunos. Para estudo, deixe a porta 8080 do security group aberta só para **My IP** (se estiver em `0.0.0.0/0`, restrinja).

**Para o tutorial novo:** deixar claro que o teste depende de a aplicação ter Swagger e login; indicar uma alternativa para projetos sem eles (chamar um endpoint simples como `GET /alunos`).

---

## 11. Ligar a esteira (GitHub Actions)

**Passo a passo executado** (a esteira rodou e o deploy chegou ao ECS):

1. Repositório no GitHub → **Settings** → **Secrets and variables** → **Actions** → aba **Secrets** → **New repository secret**. Crie `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` com a chave do usuário `GITHUB_ACTIONS_CRUD_ALUNOS` (seção 3.2). Os nomes têm de ser exatamente esses e ficar em **Secrets**, não em *Variables*.
2. O workflow fica em `.github/workflows/continuous-deployment.yml`. Neste projeto ele foi montado à parte e adaptado, e faz, nesta ordem:
    - `actions/checkout@v4`.
    - **JDK 25** (Temurin, com cache do Maven). A versão tem de ser igual à do `pom.xml` e à do `Dockerfile`.
    - `mvn -B verify` na pasta `apicrud-alunos`: compila e roda os testes. Se um teste falhar, nada é publicado.
    - `docker compose build` na pasta `apicrud-alunos`: gera a imagem `vitrodr/crud-alunos`, nome definido no `docker-compose.yml`.
    - (Publicação no Docker Hub: opcional, deixada comentada no arquivo.)
    - Só em **push** na `main` (PR não faz deploy e não acessa os secrets): login na AWS, login no ECR, `docker tag` e `docker push` para `respositorio-crud-alunos`, com a tag do **commit** (`github.sha`) e também `latest`.
    - `amazon-ecs-render-task-definition`: pega `apicrud-alunos/task-definition.json` (seção 7.2), troca a imagem do contêiner `container-crudalunos` pela do commit e gera a nova definição.
    - `amazon-ecs-deploy-task-definition`: registra a nova revisão e atualiza o Service `service-crud-alunos` no cluster `cluster-crud-alunos`, com `wait-for-service-stability: true`.
3. Regra `paths-ignore: '**.md'`: push que muda **só** arquivos `.md` não dispara a esteira.
4. Faça `git push` na `main` e acompanhe na aba **Actions** do GitHub.

**Resultado conferido no ECS:** a Task Definition passou para `crud-alunos-task-definition:2`, com a imagem `respositorio-crud-alunos:<hash do commit>` (a tag é o hash do commit `8fdf837`), as 4 variáveis (`DB_USER`, `TZ`, `DB_URL` e `DB_PASSWORD` como `valueFrom`) e a `ecsTaskExecutionRole`. O aluno 6 entrou na API.

> ⚠️ **Antes do push**, deixe o ambiente ligado (seção 15.1). A esteira **não altera** *Desired tasks*: com `0` ela termina verde, mas nenhuma aplicação sobe.

> ⚠️ A cada deploy a tarefa é trocada e o **IP público muda**.

> 💡 A pasta `.github/` chegou a aparecer como ignorada no `git status --ignored`. Hoje o workflow está versionado (`git ls-files` lista o arquivo) e `git check-ignore -v .github` não devolve regra nenhuma, então não foi preciso mexer no `.gitignore`.

**Atualizações depois da primeira versão da esteira (2026-11-01):**

- **Permissões:** o workflow ganhou `permissions: contents: read` (menor privilégio). O deploy não usa o `GITHUB_TOKEN`, e sim as chaves da AWS, então nada quebrou (deploy `success`).
- **Gatilhos com Git Flow:** `push` só na `main` (build, testes e deploy) e `pull_request` para `main`, `develop` e `release/**` (só build e testes, sem deploy). O check `build` é obrigatório nos rulesets dessas branches. Push em `develop` não dispara deploy de propósito: os passos de deploy usam `if: github.event_name == 'push'`.
- **Efeito do código na AWS:** a Task Definition já tem `DB_URL`, `DB_USER` e `DB_PASSWORD` (esta via Secrets Manager) com os mesmos nomes que o `application.yml` passou a ler, então não precisa mudar na AWS. Na primeira release com o banco, o Flyway cria a tabela `aluno` no database `crud_alunos` do RDS e aplica a `V2` (insere os 8 alunos iniciais); o RDS precisa estar `Available`. **Atenção:** a partir do passo 27 existe também a `V3` (400 alunos de exemplo para testar a paginação); a próxima release a aplicaria no RDS, então decida antes se esses dados de exemplo devem ir para a AWS. Depois disso os dados passam a ficar no RDS e **sobrevivem** a trocar a tarefa; o 1º deploy não repete a carga nos seguintes (o Flyway guarda a versão aplicada).

**Resultado do primeiro deploy com o banco (release 1.1.0, 2026-11-02):** com o RDS `Available` e o Service em 1, a esteira terminou com `success` e a tarefa subiu na `crud-alunos-task-definition:6`. A aplicação conectou no RDS com a senha vinda do Secrets Manager, o Flyway criou a tabela `aluno` e aplicou a `V2`, e `GET /alunos` devolveu os 8 alunos pelo IP público. Nada precisou mudar na Task Definition nem nas permissões. Dá para ver a tarefa e o IP sem abrir o console: `aws ecs describe-services` e `aws ecs describe-tasks` (perfil `crud-alunos`, que lê ECS e EC2).

**Segundo deploy com o banco (release 1.2.0, 2026-11-02):** com o RDS `Available` e o Service em 1, a esteira terminou com `success` e a tarefa subiu na `crud-alunos-task-definition:7`. Foi o primeiro deploy a aplicar uma migration sobre um banco que já tinha dados (`V3`, 400 linhas em cima das 8 da `V2`), e o Flyway fez isso sem intervenção: 408 alunos no RDS. Nada precisou mudar na Task Definition nem nas permissões. O contrato da API mudou nessa release (HAL e paginação novas); o teste pelo IP público (`GET /alunos`) agora lê `_embedded.alunos` e `page`, e `Accept` de XML/YAML na listagem dá 406.

**Para o tutorial novo:** dizer que o workflow precisa ser adaptado ao projeto (versão do Java, pasta do projeto, nome da imagem do `docker compose`, nomes do ECR, do contêiner, do Service e do cluster); avisar que o `task-definition.json` precisa existir e estar versionado; avisar que a tag do deploy é o hash do commit e que `latest` é só uma cópia; lembrar que *Desired tasks* não é alterado pela esteira.

---

## 15. Ligar e desligar o ambiente (para estudar sem gastar)

### 15.1 Ligar

**Passo a passo executado em 2026-11-01** (a tarefa subiu e a API respondeu no Postman):

1. **RDS:** **Aurora and RDS** → **Databases** → `database-crud-alunos` → **Actions** → **Start**, e esperar **Available**. *(Na release 1.0.0 a aplicação guarda os alunos em memória e não conecta no banco, então a tarefa sobe mesmo com o RDS parado. **Desde a release 1.1.0** (JPA, datasource e Flyway) a aplicação exige o banco no arranque: com o RDS parado a tarefa cai ao subir. Ligue sempre o RDS primeiro e espere **Available**.)*
2. **Service:** **Elastic Container Service** → **Clusters** → `cluster-crud-alunos` → aba **Services** → `service-crud-alunos` → **Update** → **Desired tasks = 1** → **Update**.
3. Aba **Tasks** do cluster (filtro *Any desired status*): esperar **Last status = Running**.
4. **IP novo:** coluna **Public IP** da tarefa, ou ID da tarefa → **Configuration** → **Public IP**.
5. **Testar:** `http://<IP>:8080/alunos` no Postman, trocando só o valor de `baseUrl` (sem barra no fim).

> 🔴 **Engano que aconteceu:** ao procurar o *Desired tasks*, a página aberta foi a da **Task Definition** (`crud-alunos-task-definition:2`), que não tem esse campo. Ele fica na página do **Service**: Clusters → cluster → Services → o Service → **Update**.

> 💡 O teste em `/swagger-ui/index.html` do tutorial não se aplica (o projeto não tem Swagger): use `GET /alunos`.

> ⚠️ Se o teste der timeout, o motivo mais provável é o seu IP de casa ter mudado: em **EC2 → Security Groups → `security-group-crud-alunos`**, edite a regra da 8080 e escolha **My IP** de novo.

### 15.2 Desligar

**Passo a passo executado em 2026-10-30:**

1. **Aplicação:** Service → **Update** → **Desired tasks = 0**. Conferir na aba **Tasks** que não há mais tarefas.
2. **RDS:** **Databases** → `database-crud-alunos` → **Actions** → **Stop temporarily**, e esperar **Stopped**.
3. Também foi apagado o banco de sobra `vitrodr-db-server` (us-east-1d), de uma tentativa anterior.

> ⚠️ **O RDS parado religa sozinho depois de 7 dias.** O nosso foi parado em 2026-10-30, então volta por volta de **2026-11-06** se não for usado: pare de novo ou apague.

**Para o tutorial novo:** destacar que *Desired tasks* fica na página do **Service** (não da Task Definition), que o IP muda a cada tarefa e que o teste depende de a aplicação ter Swagger ou não. As seções 15.3 e 15.4 ainda não foram executadas.

---

## Dúvidas abertas

| **Dúvida** | **Onde** | **Como resolver** |
| --- | --- | --- |
| Valor final do **Estimated monthly costs** depois dos ajustes (meta: bem abaixo de 145,64 USD). | Seção 1 | Conferir no console e anotar aqui. |
| A política de leitura do secret criada na seção 5.2 ficou em **outra role** (não na `ecsTaskExecutionRole`). Qual role é essa? Apagar a sobra? | Seção 5.2 | **IAM → Roles**, abrir as roles de ECS e procurar a `LerSecretDB_CrudAlunos`. Apagar a sobra, deixando a da `ecsTaskExecutionRole`. |
| O console está sendo usado com o **usuário raiz** da conta (`registeredBy` = `...:root`). | Geral | Criar um usuário IAM administrador com MFA para o dia a dia; deixar o raiz só para emergências. |
| Existe um grupo `rest-with-claude-app-sg` (nome do tutorial, de uma tentativa anterior) com regra na 3306 do banco. Manter ou apagar? | Seção 6.2 | Em **EC2 → Security Groups**, abra o grupo e veja em *Network interfaces* se algo o usa. Se nada usar, apague a regra da 3306 e depois o grupo. |
| Qual origem foi usada na porta 8080 do `security-group-crud-alunos`: `0.0.0.0/0` ou **My IP**? | Seção 6.1 | Conferir em EC2 → Security Groups → Inbound rules e anotar. |
| O cluster `rest-with-claude-spring` (sobra do tutorial original, com 1 Service e *Container Insights enhanced*) continua existindo, com 0 tarefas. Apagar? | Geral | Em **ECS → Clusters**, apagar o Service e depois o cluster, ou ao menos desligar o *Container Insights*. |
| Os perfis `default` e `crud-alunos` da AWS CLI são o mesmo usuário (`GITHUB_ACTIONS_CRUD_ALUNOS`), sem permissão de RDS. O `default` é necessário? | Seção 8 | Conferir com `aws sts get-caller-identity --profile default`; se for só duplicata, remover do `~/.aws`. |
| O repositório ficou com o nome `respositorio-crud-alunos` (falta um "i" em "repositorio"). Manter ou recriar? | Seção 2 | Recriar agora (o repositório está vazio) evita carregar o erro para a Task Definition, o `docker push` e o workflow. Atualizar a tabela de valores conforme a decisão. |

## Problemas e diferenças encontradas

| **Sintoma** | **Causa e Solução** |
| --- | --- |
| A tela de criação mostra "DB cluster identifier" e instâncias `db.r7g` | O engine **Aurora** está selecionado. Clique em **MySQL** e use **Full configuration**. |
| Estimativa de ~145 USD/mês ao criar o RDS | Valores padrão: `db.m7g.large` e 200 GiB. Use `db.t4g.micro` (Burstable) e 20 GiB. |
| Não acho "Initial database name" | Está na seção recolhida **Additional configuration**, no fim da página. |
| Erro "CIDR block, a security group ID or a prefix list has to be specified" ao salvar a regra da 3306 | O campo *Source* recebeu só o texto do nome. Escolha o grupo **na lista suspensa** ou cole o ID `sg-...` dele. |
| Tarefa do Service para com `ResourceInitializationError ... AccessDeniedException ... secretsmanager:GetSecretValue` | A `ecsTaskExecutionRole` não tem a política de leitura do secret. Crie a política inline nela (IAM → Roles) e force um novo deploy do Service. |
| Service fica `0/1 Tasks running` | Abra a aba **Tasks** (filtro *Any desired status*) e leia o **Stopped reason** da tarefa; ele indica a causa. |
| `aws: [ERROR]: The config profile (crud-alunos) could not be found` | O perfil ainda não foi criado. Rode `aws configure --profile crud-alunos`. |
| `password is empty` no `docker login` | O comando `aws ecr get-login-password` falhou (perfil inexistente ou credenciais erradas) e não passou senha. Corrija o perfil e rode o login de novo. |
| `error from registry: Your authorization token has expired` no `docker push` | O login no ECR falhou ou expirou (vale 12 horas). Refaça o login e confirme `Login Succeeded` antes do push. |
| Login falha mesmo com o perfil criado | Confira se a Access Key ID e a Secret Access Key não foram digitadas **trocadas** ou com o **nome do usuário** no lugar da chave. Refaça `aws configure --profile crud-alunos`. |
| `aws: [ERROR]: ... required: command` ao rodar `aws --verison` | Erro de digitação: o certo é `aws --version`. |
| Esteira verde, mas a API não responde | O Service está com **Desired tasks = 0**, ou o IP mudou. Ligue o Service (seção 15.1) e pegue o IP novo. |
| Procuro **Desired tasks** e a tela não tem | Você está na página da **Task Definition**. Vá em Clusters → cluster → Services → o Service → **Update**. |
| Não acho o campo "Endpoint" | Aba **Connectivity & security** → painel **Connect using** → **Endpoints**. |
