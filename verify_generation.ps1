# ============================================================================
# verify_generation.ps1 - End-to-End Verification for Spring Code Generator
# ============================================================================
# Phases:
#   1. Build & start the backend generator
#   2. Generate project from temp_request.json (download ZIP)
#   3. Extract ZIP to demo/
#   4. Build the generated project (mvn compile)
#   5. Run JUnit tests (mvn test with dev profile / H2)
#   6. Start the generated project
#   7. Run E2E tests via Invoke-RestMethod
#   8. Report results & cleanup
# ============================================================================

param(
    [string]$BackendDir = "backend",
    [string]$DemoDir = "demo",
    [string]$RequestFile = "temp_request.json",
    [int]$BackendPort = 8080,
    [int]$DemoPort = 8081,
    [switch]$SkipBackendBuild,
    [switch]$SkipGenerate
)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $ScriptDir) { $ScriptDir = Get-Location }
Set-Location $ScriptDir

# ============================================================================
# Globals
# ============================================================================
$BackendProcess = $null
$DemoProcess = $null
$Pass = 0
$Fail = 0
$Results = @()

# ============================================================================
# Helper Functions
# ============================================================================
function Write-Phase($phase, $message) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  PHASE $phase : $message" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Write-Step($message) {
    Write-Host "  -> $message" -ForegroundColor Gray
}

function Write-Ok($message) {
    Write-Host "  [PASS] $message" -ForegroundColor Green
}

function Write-Err($message) {
    Write-Host "  [FAIL] $message" -ForegroundColor Red
}

function Test-Result($name, $condition) {
    if ($condition) {
        $script:Pass++
        $script:Results += [PSCustomObject]@{ Test = $name; Status = "PASS" }
        Write-Ok $name
    } else {
        $script:Fail++
        $script:Results += [PSCustomObject]@{ Test = $name; Status = "FAIL" }
        Write-Err $name
    }
}

function Wait-ForHealthCheck($url, $maxRetries = 30, $delaySeconds = 2) {
    Write-Step "Waiting for $url to be ready..."
    for ($i = 1; $i -le $maxRetries; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
            if ($resp.StatusCode -eq 200) {
                Write-Ok "Service is ready (attempt $i/$maxRetries)"
                return $true
            }
        } catch {
            # Try alternative health indicators
            try {
                $resp = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 5 -UseBasicParsing -SkipHttpErrorCheck -ErrorAction SilentlyContinue
                if ($resp -and $resp.StatusCode -lt 500) {
                    Write-Ok "Service is ready (attempt $i/$maxRetries)"
                    return $true
                }
            } catch {}
        }
        Write-Host "    Attempt $i/$maxRetries - waiting ${delaySeconds}s..." -ForegroundColor DarkGray
        Start-Sleep -Seconds $delaySeconds
    }
    Write-Err "Service did not become ready after $maxRetries attempts"
    return $false
}

function Cleanup {
    Write-Host ""
    Write-Host "Cleaning up..." -ForegroundColor Yellow
    if ($script:DemoProcess -and !$script:DemoProcess.HasExited) {
        Write-Step "Stopping demo project (PID: $($script:DemoProcess.Id))"
        try {
            Stop-Process -Id $script:DemoProcess.Id -Force -ErrorAction SilentlyContinue
            # Also kill any child java processes
            Get-Process -Name "java" -ErrorAction SilentlyContinue |
                Where-Object { $_.Id -ne $script:BackendProcess.Id } |
                Stop-Process -Force -ErrorAction SilentlyContinue
        } catch {}
    }
    if ($script:BackendProcess -and !$script:BackendProcess.HasExited) {
        Write-Step "Stopping backend generator (PID: $($script:BackendProcess.Id))"
        try {
            Stop-Process -Id $script:BackendProcess.Id -Force -ErrorAction SilentlyContinue
        } catch {}
    }
}

# ============================================================================
# PHASE 1: Build & Start Backend Generator
# ============================================================================
Write-Phase 1 "Build & Start Backend Generator"

