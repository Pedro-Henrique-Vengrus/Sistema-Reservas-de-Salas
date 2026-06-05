<h1 align="center">🏫 Sistema de Reservas de Salas</h1>

<p align="center">
  Sistema acadêmico para gestão inteligente de ambientes institucionais.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Frontend-React-61DAFB?logo=react&logoColor=black" />
  <img src="https://img.shields.io/badge/Database-PostgreSQL-336791?logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Auth-JWT-black" />
  <img src="https://img.shields.io/badge/Migrations-Flyway-CC0200" />
</p>

---

## 📖 Sobre o Projeto

Este sistema foi criado para substituir planilhas compartilhadas na gestão acadêmica de salas, laboratórios e auditórios.

Ele permite:

- ✅ Reservas com data/hora de início e término
- ✅ Bloqueio automático de sobreposição
- ✅ Fluxo formal de aprovação
- ✅ Histórico auditável
- ✅ Geração de PDF oficial

---

## 🏗️ Arquitetura

### 🔹 Backend
- Spring Boot 3.3+
- Spring Security 6
- JWT Stateless
- Spring Data JPA
- Flyway
- OpenAPI / Swagger

### 🔹 Frontend
- React + Vite
- React Router
- Axios / Fetch
- Proteção de rotas

### 🔹 Banco de Dados
- PostgreSQL
- Migrations versionadas
- Índices estratégicos

---

## 🔄 Fluxo do Sistema

1️⃣ Usuário solicita reserva  
2️⃣ Sistema valida conflito de horário  
3️⃣ Gestor aprova ou rejeita  
4️⃣ Reserva é oficializada  
5️⃣ Log completo fica registrado  

---

## 🚀 Como Rodar o Projeto

### 🔹 Backend

```bash
./mvnw spring-boot:run

Acesse:

http://localhost:8080
http://localhost:8080/swagger-ui
🔹 Frontend
npm install
npm run dev

Acesse:

http://localhost:5173
🔐 Segurança

JWT com access token curto

Controle por roles (ADMIN, GESTOR, COORDENADOR, PROFESSOR)

Tratamento padrão de erros

🎯 Objetivo Acadêmico

Projeto desenvolvido como entrega de CRUD completo utilizando stack profissional moderna.

👨‍💻 Autor

Pedro Henrique Vengrus
Projeto Acadêmico – UNIFIL
