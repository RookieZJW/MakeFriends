# MakeFriends 交友平台 - 一键启动脚本
# 使用方法：在 PowerShell 中右键 -> 使用 PowerShell 运行

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   MakeFriends 交友平台 - 启动中..."    -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 MySQL 服务
$mysql = Get-Service MySQL80 -ErrorAction SilentlyContinue
if ($mysql.Status -ne "Running") {
    Write-Host "[1/4] 正在启动 MySQL 服务..." -ForegroundColor Yellow
    Start-Service MySQL80
    Start-Sleep 2
} else {
    Write-Host "[1/4] ✅ MySQL 服务已运行" -ForegroundColor Green
}

# 检查后端端口
$portCheck = netstat -ano | findstr ":8080"
if ($portCheck) {
    Write-Host "[2/4] ⚠️  后端端口 8080 已被占用，跳过后端启动" -ForegroundColor Yellow
} else {
    Write-Host "[2/4] 正在启动后端服务 (Spring Boot)..." -ForegroundColor Yellow
    $backDir = "E:\TraePorject\make-friends\make-friends-backend"
    Start-Process powershell -WindowStyle Normal -ArgumentList @(
        "-NoExit", "-Command", @"
cd '$backDir'
`$env:JAVA_HOME = 'D:\JDK'
`$env:MAVEN_HOME = 'D:\apache-maven-3.9.14'
`$env:PATH = "`$env:JAVA_HOME\bin;`$env:MAVEN_HOME\bin;`$env:PATH"
Write-Host '=== 后端启动中... (等待约 10 秒) ===' -ForegroundColor Cyan
mvn spring-boot:run -q
"@
    )
    Start-Sleep 3
    Write-Host "[2/4] ✅ 后端正在启动，请稍候..." -ForegroundColor Green
}

# 检查前端端口
$portCheck2 = netstat -ano | findstr ":5173"
if ($portCheck2) {
    Write-Host "[3/4] ⚠️  前端端口 5173 已被占用，跳过前端启动" -ForegroundColor Yellow
} else {
    Write-Host "[3/4] 正在启动前端服务 (Vue3)..." -ForegroundColor Yellow
    $frontDir = "E:\TraePorject\make-friends\make-friends-frontend"
    Start-Process powershell -WindowStyle Normal -ArgumentList @(
        "-NoExit", "-Command", @"
cd '$frontDir'
Write-Host '=== 前端启动中... ===' -ForegroundColor Cyan
npm run dev
"@
    )
    Write-Host "[3/4] ✅ 前端正在启动..." -ForegroundColor Green
}

Start-Sleep 2

Write-Host ""
Write-Host "[4/4] ✅ 启动完成！" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   访问地址"                                 -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "   🌐 前端页面:  http://localhost:5173/"      -ForegroundColor Yellow
Write-Host "   🔧 后端接口:  http://localhost:8080/api/"  -ForegroundColor Yellow
Write-Host "   📖 接口文档:  http://localhost:8080/api/swagger-ui.html" -ForegroundColor Yellow
Write-Host ""
Write-Host "   📱 测试账号:  13800000001" -ForegroundColor Magenta
Write-Host "   🔑 密码:      Test123456" -ForegroundColor Magenta
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   提示：请勿关闭此窗口"                    -ForegroundColor Cyan
Write-Host "   关闭后端/前端窗口即可停止服务"              -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 自动打开浏览器
Start-Process "http://localhost:5173/"