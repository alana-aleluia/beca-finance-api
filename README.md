# Beca Finance — Desafio Java 2026

API de gestão financeira em dois microsserviços independentes: usuários (8081) e
transações (8082), com PostgreSQL próprio, Flyway e arquitetura hexagonal.
Requisitos oficiais usados como referência: documento local do desafio.

## Pré-requisitos e configuração

- Rancher Desktop com Moby/dockerd, Docker CLI e Docker Compose disponíveis.
- Java 21 para executar localmente/testar fora dos containers. Maven Wrapper incluído.
- Postman ou Swagger; internet para MockAPI e BrasilAPI.

Na raiz, crie `.env` a partir de `.env.example` **apenas se ainda não existir**:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

Preencha localmente, sem commitar:

| Variável | Responsabilidade |
|---|---|
| `DB_PASSWORD` | Senha do PostgreSQL do MS1 |
| `TRANSACTIONS_DB_PASSWORD` | Senha do PostgreSQL do MS2 |
| `MOCKAPI_ACCOUNTS_URL` | URL completa do recurso `accounts` |
| `SECURITY_ADMIN_USER_IDS` | Opcional; IDs administrativos do MS1, separados por vírgula |
| `BRASILAPI_URL` | Opcional; padrão `https://brasilapi.com.br` |

Use as senhas que inicializaram os volumes existentes. Alterar `.env` não troca a
senha já armazenada no PostgreSQL. Não publique credenciais, `.env` ou logs de Authorization.

Contrato MockAPI (um recurso por usuário):

```json
{"id":"1","userId":2,"initialBalance":"1000.00","currency":"BRL","purchaseLimit":"500.00"}
```

Cadastre valores fixos e um `userId` real obtido em `/auth/me`. Uma transferência
precisa de um segundo usuário cadastrado e de sua conta no MockAPI. A existência
da conta de destino é verificada no Mock; não há acesso ao banco do MS1.
O recurso não deve conter senhas, e-mails ou hashes.

## Iniciar o ambiente completo

Se as aplicações estiverem rodando no IntelliJ, pare essas execuções para liberar
8081/8082. Se o banco do MS2 foi iniciado pelo Compose do módulo, pare **somente
esse container**, preservando o volume:

```powershell
docker compose --env-file .env -f ms-transactions/compose.yaml stop postgres-transactions
```

O Compose da raiz reutiliza, por padrão, o volume `beca-transactions_transactions_data`
da execução anterior do MS2. Não mantenha dois servidores PostgreSQL sobre esse
mesmo volume. Se usou outro nome, configure `TRANSACTIONS_VOLUME_NAME` com o nome real.
O volume do MS1 permanece `users_data` no projeto Compose da raiz.

Na raiz:

```powershell
docker compose up --build
```

Esse comando sobe as duas aplicações e os dois bancos. Para executar em segundo plano,
use `docker compose up --build -d --wait`. As migrações são automáticas, sem recriar
as tabelas ou apagar os dados. Verifique com `docker compose ps` e
`docker compose logs --tail 100 ms-users ms-transactions`.

| Serviço | Host local | Dentro da rede Compose |
|---|---|---|
| MS1 | `http://localhost:8081` | `http://ms-users:8081` |
| MS2 | `http://localhost:8082` | `http://ms-transactions:8082` |
| User DB | `localhost:5433`, `users_db`, `app_user` | `postgres-users:5432` |
| Transaction DB | `localhost:5434`, `transactions_db`, `transactions_user` | `postgres-transactions:5432` |

As portas externas podem ser substituídas por `USERS_HTTP_PORT`, `TRANSACTIONS_HTTP_PORT`,
`USERS_DB_PORT` e `TRANSACTIONS_DB_PORT`. Para parar preservando dados: `docker compose stop`.
**Não use `down -v`, `volume rm` ou recriação destrutiva de banco.**

Para rodar pelo IntelliJ, mantenha apenas os bancos no Docker; execute as classes
`MsUsersApplication` e `MsTransactionsApplication` com Java 21. Informe na configuração
Run `DB_PASSWORD` ao MS1; ao MS2, `TRANSACTIONS_DB_PASSWORD`, `MOCKAPI_ACCOUNTS_URL` e
`MS1_URL=http://localhost:8081`. O Spring não importa `.env` automaticamente.
Alternativa no terminal de cada módulo, com as variáveis definidas: `./mvnw.cmd spring-boot:run`.
Para subir somente os bancos pela raiz: `docker compose up -d postgres-users postgres-transactions`.

