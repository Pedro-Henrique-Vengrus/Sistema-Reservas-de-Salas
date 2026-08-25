# 🏫 CampusFlow

Sistema acadêmico de reserva de salas e laboratórios — entrega NPI/UniFil.
**Spring Boot 3.3 + Java 21 + PostgreSQL 16** no backend, **React 18 + Vite** no frontend,
com interface **desktop/web-first** (tabelas densas, dashboards e painel administrativo completo).

> Autor: Pedro Henrique Vengrus — Projeto Acadêmico, UniFil

---

## 🎯 Regras de negócio implementadas

### Visibilidade setorizada (regra central)
`Usuário N:N Curso` e `Sala N:N Curso`. Um solicitante só **vê, lista e reserva** ambientes
ligados a pelo menos um dos seus cursos **ativos**. A regra é aplicada em um único ponto
(`VisibilidadeService`) e vale tanto para a listagem quanto para a criação de reservas e
para as propostas de troca — nunca depende de parâmetro enviado pelo cliente.

### Modos de reserva
| Modo | Comportamento |
|---|---|
| `GRADE_BIMESTRAL` | Só pode ser lançada com o **período da grade aberto**. Sem conflito de sala/horário, nasce `APROVADA`. |
| `ULTIMA_HORA` | Eventos e aulas extras. Sempre nasce `PENDENTE_APROVACAO` e entra na fila de moderação. |

O painel administrativo lança reservas em nome de terceiros já aprovadas, inclusive com a grade fechada.

### Troca de salas
Só vale entre reservas **aprovadas e futuras**. A justificativa é obrigatória, a troca é **mútua**
(cada um assume a reserva do outro) e todos os envolvidos são notificados a cada etapa.
Dia e turno decidem **quem aprova**:

| Situação | Fluxo |
|---|---|
| Mesmo dia **e** mesmo turno | O aceite do professor dono da reserva já efetiva a troca. |
| Dia **ou** turno diferente | Depois do aceite do professor, a troca fica `AGUARDANDO_GESTOR` e só se efetiva com o aval do Admin, na aba **Trocas** da Moderação. |

O sistema revalida o cenário antes de efetivar — reservas podem ter mudado entre o aceite e a decisão do
gestor. Propostas concorrentes envolvendo as mesmas reservas são invalidadas automaticamente.

### Avisos por e-mail (opt-in)
Todo aviso aparece no **sino** da aplicação. O e-mail no endereço acadêmico é um espelho
**opcional** do ciclo da troca de sala: proposta recebida, encaminhada ao gestor, aceita,
recusada ou cancelada. Três condições precisam ser verdadeiras para uma mensagem sair:

1. O envio está habilitado no ambiente (`EMAIL_ENABLED=true` + `SMTP_HOST`);
2. O usuário aderiu em **Preferências** (⚙ no cabeçalho) — ninguém recebe sem ativar;
3. O assunto é uma troca de sala.

O envio é assíncrono e nunca propaga erro: uma falha de SMTP não desfaz a troca nem
derruba a requisição. Sem SMTP configurado o sistema apenas registra em log o que enviaria.

```bash
# Exemplo de configuração
export EMAIL_ENABLED=true
export SMTP_HOST=smtp.unifil.br
export SMTP_PORT=587
export SMTP_USER=campusflow
export SMTP_PASSWORD=...
export EMAIL_FROM=nao-responda@campusflow.unifil.br
```

### Ciclo de vida e exclusão lógica
`ATIVO → INATIVO (soft-delete) → exclusão física`, para Curso, Sala e Usuário.
Ao inativar sala ou curso com reservas futuras ativas, o sistema **bloqueia** e devolve o impacto
(HTTP 409 com `detalhes`); o administrador confirma explicitamente (`?forcar=true`) e então as reservas
são canceladas e os solicitantes notificados. A exclusão física exige registro inativo e sem histórico.

### Perfis
Somente o **Admin** opera o painel administrativo. **Reitor e Professor têm a mesma visão**:
enxergam apenas os ambientes dos seus cursos, lançam reservas próprias e participam de trocas.

| Perfil | Painel administrativo | Solicita reservas | Visibilidade |
|---|---|---|---|
| `PROFESSOR` | — | ✅ | ambientes dos seus cursos |
| `REITOR` | — | ✅ | ambientes dos seus cursos |
| `ADMIN` | ✅ | — (lança em nome de terceiros) | catálogo completo |

