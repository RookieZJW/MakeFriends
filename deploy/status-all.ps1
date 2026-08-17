# ============================================================
# 搭伴 (rokbj.me) - 查看所有服务状态
# ============================================================
# 使用方法：
#   1. 右键 PowerShell → 以管理员身份运行
#   2. cd 到 deploy 目录
#   3. 执行：.\status-all.ps1
# ============================================================

Write-Host ""
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host "  搭伴 (rokbj.me) - 服务状态检查" -ForegroundColor Magenta
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host ""

$okCount   = 0
$warnCount = 0
$errCount  = 0

function Write-StatusLine($name, $ok, $info) {
    $global:okCount, $global:warnCount, $global:errCount = $script:okCount, $script:warnCount, $script:errCount
    if ($ok -eq "OK") {
        Write-Host "  ✅ $name : " -ForegroundColor Green -NoNewline
        Write-Host $info
        $script:okCount++
    } elseif ($ok -eq "WARN") {
        Write-Host "  ⚠️  $name : " -ForegroundColor Yellow -NoNewline
        Write-Host $info
        $script:warnCount++
    } else {
        Write-Host "  ❌ $name : " -ForegroundColor Red -NoNewline
        Write-Host $info
        $script:errCount++
    }
}

# ============================================================
# 1. 进程检查
# ============================================================
Write-Host "【进程状态】" -ForegroundColor White

$redis = Get-Process redis-server -ErrorAction SilentlyContinue
if ($redis) {
    $mem = [math]::Round($redis.WorkingSet64 / 1MB, 1)
    Write-StatusLine "Redis        " "OK" "PID: $($redis.Id), 内存: $mem MB"
} else {
    Write-StatusLine "Redis        " "ERR" "未运行 (启动命令: cd D:\Redis-8.6.4 ; .\redis-server.exe)"
}

$mysqlService = Get-Service | Where-Object { $_.Name -like "*mysql*" -or $_.DisplayName -like "*mysql*" } | Select-Object -First 1
if ($mysqlService) {
    if ($mysqlService.Status -eq "Running") {
        Write-StatusLine "MySQL        " "OK" "服务: $($mysqlService.Name), 运行中"
    } else {
        Write-StatusLine "MySQL        " "ERR" "服务: $($mysqlService.Name), 已停止 (执行: Start-Service $($mysqlService.Name))"
    }
} else {
    Write-StatusLine "MySQL        " "ERR" "未找到 MySQL 服务"
}

$nginx = Get-Process nginx -ErrorAction SilentlyContinue
if ($nginx) {
    $count = @($nginx).Count
    Write-StatusLine "Nginx        " "OK" "进程数: $count"
} else {
    Write-StatusLine "Nginx        " "ERR" "未运行 (启动命令: cd D:\nginx ; .\nginx.exe)"
}

$java = Get-Process java -ErrorAction SilentlyContinue
if ($java) {
    $mem = [math]::Round(($java | Measure-Object WorkingSet64 -Sum).Sum / 1MB, 0)
    $count = @($java).Count
    Write-StatusLine "Java (后端)  " "OK" "进程数: $count, 总内存: $mem MB"
} else {
    Write-StatusLine "Java (后端)  " "ERR" "未运行 (启动命令见 .\start-all.ps1)"
}

$cf = Get-Process cloudflared -ErrorAction SilentlyContinue
if ($cf) {
    $mem = [math]::Round($cf.WorkingSet64 / 1MB, 1)
    Write-StatusLine "Cloudflared  " "OK" "PID: $($cf.Id), 内存: $mem MB"
} else {
    Write-StatusLine "Cloudflared  " "ERR" "未运行 (启动命令: cloudflared tunnel run rokbj)"
}

# ============================================================
# 2. 端口监听检查
# ============================================================
Write-Host ""
Write-Host "【端口监听】" -ForegroundColor White

$checkPorts = @{
    "3306 (MySQL)"  = 3306
    "6379 (Redis)"  = 6379
    "8080 (后端)"   = 8080
    "80   (Nginx)"  = 80
}

foreach ($label in $checkPorts.Keys) {
    $port = $checkPorts[$label]
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $ips = ($conn | Select-Object -ExpandProperty LocalAddress -Unique) -join ", "
        Write-StatusLine "端口 $label" "OK" "监听中 ($ips)"
    } else {
        Write-StatusLine "端口 $label" "ERR" "未监听"
    }
}