## Swagger e autenticação

- MS1: `http://localhost:8081/swagger-ui/index.html`
- MS2: `http://localhost:8082/swagger-ui/index.html`
- JSON OpenAPI: `/v3/api-docs` em cada serviço.

Cadastre em POST `/users` sem autenticação:

```json
{"name":"Ana Teste","email":"ana.teste@example.com","password":"SenhaExemplo123!"}
```

As credenciais acima são fictícias. Use senha própria. POST retorna 201, Location
e dados públicos. GET `/auth/me`, com Basic e essas credenciais, retorna apenas
`{"id":2,"email":"ana.teste@example.com"}` (o ID é ilustrativo: use o efetivo).
No Swagger, clique Authorize e informe e-mail/senha. O MS2 valida a identidade
pelo MS1 em cada requisição, sem copiar usuários ou hashes e sem enviar Basic
às outras APIs. Usuários sem hash, inclusive importados, não autenticam.

Use HTTPS fora do ambiente local. Não use login de navegador com Basic memorizado:
CSRF está desativado para clientes que enviam Authorization explicitamente.

Não há rate limiting nem bloqueio temporário após tentativas de autenticação
malsucedidas nesta entrega. HTTP Basic autentica cada requisição novamente;
o uso de BCrypt não limita a quantidade de tentativas.

### Importação CSV e acesso dos usuários

`POST /users/import` recebe multipart form-data no campo `file`, com CSV UTF-8
e cabeçalho exato `name,email`. O exemplo oficial é `examples/users.csv`.
A importação cria usuários sem senha (`password_hash` nulo), preserva os registros
válidos e informa erros por linha. Esses usuários não podem fazer login: recebem 401.
Não existe senha padrão nem fluxo de ativação ou definição posterior de senha;
esse fluxo está fora do escopo desta entrega. O cadastro público `POST /users`
com o mesmo e-mail é rejeitado por duplicidade e não ativa um perfil importado.

### Configurar um administrador

Não existe administrador padrão. Cadastre uma conta com senha por `POST /users`
e consulte seu ID em `GET /auth/me` usando Basic. Configure somente IDs confiáveis,
separados por vírgula; `1,5` abaixo são ilustrativos e devem ser substituídos pelos reais:

```text
SECURITY_ADMIN_USER_IDS=1,5
```

No Compose completo, defina essa variável no `.env` da raiz ou no ambiente do
terminal; o serviço `ms-users` a recebe em `environment`. Se o container já estiver
em execução, aplique a configuração com `docker compose up -d ms-users` para que
seja recriado quando necessário; apenas reiniciá-lo não atualiza seu ambiente.
No IntelliJ ou Maven, defina a variável no ambiente do processo Java, pois o Spring
não importa `.env` automaticamente. Alternativamente, use o argumento da aplicação
`--security.admin-user-ids=1,5`. Reinicie o MS1 executado localmente após alterar os IDs.

A variável corresponde à propriedade Spring `security.admin-user-ids`. Sem IDs
configurados, nenhuma conta recebe `ROLE_ADMIN`. Configurar o ID de um usuário sem
senha não permite que ele autentique. Não há endpoint de promoção nem papel
administrativo aceito no cadastro HTTP.

Somente `GET /users` e `POST /users/import` são exclusivos de ADMIN; contas
autenticadas sem esse papel recebem 403 nessas rotas. Mesmo administradores só
consultam, alteram e excluem o próprio perfil por `/users/{id}`.

## Endpoints

| Serviço / rota | Comportamento |
|---|---|
| POST `/users` | Cadastro público com senha obrigatória |
| GET `/auth/me` | Identidade autenticada |
| GET/PUT/DELETE `/users/{id}` | Somente o próprio perfil |
| GET `/users` | Listagem administrativa |
| POST `/users/import` | Multipart `file`, administrativo |
| POST `/transactions` | PURCHASE, DEPOSIT, WITHDRAWAL ou TRANSFER |
| GET `/transactions/{id}` | Registro do usuário de origem, inclusive histórico cancelado |
| GET `/transactions` | Listagem paginada do próprio histórico |
| PUT `/transactions/{id}` | Altera apenas categoria/descrição |
| DELETE `/transactions/{id}` | Cancelamento lógico com estorno seguro |
| GET `/account/balance` | Saldo inicial, atual e limite da própria conta |
| GET `/transactions/analysis` | Totais de despesas por categoria, dia ou mês |
| GET `/transactions/export` | Download do histórico em CSV |

