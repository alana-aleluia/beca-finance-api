# Autenticação do MS1

O MS1 usa HTTP Basic sem sessão, sem cookies de login e sem JWT. Os clientes previstos
são Postman e serviços que enviam `Authorization` explicitamente em cada requisição.
Use HTTPS fora do ambiente local: Basic é Base64, não criptografia de transporte.

As interfaces de portas usam o sufixo `Port`. Em `application.port.in` ficam
`CreateUserPort`, `ManageUsersPort`, `ImportUsersPort` e `AuthenticateUserPort`;
em `application.port.out`, `UserRepositoryPort`, `UserCsvReaderPort` e `PasswordHasherPort`.

## Cadastro e identidade

`POST /users` é público e exige JSON com `name`, `email` e `password`.
A senha deve ter comprimento mínimo 8, conforme a validação `@Size` do Java (unidades
UTF-16), não pode ser inteiramente branca e deve ter no máximo 72 bytes UTF-8
(limite do BCrypt). Espaços e maiúsculas da senha são
preservados. O e-mail recebe trim e lowercase; a senha não é normalizada.

O banco armazena exclusivamente `password_hash`, gerado com BCrypt com salt.
As respostas usam DTOs sem senha ou hash. Não registre bodies de cadastro nem o
header Authorization em logs de aplicação, proxy ou ferramentas de observabilidade.

`GET /auth/me` exige Basic, com e-mail como username e senha original como password.
Retorna somente `{"id":123,"email":"usuario@example.com"}`. Credenciais incorretas,
usuário inexistente e usuário sem senha retornam 401, sem diferenciar essas causas.
Não há login que gere token: cada requisição é autenticada novamente.
Não há rate limiting nem bloqueio temporário após falhas de autenticação.
BCrypt não limita a quantidade de tentativas.

## Autorização

| Rota | Política |
|---|---|
| POST /users | Pública; senha obrigatória |
| GET /auth/me | Qualquer usuário autenticado |
| GET, PUT, DELETE /users/{id} | Somente o próprio ID autenticado |
| GET /users | Administrador |
| POST /users/import | Administrador |
| Swagger e /v3/api-docs | Públicos; operações protegidas continuam exigindo Basic |
| Outras rotas | Acesso negado |

Administradores também só atualizam/excluem o próprio perfil. A autoridade administrativa
permite somente listagem global e importação. Não é aceita no cadastro HTTP.
Por padrão nenhum usuário é administrador. O responsável pela execução pode atribuir
a autoridade a contas existentes configurando IDs confiáveis, por exemplo
`--security.admin-user-ids=1,5`. Confira os IDs antes de configurar; não há tela,
endpoint de promoção ou conta administrativa padrão.
A mesma propriedade pode ser configurada pela variável de ambiente
`SECURITY_ADMIN_USER_IDS=1,5` no processo do MS1. Reinicie a aplicação após
alterar os IDs. No Compose completo, a variável do `.env` ou do ambiente do
terminal é repassada ao container `ms-users`. Para executar fora do Compose,
defina-a no processo Java: o Spring não importa `.env` automaticamente.
Configurar o ID de um usuário sem senha não permite que ele autentique.
Veja o [README](../README.md) para o procedimento e as restrições.

PUT altera somente nome/e-mail, preservando hash e data de criação. Não há troca de
senha nesta entrega. Depois de trocar o e-mail, use o novo e-mail no Basic.

O CSV continua exigindo exatamente `name,email`. Usuários antigos/importados com hash
nulo não autenticam e recebem 401. Nenhuma senha padrão é criada. Não existe
fluxo de ativação ou definição posterior de senha; isso fica fora do escopo.
O cadastro público com o mesmo e-mail é rejeitado por duplicidade e não ativa
o usuário importado.

## Integração com o MS2 e limites

O MS2 valida as credenciais Basic no MS1 por `GET /auth/me` a cada requisição
protegida e utiliza o ID retornado como identidade, sem confiar em userId enviado
pelo cliente. Não copia usuários ou hashes nem envia Basic à MockAPI ou BrasilAPI.
Credenciais inválidas resultam em 401; indisponibilidade, timeout ou resposta
inválida do MS1 resultam em 503. O Compose usa HTTP na rede local entre containers;
use HTTPS fora do ambiente local.

O MS2 restringe consulta, atualização e cancelamento ao usuário de origem da
transação; cada usuário consulta o próprio saldo. A exclusão de um usuário no MS1
impede sua autenticação, mas não apaga o histórico financeiro no MS2. A política
de retenção posterior permanece fora do escopo desta entrega.

CSRF está desativado para esta API destinada a clientes que enviam Basic explicitamente.
Não use esta configuração como login de navegador com credenciais Basic memorizadas:
stateless não elimina CSRF nesse cenário. Antes de integrar um frontend de navegador,
reavalie proteção CSRF e CORS. Não há configuração CORS liberando origens externas.

## Postman

1. Envie POST `http://localhost:8081/users`, Authorization **No Auth**, Body **raw/JSON**:
   `{"name":"Ana","email":"ana@example.com","password":"SenhaLocal123!"}`.
   Espere 201, Location e dados públicos do usuário.
2. Envie GET `http://localhost:8081/auth/me`, Authorization **Basic Auth**:
   Username `ana@example.com`, Password `SenhaLocal123!`. Espere 200 com id/email.
3. Repita com senha incorreta ou sem Authorization: espere 401.
4. Use o ID retornado para GET/PUT/DELETE `/users/{id}`. Outro ID deve retornar 403.
   GET `/users` e importação devem retornar 403 para contas comuns.
5. Para testar importação, configure explicitamente um ID administrativo e envie
   multipart form-data, campo `file`, com um CSV `name,email`.