---

## ✅ Pré-requisitos

- **Java 21** (`java -version`)
- **Node 18+** (`node -v`)
- **PostgreSQL 16** — ou Docker (recomendado)
- **Maven** (ou o wrapper `./mvnw`, se gerado)

## 🚀 Passo a passo

### 1. Subir o PostgreSQL

```bash
docker compose up -d
```

Cria o banco `campusflow` com usuário/senha `campusflow`/`campusflow` na porta 5432.
Sem Docker, crie manualmente:

```sql
CREATE DATABASE campusflow;
CREATE USER campusflow WITH PASSWORD 'campusflow';
GRANT ALL PRIVILEGES ON DATABASE campusflow TO campusflow;
```

### 2. Rodar o backend

```bash
cd backend
mvn spring-boot:run      # ou ./mvnw spring-boot:run
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui

O Flyway aplica as migrations **V1–V15** na primeira execução, deixando o banco pronto e vazio
(apenas a conta de Admin — ver *Primeiro acesso* abaixo).

### 3. Rodar os testes

```bash
cd backend && mvn test
```

48 testes de regra de negócio (JUnit 5 + Mockito, sem banco): modos de reserva, período da grade,
visibilidade setorizada, os dois caminhos da troca (direta e com aval do gestor), as condições do envio de e-mail, inativação forçada, separação de perfis e derivação de turno.

### 4. Rodar o frontend

```bash
cd frontend
npm install
npm run dev
```

App em http://localhost:5173 (o Vite faz proxy de `/api` para `localhost:8080`).

---

## 👥 Primeiro acesso

O banco nasce **vazio**: sem cursos, sem ambientes, sem reservas e com um único usuário — a conta
administrativa. Todo o resto é montado pelo Admin, no painel.

| E-mail | Senha | Perfil |
|---|---|---|
| `admin@campus.br` | `123` | ADMIN |

Ordem sugerida para colocar o sistema de pé:

1. **Cursos** — cadastre os cursos (nome + sigla). Nada mais funciona sem eles: a visibilidade
   setorizada é ancorada no curso.
2. **Ambientes** — cadastre as salas e laboratórios e **vincule cada um aos cursos** que podem
   enxergá-lo. Ambiente sem curso vinculado não aparece para nenhum solicitante.
3. **Usuários** — crie os professores e o reitor, atribuindo os cursos de cada um.
4. **Período da grade** — começa **fechado**. Enquanto estiver assim, os solicitantes só conseguem
   pedir reservas de última hora (que caem na moderação). Abra quando o preenchimento bimestral
   for liberado pelo edital.

> Trocar a senha do Admin no primeiro acesso é o recomendado — o hash que vem na migration é público.

---

## 🖥️ Telas

**Solicitante**
- **Painel** — KPIs pessoais, estado da grade e próxima semana.
- **Agenda** — grade **ambientes × horários** com filtros laterais; horário livre abre o formulário de reserva, horário ocupado abre o detalhe e a proposta de troca.
- **Ambientes** — catálogo filtrável dos ambientes visíveis ao perfil.
- **Minhas reservas** — tabela com abas Ativas/Histórico e cancelamento.
- **Trocas de sala** — propostas recebidas e enviadas, com detalhe lado a lado e cancelamento.

**Administração (somente ADMIN)**
- **Painel** — métricas de reservas, ocupação por ambiente e por curso.
- **Moderação** — duas abas: solicitações de última hora e trocas fora do mesmo dia/turno.
- **Usuários** — CRUD com exclusão lógica e atribuição de cursos.
- **Ambientes / Cursos** — CRUD com o ciclo de vida completo e diálogo de impacto.
- **Período da grade** — liberação do preenchimento bimestral, com vigência opcional.
- **Relatórios** — consulta filtrada e paginada + exportação CSV.

O sino do cabeçalho concentra as notificações (trocas, moderação, cancelamentos forçados).

---

## 🔌 Endpoints principais

| Método | Rota | Acesso |
|---|---|---|
| POST | `/api/auth/login` | público |
| GET | `/api/usuarios/me` · `/api/notificacoes/**` | autenticado |
| PUT | `/api/usuarios/me/preferencias` | autenticado (a própria adesão ao e-mail) |
| GET | `/api/salas` (filtros: `termo`, `tipo`, `cursoId`, `capacidadeMinima`, `status`) | autenticado (visibilidade setorizada) |
| GET | `/api/salas/tipos` · `/api/cursos` | autenticado |
| POST/PUT | `/api/salas` · `/api/cursos` | ADMIN |
| DELETE | `/api/salas/{id}?forcar=` · `/api/cursos/{id}?forcar=` | ADMIN (inativação) |
| DELETE | `/api/salas/{id}/permanente` · `/api/cursos/{id}/permanente` | ADMIN (exclusão física) |
| GET | `/api/salas/{id}/impacto` · `/api/cursos/{id}/impacto` | ADMIN |
| GET/POST/PUT/DELETE | `/api/usuarios/**` | ADMIN |
| GET | `/api/reservas/minhas` · `/api/reservas/agenda` · `/api/reservas/trocaveis` | autenticado |
| POST | `/api/reservas` | autenticado (valida visibilidade, conflito e modo) |
| DELETE | `/api/reservas/{id}` | dono da reserva ou ADMIN |
| GET | `/api/reservas/moderacao` · POST `/api/reservas/{id}/aprovar` · `/recusar` | ADMIN |
| GET | `/api/reservas` (busca filtrada e paginada) | ADMIN |
| GET/POST | `/api/propostas/**` (`aceitar`, `recusar`, `cancelar`) | autenticado |
| GET | `/api/propostas/moderacao` · POST `/api/propostas/{id}/gestor/aprovar` · `/recusar` | ADMIN |
| GET | `/api/periodo-grade` | autenticado |
| PUT | `/api/periodo-grade` | ADMIN |
| GET | `/api/relatorios/dashboard` · `/api/relatorios/reservas.csv` | ADMIN |

Erros seguem um formato único (`ApiError`): 401, **403** (visibilidade/perfil), 404,
**409** (conflito ou confirmação necessária, com `detalhes` do impacto), 422 (validação) e 500.

---

## 📂 Estrutura

```
campusflow/
├─ docker-compose.yml            # Postgres para dev
├─ backend/                      # Spring Boot 3.3 / Java 21
│  └─ src/
│     ├─ main/java/br/unifil/campusflow/
│     │  ├─ config/              # SecurityConfig (RBAC)
│     │  ├─ security/            # JWT, UsuarioLogado
│     │  ├─ domain/              # entidades + enums (Turno, StatusReserva, TipoReserva…)
│     │  ├─ dto/                 # records request/response
│     │  ├─ repository/          # Spring Data JPA
│     │  ├─ service/             # regras de negócio
│     │  ├─ controller/          # REST
│     │  └─ exception/           # handler global
│     ├─ main/resources/db/migration/   # Flyway V1–V15
│     └─ test/java/…/service/    # testes das regras críticas
└─ frontend/                     # React 18 + Vite
   └─ src/
      ├─ api/client.js           # fetch + token + erros tipados + download CSV
      ├─ auth/                   # AuthContext + ProtectedRoute
      ├─ components/             # AppShell, notificações
      │  └─ ui/                  # DataTable, Modal, Drawer, Toast, primitives
      ├─ lib/format.js           # formatação pt-BR
      ├─ pages/                  # Dashboard, Agenda, Ambientes, MinhasReservas, Trocas, Login
      │  └─ admin/               # Moderação, Usuários, Salas, Cursos, PeríodoGrade, Relatórios
      └─ styles/                 # tokens.css, base.css, components.css
```

---

## 🗄️ Modelo de dados

| Tabela | Papel |
|---|---|
| `tb_usuario` | nome, e-mail, senha (BCrypt), `role`, `status`, `receber_emails` |
| `tb_usuario_curso` | **N:N** usuário ↔ curso |
| `tb_curso` | nome, sigla, `status` |
| `tb_sala` | nome, código, `tipo` (catálogo), capacidade, andar, `status` |
| `tb_sala_curso` | **N:N** sala ↔ curso (visibilidade setorizada) |
| `tb_reserva` | solicitante, sala, data, horário, `turno`, `tipo_reserva`, `status`, observação |
| `tb_proposta_troca` | reserva desejada, reserva oferecida, justificativa, `status` |
| `tb_notificacao` | avisos in-app por destinatário |
| `tb_periodo_grade` | linha única com a flag de liberação da grade bimestral |
