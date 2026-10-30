# AWS - Esteira CICD

```
# 🚀 Tutorial: Deploy na AWS (RDS + ECR + ECS Fargate)

> 💡 **Objetivo:** Ao final, a aplicação Spring Boot roda na AWS, acessível por um IP público, usando um banco MySQL no RDS. Cada push na `main` (exceto os que alteram só `.md`) atualiza o deploy automaticamente. *(A esteira está configurada no `CICD.md`).*

---

## 🗺️ Visão Geral do Fluxo

```text
GitHub Actions → imagem no ECR → ECS Fargate (aplicação) → RDS MySQL (banco)
                                                           ↑
                                      senha do banco vem do Secrets Manager
```

### Valores usados neste tutorial *(troque pelos seus, se forem diferentes)*:

| **Item** | **Valor** |
| --- | --- |
| **Região** | N. Virginia (`us-east-1`) |
| **Conta AWS** | `164801376891` |
| **Banco RDS** | `vitrodr-db-server` |
| **Repositório ECR** | `rest-with-claude` |
| **Cluster ECS** | `rest-with-claude-spring` |
| **Task Definition** | `rest-with-claude-spring-taskdefinition` |
| **Porta da aplicação** | `8080` |

> 💡 *Dica:* Os nomes dos botões podem variar um pouco, porque a AWS muda o console com frequência. Se alguma tela for diferente, procure o botão de nome parecido.
> 

## 0. Antes de começar

1. Acesse **https://console.aws.amazon.com** e entre com sua conta.
2. No canto **superior direito**, clique no nome da região e escolha **US East (N. Virginia) us-east-1**. **Todos os passos abaixo usam essa região.** Se mudar de região, os recursos que você criou "somem" da tela.
3. Para abrir qualquer serviço, use a **barra de busca no topo** (atalho `Alt+S`), digite o nome e clique no resultado.

## 1. Criar o banco MySQL (RDS)

1. Busque **Aurora and RDS** e clique.
2. No menu da esquerda, clique em **Databases** → botão laranja **Create database**.
3. Em **Choose a database creation method**, deixe **Standard create**.
4. Em **Engine options**, escolha **MySQL**. Deixe a versão sugerida.
5. Em **Templates**, escolha **Free tier** (se aparecer) ou **Dev/Test**.
6. Em **Settings**:
    - **DB instance identifier:** `vitrodr-db-server`
    - **Master username:** `root`
    - **Credentials management:** escolha **Self managed**
    - **Master password** e **Confirm password:** crie uma senha forte e **anote**. Você vai usá-la no passo 5.
7. Em **Instance configuration**, escolha `db.t4g.micro` (o mais barato).
8. Em **Storage**, deixe o padrão (20 GiB).
9. Em **Connectivity**:
    - **Public access:** **Yes** (necessário para acessar o banco do seu computador).
    - **VPC security group:** deixe **Choose existing** e o grupo **default**.
10. Role até o fim, abra **Additional configuration** e, em **Initial database name**, digite `rest_udemy`.
11. Clique em **Create database**. Aguarde o **Status** ficar **Available** (de 5 a 10 minutos).
12. Clique no nome do banco e copie o **Endpoint** (aba **Connectivity & security**). Exemplo: `vitrodr-db-server.xxxx.us-east-1.rds.amazonaws.com`.

> 💰 **Custo:** O RDS cobra enquanto está ligado. Veja os passos 14 (limpeza) e 15 (ligar e desligar) quando terminar.
> 

## 2. Criar o repositório de imagens (ECR)

1. Busque **Elastic Container Registry** e clique.
2. No menu da esquerda, clique em **Repositories** → **Create repository**.
3. **Visibility settings:** **Private**.
4. **Repository name:** `rest-with-claude`.
5. Deixe o resto como está e clique em **Create repository**.
6. Anote o **URI** que aparece na lista: `164801376891.dkr.ecr.us-east-1.amazonaws.com/rest-with-claude`.

## 3. Criar os usuários de acesso (IAM)

> ⚠️ A esteira precisa de um usuário próprio, com poucas permissões. **Nunca** use o administrador na esteira.
> 

### 3.1 Criar o usuário `GITHUB_ACTIONS`

1. Busque **IAM** e clique.
2. No menu da esquerda, clique em **Users** → **Create user**.
3. **User name:** `GITHUB_ACTIONS`. **Não** marque "Provide user access to the AWS Management Console". Clique em **Next**.
4. Escolha **Attach policies directly**.
5. Na caixa de busca, procure e **marque** estas duas:
    - `AmazonEC2ContainerRegistryFullAccess`
    - `AmazonECS_FullAccess`
6. Clique em **Next** → **Create user**.

### 3.2 Gerar a chave de acesso

1. Na lista de usuários, clique em `GITHUB_ACTIONS`.
2. Abra a aba **Security credentials**.
3. Role até **Access keys** e clique em **Create access key**.
4. Escolha **Application running outside AWS**, marque a caixa de confirmação e clique em **Next**.
5. (Opcional) Descreva como `github-actions` e clique em **Create access key**.
6. **Copie agora** a **Access key** e a **Secret access key** (clique em *Show*). A secret não aparece de novo. Guarde-as para o passo 11, onde você as cadastra no GitHub.

> 🔒 **Nunca** cole essas chaves no código, em mensagens ou em arquivos do projeto.
> 

## 4. Criar o cluster (ECS)

1. Busque **Elastic Container Service** e clique.
2. No menu da esquerda, clique em **Clusters** → **Create cluster**.
3. **Cluster name:** `rest-with-claude-spring`.
4. Em **Infrastructure**, deixe marcado **AWS Fargate (serverless)**.
5. (Opcional) Em **Monitoring**, ative o *Container Insights* para ter métricas (aumenta um pouco o custo).
6. Clique em **Create**. Aguarde a mensagem de sucesso.

## 5. Guardar a senha do banco (Secrets Manager)

> A senha não pode ficar no código nem na imagem. A aplicação a recebe do Secrets Manager.
> 

### 5.1 Criar o secret

1. Busque **Secrets Manager** e clique.
2. Clique em **Store a new secret**.
3. **Secret type:** **Other type of secret**.
4. Na parte **Key/value pairs**, clique na aba **Plaintext** e **apague** o conteúdo. Digite **apenas a senha** do RDS (sem aspas, sem chaves).
5. Clique em **Next**.
6. **Secret name:** `rest-with-claude/db-password`. Clique em **Next** duas vezes (deixe a rotação desligada) e depois em **Store**.

### 5.2 Permitir que a aplicação leia o secret

*(Esta permissão vai na role que a task usa. Se a role ainda não existir, volte aqui depois do passo 7.1).*

1. Busque **IAM** → **Roles**.
2. Na busca da lista, digite o nome da role (por exemplo `ECS-role-bd3e720b`, ou `ecsTaskExecutionRole`) e clique nela.
3. Na aba **Permissions**, clique em **Add permissions** → **Create inline policy**.
4. Clique na aba **JSON**, apague tudo e cole:

JSON

# 

```
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": "secretsmanager:GetSecretValue",
    "Resource": "arn:aws:secretsmanager:us-east-1:164801376891:secret:rest-with-claude/db-password-*"
  }]
}
```

1. Clique em **Next**. Em **Policy name**, digite `LerSecretDB`. Clique em **Create policy**.

## 6. Criar o security group da aplicação

> O security group é o "firewall" que libera as portas.
> 

### 6.1 Criar o grupo

1. Busque **EC2** e clique.
2. No menu da esquerda, em **Network & Security**, clique em **Security Groups** → **Create security group**.
3. **Security group name:** `rest-with-claude-app-sg`. **Description:** `Aplicacao rest-with-claude`.
4. **VPC:** Deixe a **default**.
5. Em **Inbound rules**, clique em **Add rule**:
    - **Type:** Custom TCP
    - **Port range:** `8080`
    - **Source:** Anywhere-IPv4 (`0.0.0.0/0`)
6. Clique em **Create security group**.

### 6.2 Liberar a aplicação no banco

1. Em **Security Groups**, clique no grupo **default** (o que o RDS usa).
2. Aba **Inbound rules** → **Edit inbound rules** → **Add rule**:
    - **Type:** MYSQL/Aurora (porta `3306`)
    - **Source:** Custom → digite `rest-with-claude-app-sg` e escolha o grupo que aparece.
3. (Opcional) Para acessar o banco do seu computador (HeidiSQL, por exemplo), adicione outra regra: **MYSQL/Aurora**, **Source:** My IP (o console preenche o seu IP com `/32`). Em **Description**, escreva `meu-computador`.
4. **Remova qualquer regra com origem `0.0.0.0/0` na porta 3306.** Se, ao testar, você abriu o banco para qualquer IP, clique em **Delete** ao lado dessa regra para corrigi-la.
5. Clique em **Save rules**.

Ao final, a porta 3306 deve ter só estas regras de entrada:

| **Origem** | **Para quê** |
| --- | --- |
| `rest-with-claude-app-sg` | a aplicação no ECS |
| `SEU_IP/32` | o seu computador (opcional) |

> ⚠️ **Segurança:** Nunca deixe a porta 3306 aberta para `0.0.0.0/0`. Qualquer pessoa na internet poderia tentar entrar no banco.
> 
> 
> *Seu IP muda?* Provedores residenciais costumam trocar o IP. Quando perder o acesso ao banco, volte aqui, edite a regra e escolha **My IP** de novo (descubra o IP em `https://checkip.amazonaws.com`).
> 