Listagem, análise e CSV aceitam `from`/`to` (YYYY-MM-DD), `type`, `status`, `category`.
Listagem também aceita `page` (início 0) e `size` (padrão 50, máximo 200).
O período inclui os dias indicados no fuso America/Sao_Paulo. CSV exporta até
10000 registros; acima disso, retorna 400 e exige restringir o período, sem truncar.
Categorias são texto livre, com espaços externos removidos; nomes distintos não são mesclados.

### Exemplos Postman — MS2

Todos os exemplos usam Basic do usuário de origem. O usuário de origem nunca vem
do body. `destinationUserId` identifica somente o recebedor da transferência.

POST `http://localhost:8082/transactions`:

```json
{"type":"DEPOSIT","amount":100.00,"currency":"BRL","category":"APORTE","description":"Depósito de teste"}
```

```json
{"type":"WITHDRAWAL","amount":50.00,"currency":"BRL","category":"SAQUE"}
```

```json
{"type":"TRANSFER","amount":100.00,"currency":"BRL","category":"TRANSFERENCIA","destinationUserId":3}
```

```json
{"type":"PURCHASE","amount":10.00,"currency":"USD","category":"ALIMENTACAO"}
```

Substitua o destino por um ID real com conta MockAPI. Operações retornam 201 tanto
para APPROVED quanto para REJECTED. Uma compra BRL de 200 no cenário inicial
1000/limite 500 retorna APPROVED, `balanceBefore:1000`, `balanceAfter:800`.
Uma compra de 600 retorna REJECTED / PURCHASE_LIMIT_EXCEEDED e preserva o saldo.

GET `/transactions?type=PURCHASE&page=0&size=20`

GET `/transactions/analysis?groupBy=CATEGORY` (ou DAY / MONTH)

GET `/transactions/export` → use Send and Download no Postman.

PUT `/transactions/{id}` com `{"category":"MERCADO","description":"Correção cadastral"}`.
Valores, moedas, tipo e destinatário são imutáveis por essa rota; campos extras
não alteram dados financeiros. DELETE `/transactions/{id}` retorna 204 se cancelar,
409 se o estorno tornar algum saldo negativo/fora do limite; uma repetição não estorna duas vezes.
O original permanece consultável para auditoria, com `cancelledAt` e registro em
`transaction_cancellations`. Metadados de registro cancelado não podem ser alterados.

## Regras financeiras adotadas

Estas decisões complementam o enunciado, que não especifica todos esses detalhes:

- Uma conta BRL por usuário. A MockAPI fornece saldo inicial e limite, mas o saldo
  corrente pertence ao PostgreSQL. Nenhuma operação restaura o saldo inicial.
- `purchaseLimit` é limite **por compra**, escolhido para simulação; não se aplica
  a depósito, saque ou transferência, nem é limite acumulado.
- Valores positivos, até duas casas e 17 dígitos inteiros. Não há cheque especial,
  tarifas, juros, parcelamento ou crédito adicional implícito.
- Compras verificam limite antes de saldo. Saques/transferências verificam saldo.
  Depósitos creditam; transferências debitam origem e creditam destino em BRL.
- Apenas o usuário de origem consulta/edita/cancela o lançamento. O destinatário
  pode consultar seu próprio saldo; não recebe acesso aos saldos privados da origem.
- Somente PURCHASE APPROVED não cancelada conta como despesa. Retirada de dinheiro
  e transferência não são classificadas automaticamente como consumo.
- PUT é atualização de metadados; DELETE é cancelamento com estorno, preservando
  o histórico. Depósito já consumido e transferência cujo recebedor gastou o crédito
  podem não ser estornáveis: nesses casos, 409 e nenhuma alteração parcial.

### Câmbio

BRL usa fator 1 sem integração. Para moedas estrangeiras, o MS2 consulta
`/api/cambio/v1/cotacao/{moeda}/{data}` na BrasilAPI com o dia anterior, pois o dia
corrente não é disponibilizado. Aceita a data efetiva anterior retornada pela API
(janela máxima de 14 dias corridos), seleciona o boletim de horário mais recente
e aplica `cotacao_venda` como BRL por unidade. Não usa paridade como cotação.
Arredondamento do valor convertido: duas casas, HALF_EVEN; sem spread adicional.
A transação grava valor original, convertido, taxa, data, horário e boletim.
Uma taxa histórica já gravada nunca é recalculada ao consultar ou estornar.
Taxa inexistente, inválida ou indisponível não é substituída por taxa fictícia.

