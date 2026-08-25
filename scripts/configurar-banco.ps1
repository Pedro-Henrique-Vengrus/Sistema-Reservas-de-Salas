# Configura o PostgreSQL local para o CampusFlow (Windows / PowerShell).
#
# Descobre onde o Postgres foi instalado, liga o servico se estiver parado, cria o banco
# e o usuario que a aplicacao espera, e mostra o estado atual. Pode rodar quantas vezes
# quiser: se ja estiver tudo certo, ele so relata e nao altera nada.
#
#   .\scripts\configurar-banco.ps1
#
# Sem acentos de proposito: o PowerShell 5.1 do Windows le arquivo sem BOM como ANSI
# e embaralharia os acentos na tela.

function Titulo($t) { Write-Host "`n=== $t ===" -ForegroundColor Cyan }
function Ok($t)     { Write-Host "  [ok] $t"   -ForegroundColor Green }
function Aviso($t)  { Write-Host "  [!] $t"    -ForegroundColor Yellow }
function Erro($t)   { Write-Host "  [x] $t"    -ForegroundColor Red }

# psql devolve nada quando a consulta nao tem linhas -- chamar .Trim() direto no retorno
# estoura justamente no caso que mais interessa aqui (o usuario/banco ainda nao existe).
function Consulta($banco, $sql) {
    $saida = & psql -U postgres -h localhost -d $banco -tAc $sql 2>$null
    if ($null -eq $saida) { return '' }
    return ([string]($saida | Select-Object -First 1)).Trim()
}

function Executa($banco, $sql) {
    & psql -U postgres -h localhost -d $banco -q -c $sql 2>$null | Out-Null
    return ($LASTEXITCODE -eq 0)
}

# --------------------------------------------------------------- 1. Onde esta o Postgres
Titulo '1. Procurando o PostgreSQL'

if ($env:PSQL -and (Test-Path $env:PSQL)) {
    $psql = Get-Item $env:PSQL
} else {
    $psql = Get-ChildItem 'C:\Program Files\PostgreSQL' -Recurse -Filter 'psql.exe' -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1
}

if (-not $psql) {
    Erro 'psql.exe nao encontrado em C:\Program Files\PostgreSQL.'
    Aviso 'O PostgreSQL provavelmente nao esta instalado. Para instalar:'
    Write-Host '      winget install -e --id PostgreSQL.PostgreSQL.16'
    Aviso 'Se voce instalou em outro lugar, aponte o caminho e rode de novo:'
    Write-Host '      $env:PSQL="D:\caminho\bin\psql.exe"'
    exit 1
}

$bin = Split-Path $psql.FullName
Ok "encontrado: $($psql.FullName)"
$env:Path = "$bin;$env:Path"

# --------------------------------------------------------------- 2. O servico esta de pe
Titulo '2. Servico do PostgreSQL'

$svc = Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $svc) {
    Aviso 'nenhum servico "postgresql*" registrado -- seguindo assim mesmo'
} elseif ($svc.Status -ne 'Running') {
    Aviso "$($svc.Name) esta $($svc.Status). Tentando iniciar..."
    try {
        Start-Service $svc.Name -ErrorAction Stop
        Ok 'servico iniciado'
    } catch {
        Erro 'nao consegui iniciar (precisa de administrador).'
        Write-Host '      Abra o PowerShell como administrador e rode:'
        Write-Host "      Start-Service $($svc.Name)"
        exit 1
    }
} else {
    Ok "$($svc.Name) rodando"
}

# --------------------------------------------------------------- 3. Entrar no servidor
Titulo '3. Conectando como usuario postgres'

# Primeiro tenta sem senha: em instalacao com pg_hba em "trust", ou com o %APPDATA%\postgresql\pgpass.conf
# preenchido, nem precisa perguntar nada.
$env:PGPASSWORD = ''
$conectou = (Consulta 'postgres' 'SELECT 1') -eq '1'

if (-not $conectou) {
    Write-Host '  (a senha foi definida quando voce instalou o PostgreSQL)'
    $senha = Read-Host '  Senha do usuario postgres'
    $env:PGPASSWORD = $senha
    $conectou = (Consulta 'postgres' 'SELECT 1') -eq '1'
}