## 7. Criar a Task Definition (ECS)

> A Task Definition descreve como o contêiner roda: imagem, porta, CPU, memória e variáveis.
> 

### 7.1 Criar

1. Em **Elastic Container Service**, menu da esquerda: **Task definitions** → **Create new task definition**.
2. **Task definition family:** `rest-with-claude-spring-taskdefinition`.
3. **Launch type:** **AWS Fargate**. **OS/Architecture:** Linux/X86_64.
4. **CPU:** `1 vCPU`. **Memory:** `3 GB`.
5. **Task role** e **Task execution role:** Escolha **Create new role** (a AWS cria uma). Se já tiver uma, selecione-a e anote o nome (para aplicar o passo 5.2 depois).
6. Em **Container - 1**:
    - **Name:** `Container-vitrodr`
    - **Image URI:** `164801376891.dkr.ecr.us-east-1.amazonaws.com/rest-with-claude:latest`
    - **Container port:** `8080` *(Atenção: o padrão é 80, troque para 8080)*
    - **Protocol:** TCP
7. Em **Environment variables**, clique em **Add environment variable**:
    - **Key:** `DB_USER` | **Value type:** Value | **Value:** `root`
8. Clique em **Add environment variable** de novo:
    - **Key:** `TZ` | **Value type:** Value | **Value:** `America/Sao_Paulo` (fuso horário)
