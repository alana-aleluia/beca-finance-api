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

## Autorização

| Rota | Política |
|---|---|
| POST /users | Pública; senha obrigatória |
| GET /auth/me | Qualquer usuário autenticado |
| GET, PUT, DELETE /users/{id} | Somente o próprio ID autenticado |
| GET /users | Administrador |
| POST /users/import | Administrador |
| Outras rotas | Acesso negado |

Administradores também só atualizam/excluem o próprio perfil. A autoridade administrativa
permite somente listagem global e importação. Não é aceita no cadastro HTTP.
Por padrão nenhum usuário é administrador. O responsável pela execução pode atribuir
a autoridade a contas existentes configurando IDs confiáveis, por exemplo
`--security.admin-user-ids=1,5`. Confira os IDs antes de configurar; não há tela,
endpoint de promoção ou conta administrativa padrão.

PUT altera somente nome/e-mail, preservando hash e data de criação. Não há troca de
senha nesta entrega. Depois de trocar o e-mail, use o novo e-mail no Basic.

O CSV continua exigindo exatamente `name,email`. Usuários antigos/importados com hash
nulo não autenticam. Nenhuma senha padrão é criada; ativação fica fora do escopo.

## Limites e futura integração

O MS2 deverá validar as credenciais no MS1 por HTTPS e utilizar o ID retornado por
/auth/me como identidade, sem confiar em userId enviado pelo cliente. Deverá tratar
indisponibilidade/timeout do MS1, restringir operações financeiras ao dono e definir
a política de retenção das transações após exclusão do usuário. Não há integração
implementada nesta fase.

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