if (-not $conectou) {
    Erro 'nao consegui conectar no servidor.'
    Aviso 'Se voce nao lembra a senha, da para redefinir liberando o acesso local:'
    Write-Host "      1. Abra como administrador o arquivo pg_hba.conf (fica na pasta data da instalacao)"
    Write-Host '      2. Troque scram-sha-256 por trust nas linhas host e local'
    if ($svc) { Write-Host "      3. Reinicie como administrador: Restart-Service $($svc.Name)" }
    Write-Host '      4. psql -U postgres -c "ALTER USER postgres PASSWORD ''novasenha''"'
    Write-Host '      5. Desfaca o passo 2 e reinicie de novo'
    exit 1
}

Ok 'conectado'
$versao = Consulta 'postgres' 'SHOW server_version'
if ($versao) { Ok "PostgreSQL $versao" }

# --------------------------------------------------------------- 4. Usuario e banco
Titulo '4. Usuario e banco da aplicacao'

if ((Consulta 'postgres' "SELECT 1 FROM pg_roles WHERE rolname='campusflow'") -eq '1') {
    Ok 'usuario campusflow ja existe'
} elseif (Executa 'postgres' "CREATE USER campusflow WITH PASSWORD 'campusflow'") {
    Ok 'usuario campusflow criado'
} else {
    Erro 'falhou ao criar o usuario campusflow'; exit 1
}

if ((Consulta 'postgres' "SELECT 1 FROM pg_database WHERE datname='campusflow'") -eq '1') {
    Ok 'banco campusflow ja existe'
} elseif (Executa 'postgres' 'CREATE DATABASE campusflow OWNER campusflow') {
    Ok 'banco campusflow criado (vazio)'
} else {
    Erro 'falhou ao criar o banco campusflow'; exit 1
}

# No Postgres 15+ o schema public deixou de dar CREATE para todo mundo.
if (Executa 'campusflow' 'GRANT ALL ON SCHEMA public TO campusflow') {
    Ok 'permissoes do schema public concedidas'
}

# --------------------------------------------------------------- 5. Como esta o banco
Titulo '5. Estado atual do banco'

$temHistorico = Consulta 'campusflow' `
    "SELECT 1 FROM information_schema.tables WHERE table_name='flyway_schema_history'"

if ($temHistorico -ne '1') {
    Aviso 'banco ainda vazio -- o Flyway cria as tabelas quando o backend subir pela primeira vez'
} else {
    $ultima = Consulta 'campusflow' `
        'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1'
    Ok "migrations aplicadas ate a V$ultima (o projeto vai ate a V15)"
    if ($ultima -ne '15') {
        Aviso 'as migrations novas serao aplicadas sozinhas na proxima vez que o backend subir'
    }

    Write-Host ''
    & psql -U postgres -h localhost -d campusflow -c @"
SELECT 'cursos'    AS tabela, count(*) FROM tb_curso
UNION ALL SELECT 'ambientes',  count(*) FROM tb_sala
UNION ALL SELECT 'usuarios',   count(*) FROM tb_usuario
UNION ALL SELECT 'reservas',   count(*) FROM tb_reserva
UNION ALL SELECT 'propostas',  count(*) FROM tb_proposta_troca
ORDER BY 1
"@
}

$env:PGPASSWORD = ''

# --------------------------------------------------------------- 6. Proximo passo
Titulo 'Pronto -- proximo passo'
Write-Host '  Backend:   cd backend   ; .\mvnw.cmd spring-boot:run'
Write-Host '  Frontend:  cd frontend  ; npm.cmd install ; npm.cmd run dev'
Write-Host ''
Write-Host '  Depois abra http://localhost:5173 e entre com admin@campus.br / 123'
Write-Host ''
Write-Host '  Para espiar o banco a qualquer momento:'
Write-Host '      $env:PGPASSWORD="campusflow"; psql -U campusflow -h localhost -d campusflow'
Write-Host '      \dt   lista as tabelas       \q   sai'