9. Clique em **Add environment variable** mais uma vez:
    - **Key:** `DB_PASSWORD` | **Value type:** **ValueFrom** | **Value:** `arn:aws:secretsmanager:us-east-1:164801376891:secret:rest-with-claude/db-password` (use o ARN do secret do Secrets Manager)
10. Em **Logging**, deixe **Use log collection** marcado (`awslogs`).
11. Clique em **Create**.

> 💡 *A imagem precisa existir no ECR antes de a tarefa subir. Veja o passo 8.*
> 

### 7.2 Exportar o JSON (para a esteira)

A esteira usa o arquivo `task-definition.json` do repositório. Ele já existe no projeto. Se um dia precisar gerar de novo: abra a Task Definition → **JSON** → **Download JSON** (ou use `aws ecs describe-task-definition`).

> ⚠️ **Atenção:** O `task-definition.json` guarda valores da sua conta (ID da conta, ARN das roles, ARN do secret, nome do contêiner). Se recriar a role, o secret ou o contêiner, atualize esse arquivo, senão a esteira registra uma Task Definition com valores antigos.
> 

## 8. Enviar a primeira imagem para o ECR

> O ECS precisa de uma imagem para iniciar. Você pode enviar a primeira manualmente ou deixar a esteira (passo 11) fazer isso.
> 
1. Instale a **AWS CLI** (instalador do Windows) e confirme no PowerShell: `aws --version`.
2. Configure a chave de um usuário com permissão no ECR: rode `aws configure` e informe a Access Key, Secret Key, região `us-east-1` e formato `json`.
3. Na pasta do projeto, execute os comandos para gerar e enviar a imagem:

