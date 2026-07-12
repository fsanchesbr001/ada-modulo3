param(
    [switch]$SkipInfra,
    [switch]$SkipTests,
    [switch]$KeepAppRunning,
    [switch]$FixSmokeCredentials,
    [int]$MySqlHostPort = 3307,
    [string]$Username = "admin",
    [string]$Password = "password"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Wait-Until {
    param(
        [scriptblock]$Condition,
        [int]$TimeoutSeconds,
        [string]$FailureMessage,
        [int]$IntervalSeconds = 2
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (& $Condition) {
            return
        }
        Start-Sleep -Seconds $IntervalSeconds
    }

    throw $FailureMessage
}

function Invoke-Checked {
    param(
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$Description
    )

    Write-Step $Description
    & $FilePath @ArgumentList
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed ($LASTEXITCODE): $FilePath $($ArgumentList -join ' ')"
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$jdbcUrl = "jdbc:mysql://localhost:$MySqlHostPort/db_auth"
Push-Location $repoRoot

$gatewayProcess = $null
$logSuffix = Get-Date -Format "yyyyMMdd-HHmmss"
$gatewayLog = Join-Path $repoRoot "apps/api-gateway/target/gateway-smoke-$logSuffix.log"
$gatewayErrLog = Join-Path $repoRoot "apps/api-gateway/target/gateway-smoke-$logSuffix.err.log"
$smokePasswordHash = '$2a$10$EsgmD6NEGgFoenx.nkSYfuFoGAYoe1qPhwNfejqg.y6RCrUW/iD7y'

try {
    if (-not $SkipInfra) {
        Invoke-Checked -FilePath "docker" -ArgumentList @("compose", "-f", "infra/docker/docker-compose.yml", "up", "-d", "mysql", "redis", "rabbitmq", "zookeeper", "kafka", "prometheus", "grafana") -Description "Subindo infraestrutura local (docker compose)"

        Write-Step "Aguardando MySQL aceitar conexoes"
        Wait-Until -TimeoutSeconds 120 -FailureMessage "MySQL nao ficou pronto em 120s" -Condition {
            & docker exec pix-mysql mysqladmin ping -h 127.0.0.1 -uroot -proot --silent *> $null
            return $LASTEXITCODE -eq 0
        }
    }

    if (-not $SkipTests) {
        Invoke-Checked -FilePath ".\mvnw.cmd" -ArgumentList @("test", "-Dspring.datasource.url=$jdbcUrl") -Description "Executando testes Maven"
    }

    Write-Step "Instalando modulos para smoke test"
    Invoke-Checked -FilePath ".\mvnw.cmd" -ArgumentList @("-q", "-pl", "apps/api-gateway", "-am", "install", "-DskipTests") -Description "Instalando modulos locais no repositorio Maven"

    Write-Step "Iniciando api-gateway para smoke test"
    $gatewayProcess = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList @("-q", "-f", "apps/api-gateway/pom.xml", "spring-boot:run", "-Dspring-boot.run.arguments=--spring.datasource.url=$jdbcUrl") -PassThru -RedirectStandardOutput $gatewayLog -RedirectStandardError $gatewayErrLog

    Write-Step "Aguardando endpoint de health"
    Wait-Until -TimeoutSeconds 120 -FailureMessage "api-gateway nao respondeu health em 120s" -Condition {
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 5
            return $health.status -eq "UP"
        }
        catch {
            return $false
        }
    }

    if ($FixSmokeCredentials) {
        Write-Step "Normalizando credenciais de smoke no banco"
        & docker exec pix-mysql mysql -uroot -proot -e "INSERT INTO db_auth.auth_user (id, username, password_hash, enabled) VALUES (1, 'admin', '$smokePasswordHash', TRUE) ON DUPLICATE KEY UPDATE username=VALUES(username), password_hash=VALUES(password_hash), enabled=VALUES(enabled);"
        if ($LASTEXITCODE -ne 0) {
            throw "Falha ao normalizar credenciais de smoke no MySQL"
        }
    }
    else {
        Write-Step "Modo estrito: sem alteracao de credenciais de banco"
    }

    Write-Step "Executando smoke test de login"
    $loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $loginBody -TimeoutSec 15

    if ($response.StatusCode -ne 200) {
        throw "Login retornou status inesperado: $($response.StatusCode)"
    }

    $authorizationHeader = $response.Headers["Authorization"]
    if (-not $authorizationHeader -or -not $authorizationHeader.StartsWith("Bearer ")) {
        throw "Header Authorization ausente ou invalido no login"
    }

    $payload = $response.Content | ConvertFrom-Json
    if ($payload.tokenType -ne "Bearer" -or $payload.expiresIn -ne "1200") {
        throw "Payload de login inesperado: $($response.Content)"
    }

    Write-Host "`nVALIDACAO_OK" -ForegroundColor Green
    Write-Host "Authorization: $authorizationHeader"
}
finally {
    if ($gatewayProcess -and -not $KeepAppRunning) {
        Write-Step "Encerrando api-gateway"
        Stop-Process -Id $gatewayProcess.Id -Force -ErrorAction SilentlyContinue
    }
    Pop-Location
}