try {
    if (-not $SkipBackendBuild) {
        Write-Step "Building backend with Maven (mvn clean package -DskipTests)..."
        $buildResult = & mvn -f "$BackendDir/pom.xml" clean package -DskipTests 2>&1
        $buildExitCode = $LASTEXITCODE
        Test-Result "Backend Maven build" ($buildExitCode -eq 0)
        if ($buildExitCode -ne 0) {
            Write-Err "Backend build failed. Last output:"
            $buildResult | Select-Object -Last 20 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkRed }
            throw "Backend build failed"
        }
    } else {
        Write-Step "Skipping backend build (--SkipBackendBuild)"
    }

    # Find the JAR file
    $jarFile = Get-ChildItem "$BackendDir/target/*.jar" -Exclude "*-sources.jar","*-javadoc.jar" | Select-Object -First 1
    if (-not $jarFile) {
        throw "No JAR file found in $BackendDir/target/"
    }
    Write-Step "Found JAR: $($jarFile.Name)"

    # Start backend
    Write-Step "Starting backend on port $BackendPort..."
    $script:BackendProcess = Start-Process -FilePath "java" -ArgumentList "-jar", $jarFile.FullName `
        -PassThru -NoNewWindow -RedirectStandardOutput "$ScriptDir/backend_stdout.log" -RedirectStandardError "$ScriptDir/backend_stderr.log"
    Write-Step "Backend PID: $($script:BackendProcess.Id)"

    # Wait for backend to be ready
    $backendReady = Wait-ForHealthCheck "http://localhost:$BackendPort/api/dependencies/recommended"
    Test-Result "Backend started successfully" $backendReady
    if (-not $backendReady) { throw "Backend failed to start" }

# ============================================================================
# PHASE 2: Generate Project from temp_request.json
# ============================================================================
    Write-Phase 2 "Generate Project (POST to /api/generate/project)"

    if (-not $SkipGenerate) {
        Write-Step "Sending request from $RequestFile..."
        $requestBody = Get-Content $RequestFile -Raw
        $zipPath = "$ScriptDir/generated_project.zip"

        try {
            Invoke-RestMethod -Uri "http://localhost:$BackendPort/api/generate/project" `
                -Method POST `
                -ContentType "application/json" `
                -Body $requestBody `
                -OutFile $zipPath `
                -TimeoutSec 60
            $zipExists = Test-Path $zipPath
            $zipSize = if ($zipExists) { (Get-Item $zipPath).Length } else { 0 }
            Test-Result "Project ZIP generated ($([math]::Round($zipSize/1024, 1)) KB)" ($zipExists -and $zipSize -gt 1000)
        } catch {
            Test-Result "Project ZIP generated" $false
            Write-Err "Error: $($_.Exception.Message)"
            throw "Project generation failed"
        }