PowerShell

# 

```
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 164801376891.dkr.ecr.us-east-1.amazonaws.com
docker build -t rest-with-claude .
docker tag rest-with-claude:latest [164801376891.dkr.ecr.us-east-1.amazonaws.com/rest-with-claude:latest](https://164801376891.dkr.ecr.us-east-1.amazonaws.com/rest-with-claude:latest)
docker push [164801376891.dkr.ecr.us-east-1.amazonaws.com/rest-with-claude:latest](https://164801376891.dkr.ecr.us-east-1.amazonaws.com/rest-with-claude:latest)
```

1. No console do **ECR**, abra o repositório e confirme que a imagem `latest` aparece na lista.

## 9. Criar o Service (ECS)

> O Service mantém a aplicação rodando e a reinicia se cair.
> 
1. Em **Elastic Container Service** → **Clusters** → clique em `rest-with-claude-spring`.
2. Na aba **Services**, clique em **Create**.
3. **Compute options:** **Capacity provider strategy** (padrão Fargate).
4. **Application type:** **Service**.
5. **Task definition family:** `rest-with-claude-spring-taskdefinition`; **Revision:** a mais recente.
6. **Service name:** `rest-with-claude-spring-taskdefinition-service` *(a AWS pode acrescentar um sufixo)*.
7. **Desired tasks:** `1`.
8. Abra **Networking**:
    - **VPC:** A default.
    - **Subnets:** Deixe **somente** as subnets das zonas `us-east-1a` a `us-east-1f`. **Remova** qualquer subnet cuja zona seja `us-east-1-atl-...` ou parecida (**Local Zone**). O Fargate não funciona nelas, e o Service fica parado sem gerar erro evidente.
    - **Security group:** Escolha **Use an existing security group**, remova o `default` e selecione `rest-with-claude-app-sg`.
    - **Public IP:** **Turned on**.
9. Clique em **Create**.
10. Aguarde. Na aba **Tasks**, o **Last status** deve virar **Running** em poucos minutos.

## 10. Testar a aplicação

1. No cluster, aba **Tasks**, clique no ID da tarefa em execução.
2. Em **Configuration**, copie o **Public IP**.
3. No navegador, abra `http://<IP-público>:8080/swagger-ui/index.html`. A página do Swagger deve abrir.
4. Abrir só `http://<IP-público>:8080/` devolve **401 (não autorizado)**. Isso é normal: significa que a aplicação está **no ar** e a API pede autenticação.

### Testar a API no Postman

1. Faça login via **POST** em `http://<IP-público>:8080/auth/signin` com a aba **Body → raw → JSON**:

JSON

# 

```
{ "username": "SEU_USUARIO", "password": "SUA_SENHA" }
```

1. Copie o `accessToken` da resposta.
2. Nas outras chamadas (por exemplo, **GET** em `http://<IP-público>:8080/person`), vá na aba **Authorization**, escolha **Bearer Token** e cole o token.

> 💡 *Dica:* Crie no Postman a variável `{{base_url}}` = `http://<IP-público>:8080` e use `{{base_url}}/auth/signin`. Assim, quando o IP mudar, você troca em um único lugar. Cuidado para não deixar barra dupla (`//auth/signin`), pois isso altera o caminho e a API responde 401.
> 

### Ver os logs

1. Na tarefa, abra a aba **Logs**, ou busque **CloudWatch** → **Log groups** → `/ecs/rest-with-claude-spring-taskdefinition`.
2. Procure `Started Startup` (aplicação subiu) ou `ERROR` (algo falhou).

> *Nota:* O IP público muda sempre que uma nova tarefa é criada. Para um endereço fixo, seria necessário um Load Balancer.
> 

## 11. Ligar a esteira (GitHub Actions)

1. Abra o repositório no GitHub → **Settings**.
2. Menu da esquerda: **Secrets and variables** → **Actions**.
3. Na aba **Secrets**, clique em **New repository secret** e crie:
    - **Name:** `AWS_ACCESS_KEY_ID` | **Secret:** A Access Key do passo 3.2
    - **Name:** `AWS_SECRET_ACCESS_KEY` | **Secret:** A Secret Access Key do passo 3.2
