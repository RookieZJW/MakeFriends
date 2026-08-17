# ============================================================
# 搭伴 (rokbj.me) - 一键启动所有服务
# ============================================================
# 使用方法：
#   1. 右键 PowerShell → 以管理员身份运行
#   2. cd 到 deploy 目录
#   3. 执行：.\start-all.ps1
#
# 启动顺序：Redis → MySQL → 后端 → Nginx → Cloudflare Tunnel
# ============================================================

$ErrorActionPreference = "Continue"

# ============================================================
# ⚙️ 路径配置 - 如果路径不同，在这里改！
# ============================================================
$REDIS_DIR    = "D:\Redis-8.6.4"
$NGINX_DIR    = "D:\nginx"
$BACKEND_DIR  = "E:\TraePorject\make-friends\make-friends-backend"
$BACKEND_JAR  = "target\make-friends-backend-1.0.0.jar"
$JAVA_EXE     = "D:\JDK\bin\java.exe"
$TUNNEL_NAME  = "rokbj"

# 窗口标题前缀
$TITLE_PREFIX = "[搭伴]"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  搭伴 (rokbj.me) - 一键启动" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# 1. Redis
# ============================================================
Write-Host "[1/5] 启动 Redis..." -ForegroundColor Yellow

$redisRunning = Get-Process redis-server -ErrorAction SilentlyContinue
if ($redisRunning) {
    Write-Host "  ✅ Redis 已经在运行 (PID: $($redisRunning.Id))" -ForegroundColor Green
} else {
    try {
        Start-Process -FilePath "$REDIS_DIR\redis-server.exe" `
            -WorkingDirectory $REDIS_DIR `
            -WindowStyle Minimized
        Start-Sleep -Seconds 2

        $redisRunning = Get-Process redis-server -ErrorAction SilentlyContinue
        if ($redisRunning) {
            Write-Host "  ✅ Redis 启动成功 (PID: $($redisRunning.Id))" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️ Redis 进程未检测到，请手动检查" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ❌ Redis 启动失败: $_" -ForegroundColor Red
    }
}

# ============================================================
# 2. MySQL
# ============================================================
Write-Host ""
Write-Host "[2/5] 检查 MySQL..." -ForegroundColor Yellow

$mysqlService = Get-Service | Where-Object { $_.Name -like "*mysql*" -or $_.DisplayName -like "*mysql*" } | Select-Object -First 1

if (-not $mysqlService) {
    Write-Host "  ⚠️ 未找到 MySQL 服务，请确认已安装并手动启动" -ForegroundColor Red
} else {
    Write-Host "  发现 MySQL 服务: $($mysqlService.Name) ($($mysqlService.DisplayName))"
    if ($mysqlService.Status -eq "Running") {
        Write-Host "  ✅ MySQL 已经在运行" -ForegroundColor Green
    } else {
        Write-Host "  正在启动 MySQL..."
        try {
            Start-Service $mysqlService.Name
            Start-Sleep -Seconds 5
            $mysqlService.Refresh()
            if ($mysqlService.Status -eq "Running") {
                Write-Host "  ✅ MySQL 启动成功" -ForegroundColor Green
            } else {
                Write-Host "  ⚠️ MySQL 启动失败，请手动启动" -ForegroundColor Red
            }
        } catch {
            Write-Host "  ❌ MySQL 启动失败: $_" -ForegroundColor Red
        }
    }
}

# ============================================================
# 3. 后端 Spring Boot
# ============================================================
Write-Host ""
Write-Host "[3/5] 启动后端 Spring Boot (端口 8080)..." -ForegroundColor Yellow

# 检查 8080 端口是否被占用
$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($port8080) {
    $javaProc = Get-Process -Id $port8080.OwningProcess -ErrorAction SilentlyContinue
    Write-Host "  ✅ 8080 端口已有进程 (PID: $($port8080.OwningProcess), 名称: $($javaProc.ProcessName))" -ForegroundColor Green
} else {
    # 检查 jar 是否存在
    $jarPath = Join-Path $BACKEND_DIR $BACKEND_JAR
    if (-not (Test-Path $jarPath)) {
        Write-Host "  ❌ 未找到 Jar 包: $jarPath" -ForegroundColor Red
        Write-Host "     请先执行 Maven 打包: cd $BACKEND_DIR ; D:\apache-maven-3.9.14\bin\mvn.cmd clean package -DskipTests" -ForegroundColor Red
    } else {
        try {
            $javaArgs = "-Xms256m", "-Xmx512m", "-jar", "target\make-friends-backend-1.0.0.jar"
            Start-Process -FilePath $JAVA_EXE `
                -ArgumentList $javaArgs `
                -WorkingDirectory $BACKEND_DIR `
                -WindowStyle Normal
            Write-Host "  ⏳ 正在启动后端 (PID 见新窗口)..." -ForegroundColor Yellow
            Write-Host "     等待 15 秒后验证..." -ForegroundColor DarkGray

            $maxWait = 20
            $waited = 0
            while ($waited -lt $maxWait) {
                Start-Sleep -Seconds 2
                $waited += 2
                $check = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
                if ($check) {
                    Write-Host "  ✅ 后端启动成功，8080 端口已监听" -ForegroundColor Green
                    break
                }
                Write-Host "     等待中... ($waited/$maxWait 秒)" -ForegroundColor DarkGray
            }
            if ($waited -ge $maxWait) {
                Write-Host "  ⚠️ 后端仍在启动中，如未成功请查看后端窗口日志" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "  ❌ 后端启动失败: $_" -ForegroundColor Red
        }
    }
}

# ============================================================
# 4. Nginx
# ============================================================
Write-Host ""
Write-Host "[4/5] 启动 Nginx (端口 80)..." -ForegroundColor Yellow

$port80 = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue
$nginxProc = Get-Process nginx -ErrorAction SilentlyContinue

if ($nginxProc -and $port80) {
    Write-Host "  ✅ Nginx 已经在运行 (进程数: $($nginxProc.Count), 80 端口监听中)" -ForegroundColor Green
} else {
    try {
        if ($port80 -and -not $nginxProc) {
            Write-Host "  ⚠️ 80 端口被其他进程占用 (PID: $($port80.OwningProcess))" -ForegroundColor Red
        } else {
            # 如果 Nginx 已在运行但端口没监听，先 stop 再 start
            if ($nginxProc) {
                Write-Host "  Nginx 进程存在但端口未监听，重载..."
                & "$NGINX_DIR\nginx.exe" -s reload 2>$null
                Start-Sleep -Seconds 2
            } else {
                Start-Process -FilePath "$NGINX_DIR\nginx.exe" `
                    -WorkingDirectory $NGINX_DIR `
                    -WindowStyle Hidden
                Start-Sleep -Seconds 2
            }
            $nginxProc = Get-Process nginx -ErrorAction SilentlyContinue
            $port80 = Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue
            if ($nginxProc -and $port80) {
                Write-Host "  ✅ Nginx 启动成功 (进程数: $($nginxProc.Count), 80 端口监听中)" -ForegroundColor Green
            } else {
                Write-Host "  ⚠️ Nginx 启动异常，手动执行检查：" -ForegroundColor Yellow
                Write-Host "     cd $NGINX_DIR ; .\nginx.exe -t ; .\nginx.exe" -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "  ❌ Nginx 启动失败: $_" -ForegroundColor Red
    }
}

# ============================================================
# 5. Cloudflare Tunnel
# ============================================================
Write-Host ""
Write-Host "[5/5] 启动 Cloudflare Tunnel ($TUNNEL_NAME)..." -ForegroundColor Yellow

$cfProc = Get-Process cloudflared -ErrorAction SilentlyContinue
if ($cfProc) {
    Write-Host "  ✅ cloudflared 已在运行 (PID: $($cfProc.Id))" -ForegroundColor Green
} else {
    try {
        Start-Process -FilePath "cloudflared" `
            -ArgumentList "tunnel", "run", $TUNNEL_NAME `
            -WindowStyle Normal
        Write-Host "  ⏳ 正在启动 cloudflared tunnel（新窗口）" -ForegroundColor Yellow
        Write-Host "     看到 'Connection 1 registered' 和 'Tunnel started' 即为成功" -ForegroundColor DarkGray
        Start-Sleep -Seconds 3
    } catch {
        Write-Host "  ❌ cloudflared 启动失败: $_" -ForegroundColor Red
        Write-Host "     请确认已安装 cloudflared 并执行过 cloudflared tunnel login" -ForegroundColor Red
    }
}

# ============================================================
# 总结
# ============================================================
Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  🎉 启动命令执行完毕" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📌 本机访问:    http://localhost" -ForegroundColor White
Write-Host "🌍 外网访问:    https://rokbj.me" -ForegroundColor White
Write-Host ""
Write-Host "💡 查看状态:    .\status-all.ps1" -ForegroundColor Gray
Write-Host "💡 一键停止:    .\stop-all.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "⚠️ 注意：cloudflared / 后端 / Redis 的窗口不要关闭！" -ForegroundColor Yellow
Write-Host "   否则服务就断了" -ForegroundColor Yellow
Write-Host ""
