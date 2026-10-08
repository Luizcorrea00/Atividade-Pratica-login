# Login Seguro

Projeto acadêmico independente do PFC: Java 21, Spring Boot 3.5.16, Spring Security, Thymeleaf e MongoDB Atlas. Os pacotes de domínio não dependem dos temas visuais.

## Funcionalidades

- Cadastro público, login e logout por POST; cadastro sempre recebe USER.
- BCrypt custo 12, validação no servidor e e-mail normalizado com índice único.
- USER: painel; MANAGER: painel e gestão; ADMIN: todas as áreas e CRUD de usuários.
- Administrador pode editar nome, perfil e status e excluir usuários. Cadastro também é público. A própria conta administrativa não pode ser modificada/excluída nessa interface, preservando uma conta de administração.
- Sessões persistidas na coleção `sessions`, expiração de 30 minutos de inatividade e índice TTL. Expiração também é verificada pela aplicação (TTL não remove imediatamente).
- Conta bloqueada, excluída ou com perfil alterado perde o acesso na próxima requisição, mesmo com sessão antiga.
- CSRF habilitado, rotação de sessão após login, HttpOnly, SameSite=Lax, CSP e saída com remoção do cookie.
- Dois temas: `default` e `forest`.

## Preparar MongoDB Atlas

1. Crie um projeto e um cluster Atlas em sua própria conta.
2. Em Database Access, crie um usuário de banco exclusivo com `readWrite` apenas em `login_seguro`. Esse usuário não é a conta de login do aplicativo.
3. Em Network Access, autorize apenas o IP da sua máquina ou servidor. Evite liberar a internet inteira.
4. Em Connect > Drivers > Java, copie a URI `mongodb+srv`. Defina o banco `login_seguro` no caminho e mantenha `tls=true`. Não desative a validação de certificados.
5. Codifique caracteres reservados da senha na URI (percent-encoding, por exemplo `@` vira `%40`). Não publique a URI real.
6. A aplicação cria `users` e `sessions`, o índice único de e-mail e o índice TTL. O usuário de banco deve poder criar esses índices.

O arquivo `src/main/resources/application.yml` lê `MONGODB_URI` do ambiente e não possui credenciais embutidas. `.env.example` é um modelo, não uma conexão funcional. `.env` está ignorado pelo Git.

## Executar com Docker (recomendado)

Instale e inicie Docker Desktop com containers Linux.

```powershell
Copy-Item .env.example .env
# Edite .env: informe URI Atlas e credenciais iniciais fortes.
docker compose up --build
```

Abra http://localhost:8080. A imagem compila o projeto e executa os testes antes de iniciar. O serviço fica acessível apenas na máquina local. Encerre com `docker compose down`.

## Executar com Java e Maven

Requisitos: JDK 21, Maven 3.9+ e acesso à internet/Atlas.

```powershell
$env:MONGODB_URI='mongodb+srv://USUARIO:SENHA_CODIFICADA@CLUSTER.mongodb.net/login_seguro?retryWrites=true&w=majority&tls=true'
$env:BOOTSTRAP_ADMIN_EMAIL='seu-email@example.com'
$env:BOOTSTRAP_ADMIN_PASSWORD='DefinaUmaSenhaForte123'
$env:COOKIE_SECURE='false'
$env:APP_THEME='default'
mvn verify
mvn spring-boot:run
```

O Spring Boot **não carrega `.env` automaticamente**; use variáveis de ambiente para Maven. O Docker Compose carrega o arquivo pelo `env_file`.

Na primeira execução, o administrador é criado se o e-mail configurado ainda não existir. Se já existir uma conta comum com esse e-mail, ela não é promovida automaticamente. Use um e-mail inicial exclusivo. Remova as variáveis BOOTSTRAP após a criação (e do `.env`); não há senha padrão. Senha: 12 a 64 caracteres, maiúscula, minúscula, número e até 72 bytes UTF-8, limite do BCrypt.

Em produção, configure HTTPS no servidor/proxy e `COOKIE_SECURE=true`. Essa variável não ativa HTTPS; apenas restringe o envio do cookie a HTTPS. Não a habilite no teste HTTP local. Defina limites de tentativas de login no proxy antes de exposição pública; esta base acadêmica ainda não implementa rate limiting, MFA ou recuperação de senha.

## Estrutura