# ============================================================================
# PHASE 3: Extract ZIP to Demo Directory
# ============================================================================
        Write-Phase 3 "Extract ZIP to $DemoDir/"

        # Clean demo directory (robust Windows deletion with retry)
        if (Test-Path $DemoDir) {
            Write-Step "Removing existing $DemoDir/..."
            for ($delRetry = 0; $delRetry -lt 5; $delRetry++) {
                Remove-Item -Recurse -Force $DemoDir -ErrorAction SilentlyContinue
                Start-Sleep -Milliseconds 500
                if (-not (Test-Path $DemoDir)) { break }
                Write-Step "  Retry delete $DemoDir ($($delRetry+1)/5)..."
                # Try cmd.exe fallback
                cmd /c "rd /s /q `"$DemoDir`"" 2>$null
                Start-Sleep -Seconds 1
            }
        }
        Remove-Item "$ScriptDir/temp_extract" -Recurse -Force -ErrorAction SilentlyContinue

        Write-Step "Extracting ZIP..."
        Expand-Archive -Path $zipPath -DestinationPath "$ScriptDir/temp_extract" -Force

        # The ZIP contains a project folder inside - find and move it
        $extractedDirs = Get-ChildItem "$ScriptDir/temp_extract" -Directory
        if ($extractedDirs.Count -eq 1) {
            if (Test-Path $DemoDir) {
                # If demo dir still exists (Windows lock), copy contents into it
                Copy-Item -Path "$($extractedDirs[0].FullName)\*" -Destination $DemoDir -Recurse -Force
            } else {
                Move-Item $extractedDirs[0].FullName $DemoDir
            }
        } else {
            if (Test-Path $DemoDir) {
                Copy-Item -Path "$ScriptDir/temp_extract\*" -Destination $DemoDir -Recurse -Force
            } else {
                Move-Item "$ScriptDir/temp_extract" $DemoDir
            }
        }
        Remove-Item "$ScriptDir/temp_extract" -Recurse -Force -ErrorAction SilentlyContinue

        # Wait until the extracted project is fully present and stable (pom.xml exists and files settled)
        $maxAttempts = 30
        $attempt = 0
        $pomPath = "$DemoDir/pom.xml"
        $prevTotalBytes = -1
        $stableCounter = 0
        $stableRequired = 3

        Write-Step "Waiting for extracted project files to appear and stabilize..."
        while ($attempt -lt $maxAttempts) {
            $attempt++
            if (Test-Path $pomPath) {
                # compute total size of files under demo (in bytes)
                try {
                    $items = Get-ChildItem -Path $DemoDir -Recurse -File -ErrorAction Stop
                    $totalBytes = ($items | Measure-Object -Property Length -Sum).Sum
                } catch {
                    $totalBytes = 0
                }

                # If filesize hasn't changed for several iterations, consider extraction stable
                if ($totalBytes -eq $prevTotalBytes -and $totalBytes -gt 1024) {
                    $stableCounter++
                } else {
                    $stableCounter = 0
                }
                $prevTotalBytes = $totalBytes

                Write-Step "Attempt $attempt/$maxAttempts - pom.xml found; total bytes=$totalBytes; stableCounter=$stableCounter"

                if ($stableCounter -ge $stableRequired) {
                    Write-Ok "Demo project extracted and files stabilized"
                    break
                }
            } else {
                Write-Step "Attempt $attempt/$maxAttempts - pom.xml not found yet"
            }

            Start-Sleep -Seconds 1
        }

        if (-not (Test-Path $pomPath)) {
            Test-Result "Demo project extracted (pom.xml exists)" $false
            throw "Extraction failed - no pom.xml found after waiting"
        }

        if ($stableCounter -lt $stableRequired) {
            Test-Result "Demo project extracted (files stabilized)" $false
            throw "Extraction did not stabilize in time"
        }

        Test-Result "Demo project extracted (pom.xml exists and files stabilized)" $true
    } else {
        Write-Step "Skipping generation (--SkipGenerate)"
        Test-Result "Demo project exists" (Test-Path "$DemoDir/pom.xml")
    }

    # Stop backend - we need port 8080 free
    Write-Step "Stopping backend generator..."
    if ($script:BackendProcess -and !$script:BackendProcess.HasExited) {
        Stop-Process -Id $script:BackendProcess.Id -Force -ErrorAction SilentlyContinue
    }
    # Also kill any remaining java processes from the backend
    Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3

    # ============================================================================
    # PHASE 4: Build Generated Project
    # ============================================================================
    Write-Phase 4 "Build Generated Project (mvn compile)"

    Write-Step "Compiling demo project..."
    $compileResult = & mvn -f "$DemoDir/pom.xml" clean compile 2>&1
    $compileExitCode = $LASTEXITCODE
    Test-Result "Demo project compiles" ($compileExitCode -eq 0)
    if ($compileExitCode -ne 0) {
        Write-Err "Compilation errors:"
        $compileResult | Select-String "ERROR" | Select-Object -First 15 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkRed }
    }

    # ============================================================================
    # PHASE 5: Run JUnit Tests (dev profile with H2)
    # ============================================================================
    Write-Phase 5 "Run JUnit Tests (mvn test -Dspring.profiles.active=dev)"

    Write-Step "Running unit tests with H2 in-memory database..."
    $testResult = & mvn -f "$DemoDir/pom.xml" test "-Dspring.profiles.active=dev" 2>&1
    $testExitCode = $LASTEXITCODE

    # Extract test summary
    $testSummary = $testResult | Select-String "Tests run:"
    if ($testSummary) {
        $lastSummary = $testSummary | Select-Object -Last 1
        Write-Step "Test Summary: $lastSummary"
    }
    Test-Result "JUnit tests pass" ($testExitCode -eq 0)
    if ($testExitCode -ne 0) {
        Write-Err "Test failures:"
        $testResult | Select-String "FAIL|ERROR|Tests run:" | Select-Object -Last 10 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkRed }
    }

    # ============================================================================
    # PHASE 6: Start Generated Project
    # ============================================================================
    Write-Phase 6 "Start Generated Project"

    Write-Step "Starting demo project on port $DemoPort (dev profile with H2)..."
    $script:DemoProcess = Start-Process -FilePath "mvn" `
        -ArgumentList "-f", "$DemoDir/pom.xml", "spring-boot:run", "-Dspring-boot.run.profiles=dev", "-Dspring-boot.run.arguments=--server.port=$DemoPort" `
        -PassThru -NoNewWindow `
        -RedirectStandardOutput "$ScriptDir/demo_stdout.log" `
        -RedirectStandardError "$ScriptDir/demo_stderr.log"
    Write-Step "Demo PID: $($script:DemoProcess.Id)"

    $demoReady = Wait-ForHealthCheck "http://localhost:$DemoPort/v3/api-docs" 40 3
    Test-Result "Demo project started successfully" $demoReady
    if (-not $demoReady) {
        Write-Err "Demo stderr (last 20 lines):"
        if (Test-Path "$ScriptDir/demo_stderr.log") {
            Get-Content "$ScriptDir/demo_stderr.log" | Select-Object -Last 20 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkRed }
        }
        throw "Demo project failed to start"
    }

    # ============================================================================
    # PHASE 7: E2E Tests
    # ============================================================================
    Write-Phase 7 "E2E Tests (REST API)"

    $baseUrl = "http://localhost:$DemoPort"
    $headers = @{ "Content-Type" = "application/json" }

    # --- Auth: Register ---
    Write-Step "Testing Auth: Register..."
    try {
        $registerBody = @{
            username = "testuser@test.com"
            password = "Test123!"
            email    = "testuser@test.com"
        } | ConvertTo-Json
        $registerResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -Body $registerBody -ContentType "application/json" -TimeoutSec 10
        $hasAccessToken = $null -ne $registerResp.accessToken -and $registerResp.accessToken.Length -gt 10
        Test-Result "Auth: Register user" $hasAccessToken
    } catch {
        Test-Result "Auth: Register user" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- Auth: Login ---
    Write-Step "Testing Auth: Login..."
    $authToken = $null
    $refreshToken = $null
    try {
        $loginBody = @{
            username = "testuser@test.com"
            password = "Test123!"
        } | ConvertTo-Json
        $loginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 10
        $authToken = $loginResp.accessToken
        $refreshToken = $loginResp.refreshToken
        $hasToken = $null -ne $authToken -and $authToken.Length -gt 10
        Test-Result "Auth: Login user" $hasToken
        if ($loginResp.roles) {
            Write-Step "  Roles returned: $($loginResp.roles -join ', ')"
        }
    } catch {
        Test-Result "Auth: Login user" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    $authHeaders = @{
        "Content-Type"  = "application/json"
        "Authorization" = "Bearer $authToken"
    }

    # --- Auth: Token Refresh ---
    Write-Step "Testing Auth: Token Refresh..."
    try {
        $refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
        $refreshResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/refresh" -Method POST -Body $refreshBody -ContentType "application/json" -TimeoutSec 10
        Test-Result "Auth: Token refresh" ($null -ne $refreshResp.accessToken)
    } catch {
        Test-Result "Auth: Token refresh" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- Admin API: Login as Admin (needed for CRUD tests requiring admin permissions) ---
    Write-Step "Testing Admin API: Login as admin..."
    $adminToken = $null
    try {
        $adminLoginBody = @{
            username = "admin@admin.com"
            password = "admin123"
        } | ConvertTo-Json
        $adminLoginResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $adminLoginBody -ContentType "application/json" -TimeoutSec 10
        $adminToken = $adminLoginResp.accessToken
        $hasAdminToken = $null -ne $adminToken -and $adminToken.Length -gt 10
        Test-Result "Admin: Login as admin" $hasAdminToken
        if ($adminLoginResp.roles) {
            Write-Step "  Admin roles: $($adminLoginResp.roles -join ', ')"
        }
    } catch {
        Test-Result "Admin: Login as admin" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    $adminHeaders = @{
        "Content-Type"  = "application/json"
        "Authorization" = "Bearer $adminToken"
    }

    # --- CRUD: Category (CATEGORY_WRITE requires ADMIN) ---
    Write-Step "Testing CRUD: Category..."
    $categoryId = $null
    try {
        $catBody = @{ name = "Electronics" } | ConvertTo-Json
        $catResp = Invoke-RestMethod -Uri "$baseUrl/api/categorys" -Method POST -Body $catBody -Headers $adminHeaders -TimeoutSec 10
        $categoryId = $catResp.id
        Test-Result "CRUD: Create Category (admin)" ($null -ne $categoryId)
    } catch {
        Test-Result "CRUD: Create Category (admin)" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # List categories with user auth (CATEGORY_READ is allowed for USER)
    try {
        $catGetResp = Invoke-RestMethod -Uri "$baseUrl/api/categorys" -Method GET -Headers $authHeaders -TimeoutSec 10
        $catCount = if ($catGetResp.content) { $catGetResp.content.Count } else { 0 }
        Test-Result "CRUD: List Categories (user auth, count: $catCount)" ($catCount -gt 0)
    } catch {
        Test-Result "CRUD: List Categories (user auth)" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- CRUD: Product (GET is permitAll, POST/PUT/DELETE require ADMIN) ---
    Write-Step "Testing CRUD: Product..."

    # GET products - should work without auth (PERMIT_ALL)
    try {
        $productsResp = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method GET -ContentType "application/json" -TimeoutSec 10
        Test-Result "CRUD: List Products (no auth, PERMIT_ALL)" ($null -ne $productsResp)
    } catch {
        Test-Result "CRUD: List Products (no auth, PERMIT_ALL)" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- CRUD: Order (requires AUTHENTICATED) ---
    Write-Step "Testing CRUD: Order..."
    $orderId = $null
    try {
        $orderBody = @{
            status    = "PENDING"
            createdAt = "2025-01-01T12:00:00"
        } | ConvertTo-Json
        $orderResp = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method POST -Body $orderBody -Headers $authHeaders -TimeoutSec 10
        $orderId = $orderResp.id
        Test-Result "CRUD: Create Order (authenticated)" ($null -ne $orderId)
    } catch {
        Test-Result "CRUD: Create Order (authenticated)" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    try {
        $ordersResp = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method GET -Headers $authHeaders -TimeoutSec 10
        $orderCount = if ($ordersResp.content) { $ordersResp.content.Count } else { 0 }
        Test-Result "CRUD: List Orders (count: $orderCount)" ($orderCount -gt 0)
    } catch {
        Test-Result "CRUD: List Orders" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- Admin API: Role Management ---
    Write-Step "Testing Admin API: Role Management..."
    try {
        $rolesResp = Invoke-RestMethod -Uri "$baseUrl/api/admin/roles" -Method GET -Headers $adminHeaders -TimeoutSec 10
        $roleCount = $rolesResp.Count
        Test-Result "Admin: List roles (count: $roleCount)" ($roleCount -ge 2)
    } catch {
        Test-Result "Admin: List roles" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- Admin API: Create Role ---
    $newRoleId = $null
    try {
        $roleBody = @{
            name        = "MODERATOR"
            description = "Moderator role"
            permissions = @("USER_READ", "PRODUCT_READ")
        } | ConvertTo-Json
        $createRoleResp = Invoke-RestMethod -Uri "$baseUrl/api/admin/roles" -Method POST -Body $roleBody -Headers $adminHeaders -TimeoutSec 10
        $newRoleId = $createRoleResp.id
        Test-Result "Admin: Create MODERATOR role" ($null -ne $newRoleId)
    } catch {
        Test-Result "Admin: Create MODERATOR role" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- Admin API: Update Role ---
    if ($newRoleId) {
        try {
            $updateBody = @{
                name        = "MODERATOR"
                description = "Updated moderator role"
                permissions = @("USER_READ", "PRODUCT_READ", "ORDER_READ")
            } | ConvertTo-Json
            $updateRoleResp = Invoke-RestMethod -Uri "$baseUrl/api/admin/roles/$newRoleId" -Method PUT -Body $updateBody -Headers $adminHeaders -TimeoutSec 10
            Test-Result "Admin: Update MODERATOR role" ($updateRoleResp.description -eq "Updated moderator role")
        } catch {
            Test-Result "Admin: Update MODERATOR role" $false
            Write-Err "  Error: $($_.Exception.Message)"
        }
    }

    # --- Admin API: Get User Roles ---
    try {
        $userRolesResp = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/1/roles" -Method GET -Headers $adminHeaders -TimeoutSec 10
        Test-Result "Admin: Get user roles" ($null -ne $userRolesResp)
    } catch {
        Test-Result "Admin: Get user roles" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- Admin API: Create Product (requires ADMIN role) ---
    try {
        $productBody = @{
            name  = "Laptop"
            price = 999.99
            stock = 50
        } | ConvertTo-Json
        $productResp = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method POST -Body $productBody -Headers $adminHeaders -TimeoutSec 10
        Test-Result "Admin: Create Product (ADMIN role)" ($null -ne $productResp.id)
    } catch {
        Test-Result "Admin: Create Product (ADMIN role)" $false
        Write-Err "  Error: $($_.Exception.Message)"
    }

    # --- Admin API: Delete Role ---
    if ($newRoleId) {
        try {
            Invoke-RestMethod -Uri "$baseUrl/api/admin/roles/$newRoleId" -Method DELETE -Headers $adminHeaders -TimeoutSec 10
            Test-Result "Admin: Delete MODERATOR role" $true
        } catch {
            # 204 No Content might throw, check status
            if ($_.Exception.Response.StatusCode.value__ -eq 204) {
                Test-Result "Admin: Delete MODERATOR role" $true
            } else {
                Test-Result "Admin: Delete MODERATOR role" $false
                Write-Err "  Error: $($_.Exception.Message)"
            }
        }
    }

    # ============================================================================
    # PHASE 8: Report
    # ============================================================================
    Write-Phase 8 "Results Summary"

} catch {
    Write-Err "Script terminated: $($_.Exception.Message)"
} finally {
    Cleanup
}

# Final Report
Write-Host ""
Write-Host "============================================" -ForegroundColor White
Write-Host "  TEST RESULTS" -ForegroundColor White
Write-Host "============================================" -ForegroundColor White
Write-Host ""

foreach ($r in $Results) {
    $color = if ($r.Status -eq "PASS") { "Green" } else { "Red" }
    $icon = if ($r.Status -eq "PASS") { "[PASS]" } else { "[FAIL]" }
    Write-Host "  $icon $($r.Test)" -ForegroundColor $color
}

Write-Host ""
Write-Host "--------------------------------------------" -ForegroundColor White
Write-Host "  TOTAL: $($Pass + $Fail) | PASS: $Pass | FAIL: $Fail" -ForegroundColor $(if ($Fail -eq 0) { "Green" } else { "Yellow" })
Write-Host "--------------------------------------------" -ForegroundColor White
Write-Host ""

# Exit with code
if ($Fail -gt 0) {
    Write-Host "Some tests failed!" -ForegroundColor Red
    exit 1
} else {
    Write-Host "All tests passed!" -ForegroundColor Green
    exit 0
}