### Transações, concorrência e erros

Chamadas ao MS1, MockAPI e BrasilAPI terminam antes de adquirir bloqueios.
A operação local READ COMMITTED inicializa contas por unicidade e ON CONFLICT,
obtém PESSIMISTIC_WRITE em ordem crescente de ID e confirma saldo(s) e registro
na mesma transação. Transferências e estornos não têm débito/crédito parcial.
Cancelamentos bloqueiam primeiro o lançamento e registram a reversão uma única vez.

400: entrada inválida; 401: Basic ausente/inválido; 404: transação inexistente ou
de outro usuário; 409: estorno impossível/registro cancelado; 503: dependência,
bloqueio ou persistência indisponível. Falhas técnicas não viram REJECTED fictício.

**POST ainda não é idempotente.** O documento não exige chave de idempotência;
ela não é necessária ao bloqueio/estorno escolhido, mas uma repetição depois de
perda de resposta pode duplicar a operação. Não configure retry automático de POST.
O cancelamento, por sua vez, é protegido contra estorno repetido.

## Banco e migrações

MS1 preserva V1–V3. MS2 preserva V1 validada e usa V2 para novas operações/câmbio/auditoria.
Tabelas: `accounts`, `transactions`, `transaction_cancellations` e histórico Flyway.
Não há FK entre microsserviços. A exclusão de um usuário não apaga seu histórico
financeiro; a conta deixa de poder autenticar. Retenção posterior é decisão fora deste desafio.

```powershell
docker compose exec postgres-transactions psql -U transactions_user -d transactions_db
```

```sql
SELECT id,user_id,initial_balance,balance FROM accounts ORDER BY id;
SELECT id,type,amount,currency,amount_brl,status,rejection_reason,cancelled_at FROM transactions ORDER BY id;
SELECT * FROM transaction_cancellations ORDER BY transaction_id;
```

## Testes e cobertura

Com Java 21 e JAVA_HOME definido, execute a partir da raiz:

```powershell
Push-Location ms-users
.\mvnw.cmd verify
Pop-Location
Push-Location ms-transactions
.\mvnw.cmd verify
Pop-Location
```

Em ambos os módulos, `verify` executa os testes unitários e gera automaticamente
o relatório JaCoCo, configurado nos respectivos `pom.xml`. Abra os relatórios HTML:

- MS1: `ms-users/target/site/jacoco/index.html`.
- MS2: `ms-transactions/target/site/jacoco/index.html`.

Os diretórios `target/` são gerados localmente e não devem entrar no commit.

Para PostgreSQL real, use um banco **isolado local** cujo nome termina em `_test`:

```powershell
$env:TEST_DATABASE_URL = 'jdbc:postgresql://127.0.0.1:55440/ms2_checkpoint1_test'
$env:TEST_DATABASE_USER = 'postgres'
# TEST_DATABASE_PASSWORD, se necessário; nunca use o banco de desenvolvimento.
Set-Location ms-transactions
.\mvnw.cmd verify -Ppostgres-it
```

Os testes criam schemas novos, sem apagar os existentes. Sem TEST_DATABASE_URL,
testes PostgreSQL são ignorados. Mocks e H2 não são prova de bloqueio PostgreSQL.
Testes externos são opt-in: `MOCKAPI_LIVE_URL` (recurso accounts com usuário 2) e
`BRASILAPI_LIVE_URL=https://brasilapi.com.br`, com o mesmo perfil. Sem essas variáveis,
esses testes são ignorados, evitando dependência externa na suíte unitária.
Relatórios: `target/surefire-reports`, `target/failsafe-reports` e
`target/site/jacoco/index.html` dentro de cada módulo.

## Entrega

Arquivo de importação: `examples/users.csv`, cabeçalho exato `name,email`, UTF-8.
Importação exige administrador configurado, preserva sucesso parcial e não cria senhas.
Documentação detalhada do MS1: `docs/authentication.md`.
JWT, Excel/PDF, Testcontainers e Azure são bônus não implementados.
Jira/Kanban, compartilhamento com o mentor, publicação do código e demonstração
final devem ser comprovados pelo responsável pela entrega; não são inferidos pelo código.