# ============================================================
# 3. 本地可访问性检查 (HTTP)
# ============================================================
Write-Host ""
Write-Host "【本地 HTTP 检查】" -ForegroundColor White

try {
    $r = Invoke-WebRequest -Uri "http://localhost/" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-StatusLine "http://localhost/" "OK" "状态码 $($r.StatusCode)"
} catch {
    $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { "连接失败" }
    Write-StatusLine "http://localhost/" "ERR" "$status"
}

try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($r) {
        Write-StatusLine "后端健康检查" "OK" "状态码 $($r.StatusCode), 内容: $($r.Content.Substring(0, [Math]::Min(60, $r.Content.Length)))"
    } else {
        # 健康检查接口不一定配置，尝试访问 /api/dict/list
        try {
            $r2 = Invoke-WebRequest -Uri "http://localhost:8080/api/dict/hobbies" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            Write-StatusLine "后端 API 测试" "OK" "/api/dict/hobbies 返回 $($r2.StatusCode)"
        } catch {
            Write-StatusLine "后端 API 测试" "WARN" "后端健康检查接口未配置，跳过"
        }
    }
} catch {
    Write-StatusLine "后端 API 检查" "WARN" "异常: $($_.Exception.Message.Substring(0,50))"
}

# ============================================================
# 4. 文件检查
# ============================================================
Write-Host ""
Write-Host "【关键文件】" -ForegroundColor White

$nginxConf = "D:\nginx\conf\nginx.conf"
if (Test-Path $nginxConf) {
    $time = (Get-Item $nginxConf).LastWriteTime.ToString("yyyy-MM-dd HH:mm")
    Write-StatusLine "nginx.conf   " "OK" "$nginxConf (修改时间: $time)"
} else {
    Write-StatusLine "nginx.conf   " "ERR" "文件不存在: $nginxConf"
}

$jarPath = "E:\TraePorject\make-friends\make-friends-backend\target\make-friends-backend-1.0.0.jar"
if (Test-Path $jarPath) {
    $size = [math]::Round((Get-Item $jarPath).Length / 1MB, 1)
    $time = (Get-Item $jarPath).LastWriteTime.ToString("yyyy-MM-dd HH:mm")
    Write-StatusLine "后端 Jar 包  " "OK" "$size MB, 构建时间: $time"
} else {
    Write-StatusLine "后端 Jar 包  " "ERR" "未找到: $jarPath (请先打包)"
}

$frontendDist = "E:\TraePorject\make-friends\make-friends-frontend\dist\index.html"
if (Test-Path $frontendDist) {
    $time = (Get-Item $frontendDist).LastWriteTime.ToString("yyyy-MM-dd HH:mm")
    Write-StatusLine "前端 dist/   " "OK" "index.html 修改时间: $time"
} else {
    Write-StatusLine "前端 dist/   " "ERR" "未找到: $frontendDist (请先 npm run build)"
}

$cfConfig = "$env:USERPROFILE\.cloudflared\config.yml"
if (Test-Path $cfConfig) {
    Write-StatusLine "cf config.yml" "OK" $cfConfig
} else {
    Write-StatusLine "cf config.yml" "ERR" "未找到: $cfConfig"
}

# ============================================================
# 总结
# ============================================================
Write-Host ""
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host "  检查总结" -ForegroundColor Magenta
Write-Host "==============================================" -ForegroundColor Magenta
Write-Host ""

Write-Host "  正常: $okCount 项  " -ForegroundColor Green -NoNewline
Write-Host "  警告: $warnCount 项  " -ForegroundColor Yellow -NoNewline
Write-Host "  错误: $errCount 项" -ForegroundColor Red
Write-Host ""

if ($errCount -eq 0) {
    Write-Host "🎉 所有服务正常！" -ForegroundColor Green
    Write-Host ""
    Write-Host "📌 本机访问:  http://localhost" -ForegroundColor White
    Write-Host "🌍 外网访问:  https://rokbj.me" -ForegroundColor White
} elseif ($warnCount -eq 0) {
    Write-Host "⚠️ 有 $errCount 个错误，请启动对应服务后重试" -ForegroundColor Yellow
    Write-Host "   一键启动: .\start-all.ps1" -ForegroundColor White
} else {
    Write-Host "⚠️ 有 $errCount 个错误 + $warnCount 个警告" -ForegroundColor Yellow
}
Write-Host ""
