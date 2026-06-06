# 🏫 CampusFlow

Sistema acadêmico de reserva de salas — entrega NPI/UniFil.
Esta base entrega **dois casos de uso completos** (Gerenciar Salas e Gerenciar Cursos)
em full-stack: **Spring Boot 3.3 + Java 21 + PostgreSQL** no backend e **React + Vite** no frontend.

> Autor: Pedro Henrique Vengrus — Projeto Acadêmico, UniFil

---

## 🧱 O que já está pronto nesta base

- **Auth JWT stateless** (access token de 1h) com login real contra o banco.
- **RBAC**: leitura para qualquer autenticado, escrita (CRUD) só para `ADMIN`/`GESTOR`.
- **Curso**: CRUD completo.
- **Sala**: CRUD completo + **vínculo N:N com Curso** (visibilidade setorizada).
- **Flyway** V1–V3 (schema + seed com os usuários de teste).
- **Swagger/OpenAPI** em `/swagger-ui`.
- **Tratamento padrão de erros** (401/403/404/409/422/500).
- **Frontend**: login + painel admin com as duas telas de CRUD, guard de rota e interceptor HTTP.

### Credenciais de teste (senha = `123`)
| Email | Papel |
|---|---|
| pedro@campus.br | PROFESSOR |
| joao@campus.br | COORDENADOR |
| admin@campus.br | ADMIN |

> Login funciona com qualquer usuário, mas as telas de CRUD exigem `ADMIN`/`GESTOR`.
> Entre como **admin@campus.br** para ver os dois casos de uso.

---

## ✅ Pré-requisitos

- **Java 21** (`java -version`)
- **Node 18+** (`node -v`)
- **PostgreSQL 16** — ou Docker (recomendado, mais simples)
- **Git**

---

## 🚀 Passo a passo (do zero)

### 1. Subir o PostgreSQL

**Opção A — Docker (recomendado):**
```bash
docker compose up -d
```
Isso cria o banco `campusflow` com usuário/senha `campusflow`/`campusflow` na porta 5432.

**Opção B — Postgres instalado localmente:**
```sql
CREATE DATABASE campusflow;
CREATE USER campusflow WITH PASSWORD 'campusflow';
GRANT ALL PRIVILEGES ON DATABASE campusflow TO campusflow;
```
Se usar credenciais diferentes, ajuste `backend/src/main/resources/application.yml`.

### 2. Gerar o Maven Wrapper (uma vez só)

Na primeira vez, dentro de `backend/`, gere os scripts do wrapper:
```bash
cd backend
mvn wrapper:wrapper
```
> Se você **não** tem o `mvn` instalado, instale o Maven uma vez (`sdk install maven` ou via gerenciador do SO),
> rode o comando acima, e depois disso o `./mvnw` funciona sozinho.

### 3. Rodar o backend
```bash
cd backend
./mvnw spring-boot:run
```
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui

O Flyway cria as tabelas e insere os dados de teste automaticamente na primeira execução.

### 4. Rodar o frontend
```bash
cd frontend
npm install
npm run dev
```
- App: http://localhost:5173

O Vite faz proxy de `/api` para `localhost:8080`, então não há problema de CORS em dev.

---

## 🔌 Endpoints principais

| Método | Rota | Acesso |
|---|---|---|
| POST | `/api/auth/login` | público |
| GET | `/api/cursos` | autenticado |
| POST/PUT/DELETE | `/api/cursos` | ADMIN/GESTOR |
| GET | `/api/salas` | autenticado |
| GET | `/api/salas?cursoId={id}` | autenticado (visibilidade setorizada) |
| POST/PUT/DELETE | `/api/salas` | ADMIN/GESTOR |
| GET | `/api/reservas/minhas` | autenticado |
| GET | `/api/reservas/outros` | autenticado |
| GET | `/api/reservas/agenda?salaId=&data=` | autenticado |
| POST | `/api/reservas` | autenticado (bloqueia conflito de horário) |
| DELETE | `/api/reservas/{id}` | autenticado (só o dono) |
| GET | `/api/propostas/recebidas` · `/enviadas` | autenticado |
| POST | `/api/propostas` | autenticado (justificativa obrigatória) |
| POST | `/api/propostas/{id}/aceitar` · `/recusar` | dono da reserva |

### Telas (frontend)
- **Login** → entra e redireciona para Agenda
- **Agenda** → fluxo data → sala → horário; slot ocupado abre modal de proposta de troca
- **Salas** → cards das salas visíveis ao curso
- **Minhas Reservas** → suas reservas (cancelar) + reservas de outros (propor troca)
- **Propostas** → abas Recebidas (aceitar/recusar) e Enviadas, com badge de pendentes
- **Admin** → só para ADMIN/GESTOR; gerencia Salas e Cursos. O admin também navega em todas as abas de solicitante.

---

## 📂 Estrutura

```
campusflow/
├─ docker-compose.yml      # Postgres para dev
├─ backend/                # Spring Boot 3.3 / Java 21
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/br/unifil/campusflow/
│     │  ├─ config/        # SecurityConfig
│     │  ├─ security/      # JWT (service, filtro, userdetails)
│     │  ├─ domain/        # Curso, Sala, Usuario, Role
│     │  ├─ dto/           # records request/response
│     │  ├─ repository/    # Spring Data JPA
│     │  ├─ service/       # regras de negócio
│     │  ├─ controller/    # Auth, Curso, Sala
│     │  └─ exception/     # handler global
│     └─ resources/
│        ├─ application.yml
│        └─ db/migration/  # Flyway V1, V2, V3
└─ frontend/               # React + Vite
   └─ src/
      ├─ api/client.js     # fetch + interceptor de token
      ├─ auth/             # AuthContext + ProtectedRoute
      ├─ components/       # Topbar, GerenciarSalas, GerenciarCursos
      └─ pages/            # Login, AdminPanel
```

---

## 📤 Subir no GitHub

```bash
# na raiz campusflow/
git init
git add .
git commit -m "feat: base CampusFlow - casos de uso Sala e Curso (full-stack)"

# crie um repo vazio no GitHub (sem README), copie a URL, e:
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/campusflow.git
git push -u origin main
```

> O `.gitignore` já exclui `target/`, `node_modules/` e arquivos de IDE.

---

## 🗒️ Notas sobre os diagramas

Esta base usa **Sala N:N Curso** (tabela `tb_sala_curso`), que é o que sustenta a
visibilidade setorizada da regra de negócio. Seus diagramas de **classe** e **ER** ainda
mostram `cursoId` como FK única na sala e não trazem a entidade de vínculo — vale atualizá-los
para refletir o N:N e incluir o `Curso` no diagrama de classes (que hoje está só no ER).