```text
src/main/java/br/edu/login/
  user/       entidade, roles, DTO validado, repositório e serviço
  security/   regras de acesso, BCrypt, verificação de conta e bootstrap
  session/    integração Spring Session + MongoDB, cookie e TTL
  web/        controladores e seleção segura de tema
src/main/resources/
  templates/  páginas Thymeleaf
  static/css/ estilo comum e temas
  application.yml
src/test/     testes de serviço, acesso, CSRF e persistência de sessão
.github/workflows/ci.yml
output/pdf/documentacao-login-seguro.pdf
```

A sessão usa Spring Session Core com `SessionRepository<MapSession>` próprio. Os atributos serializados são internos ao servidor; proteja a escrita no banco, pois a desserialização Java pressupõe banco confiável. Não permita que clientes escrevam documentos de sessão. Migrações que mudem classes serializadas devem invalidar sessões existentes. Dados de sessão contêm contexto de segurança e devem receber a mesma proteção que os dados de usuários.

## Rotas e perfis

| Rota | Acesso |
|---|---|
| GET/POST /register, GET/POST /login | Público (POST protegido por CSRF) |
| GET /dashboard | Autenticado |
| GET /manager | MANAGER ou ADMIN |
| GET /admin/users | ADMIN |
| POST /admin/users/{id} | ADMIN |
| POST /admin/users/{id}/delete | ADMIN |
| POST /logout | Encerramento com CSRF |

A autorização é aplicada no servidor. Ocultar links no Thymeleaf serve apenas à interface.

## Customização para o PFC

Altere `APP_THEME=forest` para trocar a paleta. Para adicionar tema, crie um CSS em `static/css/themes` e acrescente seu identificador à lista permitida de `ThemeAdvice`; entradas arbitrárias não viram caminhos de arquivos. Edite templates e textos sem mudar os serviços. Para novos recursos, crie pacotes de domínio, serviços e repositórios próprios e registre as regras de acesso em `SecurityConfig`. Preserve o cadastro com privilégio mínimo.

## Testes e validação

`mvn verify` executa testes sem precisar de credenciais Atlas. Eles validam hash, perfil inicial, excesso de bytes, negação de rotas, CSRF, templates, logout, revogação e round-trip/expiração/rotação do repositório de sessões com MongoTemplate simulado. O CI executa os mesmos testes. Esses testes não substituem integração real com Atlas.

Roteiro manual com Atlas: cadastrar uma conta, tentar duplicar e-mail, testar senha incorreta e correta, conferir `users.passwordHash`, conferir `sessions` e TTL, promover a conta a MANAGER no administrador e entrar novamente, verificar 403 para USER, bloquear usuário conectado e verificar revogação, fazer logout e verificar exclusão da sessão. Não inclua capturas com senhas/URIs na entrega.

## Gitflow e publicação no GitHub

Branches: `main` para versões entregues; `develop` para integração; `feature/*` para novas funcionalidades; `release/*` para preparação de versão; `hotfix/*` para correções urgentes. Integre features em develop com `--no-ff`; releases em main e develop, com tag. Não confunda a branch develop com uma prova de todo o processo: consulte o histórico real.

Repositório de entrega: https://github.com/Luizcorrea00/Atividade-Pratica-login (separado do PFC). Para vincular uma cópia local sem remoto:

```powershell
git remote add origin https://github.com/Luizcorrea00/Atividade-Pratica-login.git
git push -u origin main
git push -u origin develop
git push origin --tags
```

Para próximas features:

```powershell
git switch develop
git switch -c feature/nome-do-recurso
# implemente, teste, git add e git commit
git switch develop
git merge --no-ff feature/nome-do-recurso
```

Use pull requests e proteção das branches main/develop no GitHub. A configuração local não cria proteção remota automaticamente. Nunca faça `git add` de `.env` ou credenciais.

## Documentação e referências

O PDF inclui arquitetura, Atlas, decisões, adaptação e roteiro de testes. A capa identifica Luiz Antonio da Silva Correa, UMC, Sistema de Informação, Mogi das Cruzes. O gerador editável está em `docs/gerar_pdf.py` (Python com reportlab). Formatação baseada na NBR 14724:2024 segundo manual institucional, A4, margens superior/esquerda 3 cm, inferior/direita 2 cm, fonte 12 e entrelinha 1,5. Confirme exigências específicas da instituição.

- Spring: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- Spring Session: https://docs.spring.io/spring-session/reference/
- Spring Security: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/password-encoder.html
- MongoDB: https://www.mongodb.com/docs/manual/connection-string-options/
- Manual acadêmico: https://www.pucgoias.edu.br/mestrado-e-doutorado/wp-content/uploads/sites/27/2026/03/Normalizado2026-Final.pdf