4. Se for usar o Docker Hub (opcional), confira que `DOCKER_USERNAME` e `DOCKER_ACCESS_TOKEN` também estão cadastrados (veja o `CICD.md`). O deploy na AWS não depende deles.
5. Antes do push, deixe o ambiente pronto: o RDS **Available** e o Service com **Desired tasks = 1** (seção 15).
6. Faça um `git push` na `main`. Acompanhe na aba **Actions** do GitHub: o último passo, **Deploy to Amazon ECS**, deve ficar verde.
7. No ECS, o Service passa a usar uma nova revisão da Task Definition com a imagem do commit.

> Os nomes dos secrets precisam ser exatamente os acima, e precisam estar na aba **Secrets**, não em *Variables*.
> 

> **O que a esteira não faz:**
> 
> - Ela **não altera** o *Desired tasks*. Com 0, termina verde, mas nenhuma aplicação sobe.
> - Push que muda **só arquivos `.md`** não dispara a esteira (regra `paths-ignore`).
> - O nome do Service (`service:`) é fixo no workflow `.github/workflows/continuous-deployment.yml`. Se você recriar o Service, o nome muda e você precisará atualizar esse campo.

## 12. Rotina de uso

- **Publicar uma nova versão:** `git push` na `main` (com o ambiente ligado, veja a seção 15). A esteira cuida do resto. Push que altera só `.md` não dispara a esteira.
- **Ver se está rodando:** ECS → Cluster → Service → **1 running**.
- **Ver erros:** CloudWatch → `/ecs/rest-with-claude-spring-taskdefinition`.

## 13. Problemas comuns

| **Sintoma** | **Causa e Solução** |
| --- | --- |
| Service em `IN_PROGRESS` com 0 tarefas e sem eventos | Há subnets de **Local Zone** no Service. Em **Update service** → **Networking**, mantenha só as zonas `1a` a `1f`. |
| Para ver o erro real do Service | Rode uma tarefa avulsa (**Run new task** na aba Tasks do cluster) ou use `aws ecs run-task`: o erro virá detalhado. |
| Tarefa sobe e cai em segundos | `DB_PASSWORD` ausente ou banco inacessível. Veja o log no CloudWatch. |
| `ResourceInitializationError` ao buscar o secret | A role da task não tem a política `LerSecretDB` (passo 5.2). |
| `CannotPullContainerError` | A imagem `latest` não está no ECR (passo 8), ou o nome da imagem na Task Definition está errado. |
| Timeout ao conectar no banco (pela aplicação) | O security group do RDS não aceita o `rest-with-claude-app-sg` na porta 3306 (passo 6.2). |
| Timeout ao conectar no banco (pelo seu computador) | Seu IP mudou. Atualize a regra do seu IP na porta 3306 (passo 6.2). |
| Página não abre em `http://<ip>:8080` | Porta do contêiner diferente de 8080 ou security group sem a regra para a porta 8080. |
| `AccessDenied` no workflow | Usuário `GITHUB_ACTIONS` sem permissão, ou secrets da AWS com nome ou valor errado. |
| Esteira verde, mas a aplicação não sobe | O Service está com **Desired tasks = 0**, ou o RDS está parado. Ligue na ordem da seção 15. |
| `Service not found` no passo de deploy | O nome do Service mudou. Atualize o campo `service:` no workflow. |
| 401 em `/auth/signin` | Método diferente de POST, ou barra dupla na URL (`//auth/signin`). Confira também o corpo JSON (`username` e `password`). |
| Aviso do Flyway sobre MySQL 8.4 | É apenas um aviso. Atualize a versão do Flyway quando for conveniente. |

## 14. Limpeza (evitar cobrança)

O que gera custo enquanto está ligado: **RDS**, **tarefa Fargate** e (se ativado) *Container Insights*.

Para apenas **pausar** (e voltar depois), siga a seção 15. Esta seção é para **encerrar de vez**:

1. **Apagar a aplicação:** ECS → Cluster → Service → **Delete service**. Lembre-se de que, se recriar o Service, o nome muda e o workflow precisa ser atualizado.
2. **Apagar o banco:** RDS → Databases → selecione → **Actions** → **Delete** (crie um snapshot final se quiser guardar os dados).
3. **Remover chaves sem uso:** IAM → Users → o usuário → **Security credentials** → apague as Access Keys que não usa mais.
4. **Excluir imagens antigas:** ECR → o repositório → selecione as imagens → **Delete**.

## 15. Ligar e desligar o ambiente (para estudar sem gastar)

Tudo o que foi criado (cluster, Task Definition, repositório, secret, security groups) fica guardado. Só o que **roda** gera custo mais alto: a tarefa no Fargate e o RDS. Desligar esses dois deixa o ambiente praticamente sem custo.

### 15.1 Ligar

**Ordem importa:** o banco precisa estar pronto antes da aplicação, porque o Spring conecta nele ao iniciar.

1. **Ligar o RDS:**
    1. Busque **Aurora and RDS** → **Databases**.
    2. Marque `vitrodr-db-server`.
    3. Clique em **Actions** → **Start**.
    4. Espere a coluna **Status** mudar de `Starting` para **Available** (alguns minutos). Clique no botão de atualizar da tabela para acompanhar.
2. **Ligar a aplicação:**
    1. Busque **Elastic Container Service** → **Clusters** → `rest-with-claude-spring`.
    2. Na aba **Services**, marque o Service e clique em **Update**.
    3. Em **Desired tasks**, digite `1`.
    4. Role até o fim e clique em **Update**.
3. **Esperar a tarefa subir:**
    1. Abra o Service e a aba **Tasks**.
    2. Espere o **Last status** ficar **Running** (de 1 a 3 minutos).
4. **Pegar o IP novo:**
    1. Clique no ID da tarefa.
    2. Em **Configuration**, copie o **Public IP**. Ele muda a cada vez que a tarefa é criada.
5. **Testar:**
    1. Abra `http://<IP>:8080/swagger-ui/index.html` no navegador.
    2. No Postman, troque o endereço base (se usar a variável `{{base_url}}`, é só alterá-la). Cuidado para não deixar `//` na URL.

> Se você for testar a esteira, o RDS precisa estar `Available` antes do push. E lembre-se de colocar **Desired tasks = 1**: a esteira registra a nova versão, mas não altera a quantidade de tarefas.
> 

### 15.2 Desligar

1. **Desligar a aplicação:**
    1. **Elastic Container Service** → **Clusters** → `rest-with-claude-spring`.
    2. Marque o Service e clique em **Update**.
    3. Em **Desired tasks**, digite `0` e clique em **Update**.
    4. Confirme na aba **Tasks** que não há mais tarefas em execução.
2. **Pausar o RDS:**
    1. **Aurora and RDS** → **Databases** → marque `vitrodr-db-server`.
    2. **Actions** → **Stop temporarily** → confirme em **Stop temporarily**.
    3. Espere o **Status** ficar `Stopped`.

### 15.3 O que fica cobrando mesmo desligado

| **Item** | **Custo aproximado** |
| --- | --- |
| Armazenamento do RDS (20 GiB) | Centavos por mês |
| Secret no Secrets Manager | Cerca de US$ 0,40 por mês |
| Imagens no ECR | Centavos por mês |

### 15.4 Cuidados

- **O RDS religa sozinho depois de 7 dias parado.** A AWS faz isso para manter o banco atualizado. Se você não for usá-lo, pause de novo. Anote a data para conferir.
- **O IP público da aplicação muda** a cada vez que a tarefa sobe. Não use o IP antigo.
- **O seu IP de casa** está liberado no RDS. Se o provedor trocar o IP, ajuste a regra (passo 6.2).
- **Para não ser pego de surpresa:** em **Billing and Cost Management** → **Budgets** → **Create budget** → **Zero spend budget**, configure com o seu e-mail. A AWS avisa assim que houver qualquer cobrança.
- **Para conferir o que está ligado** (sem entrar em vários serviços): em **Billing** → **Bills**, veja o custo detalhado por serviço no mês.