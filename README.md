# Beca Finance — MS1 Usuários

MS1 do Desafio Beca Java 2026: CRUD de usuários, importação CSV e autenticação
HTTP Basic. Consulte [a documentação de autenticação](docs/authentication.md)
para contratos, regras de acesso e exemplos de validação manual.

## Limitação de tentativas de autenticação

Não há rate limiting nem bloqueio temporário após tentativas de autenticação
malsucedidas nesta entrega. HTTP Basic autentica cada requisição novamente;
o uso de BCrypt não limita a quantidade de tentativas. Use HTTPS fora do ambiente local.

## Importação CSV e acesso dos usuários

`POST /users/import` recebe multipart form-data no campo `file`, com CSV UTF-8
cujo cabeçalho deve ser exatamente `name,email`. A importação cria usuários sem
senha (`password_hash` nulo), preserva os registros válidos e informa erros por linha.

Usuários importados não podem fazer login: suas tentativas de autenticação retornam
401. Não existe senha padrão nem fluxo de ativação ou definição posterior de senha;
esse fluxo está fora do escopo desta entrega. Importar um perfil não cria acesso à API.
O cadastro público `POST /users` exige senha e não ativa um perfil já importado:
o mesmo e-mail é rejeitado por duplicidade.

## Privilégios administrativos

Não existe administrador padrão. Após cadastrar uma conta com senha por `POST /users`,
consulte seu ID em `GET /auth/me` usando Basic e configure somente IDs confiáveis
na propriedade Spring `security.admin-user-ids`, separados por vírgula.
Os IDs `1,5` abaixo são ilustrativos; substitua-os pelos IDs reais.

Na configuração de execução do IntelliJ, defina a variável de ambiente:

```text
SECURITY_ADMIN_USER_IDS=1,5
```

Alternativamente, passe o argumento da aplicação:

```text
--security.admin-user-ids=1,5
```

A variável `SECURITY_ADMIN_USER_IDS` corresponde à propriedade
`security.admin-user-ids`. Reinicie o MS1 após alterar a configuração. Sem IDs
configurados, nenhuma conta recebe `ROLE_ADMIN`; configurar o ID de um usuário
sem senha não permite que ele autentique.

| Endpoint | Acesso |
|---|---|
| `GET /users` | Exclusivo de ADMIN |
| `POST /users/import` | Exclusivo de ADMIN |
| `GET /auth/me` | Qualquer usuário autenticado |
| `GET`, `PUT`, `DELETE /users/{id}` | Somente o próprio usuário, inclusive para ADMIN |
| `POST /users` | Público, com senha obrigatória |

Contas autenticadas sem ADMIN recebem 403 nas duas rotas administrativas.
Não há endpoint de promoção; o papel não pode ser solicitado no cadastro HTTP.

O Compose desta versão sobe somente o PostgreSQL, não a aplicação MS1.
Portanto, colocar `SECURITY_ADMIN_USER_IDS` no `.env` do Compose não configura
automaticamente o MS1 executado no IntelliJ ou pelo Maven. Defina a variável
no ambiente do processo Java ou use o argumento da aplicação acima.
O Spring não importa automaticamente o arquivo `.env`. Não versione credenciais.
