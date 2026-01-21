$response = Invoke-RestMethod -Uri "http://localhost:8080/api/generate/preview" -Method Post -ContentType "application/json" -InFile "temp_request.json"
$count = $response.files.Count
Write-Host "File Count: $count"
if ($count -eq 0) { Write-Host "No files returned!"; exit 1 }

# Check for User Entity augmentation
$userFile = $response.files | Where-Object { $_.path -like "*User.java" }
if ($userFile) {
    Write-Host "Found User.java"
    $userFile.content | Out-File "user_debug.txt" -Encoding utf8
    Write-Host "User.java content saved to user_debug.txt"
    if ($userFile.content -match "private String password") { Write-Host " - SUCCESS: Password field injected" } else { Write-Host " - FAIL: Password field MISSING" }
    if ($userFile.content -match "roles") { Write-Host " - SUCCESS: Roles relationship injected" } else { Write-Host " - FAIL: Roles relationship MISSING" }
} else { Write-Host "User.java NOT FOUND" }

# Check for Security Config Rules
$secConfigFile = $response.files | Where-Object { $_.path -like "*SecurityConfig.java" }
if ($secConfigFile) {
    Write-Host "Found SecurityConfig.java"
    if ($secConfigFile.content -match "requestMatchers.*products.*hasRole.*ADMIN") { Write-Host " - SUCCESS: Admin Rule for Products found" } else { Write-Host " - FAIL: Admin Rule for Products MISSING" }
}

# Check for other entities
if ($response.files | Where-Object { $_.path -like "*Product.java" }) { Write-Host "Found Product.java" }
if ($response.files | Where-Object { $_.path -like "*Order.java" }) { Write-Host "Found Order.java" }

# Check for RBAC Enums (Static Mode)
$permissionFile = $response.files | Where-Object { $_.path -like "*Permission.java" }
if ($permissionFile) {
    Write-Host "✅ SUCCESS: Found Permission.java (Static RBAC)"
    $permissionFile.content | Out-File "permission_debug.txt" -Encoding utf8
    if ($permissionFile.content -match "enum Permission") { Write-Host "  - Contains enum definition" }
    if ($permissionFile.content -match "USER_READ") { Write-Host "  - Contains USER_READ permission" }
} else {
    Write-Host "❌ FAIL: Permission.java NOT FOUND"
}

$roleFile = $response.files | Where-Object { $_.path -like "*Role.java" -and $_.path -like "*security*" }
if ($roleFile) {
    Write-Host "✅ SUCCESS: Found Role.java (Static RBAC)"
    $roleFile.content | Out-File "role_debug.txt" -Encoding utf8
    if ($roleFile.content -match "enum Role") { Write-Host "  - Contains enum definition" }
    if ($roleFile.content -match "ADMIN") { Write-Host "  - Contains ADMIN role" }
    if ($roleFile.content -match "Permission\.") { Write-Host "  - References Permission enum" }
} else {
    Write-Host "❌ FAIL: Role.java (enum) NOT FOUND"
}
