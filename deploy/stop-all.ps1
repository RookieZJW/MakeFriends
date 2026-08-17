# ============================================================
# 搭伴 (rokbj.me) - 一键停止所有服务
# ============================================================
# 使用方法：
#   1. 右键 PowerShell → 以管理员身份运行
#   2. cd 到 deploy 目录
#   3. 执行：.\stop-all.ps1
#
# 停止顺序：Tunnel → Nginx → 后端 → Redis
# MySQL 不停止（系统服务，日常用得上）
# ============================================================

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Yellow
Write-Host "  搭伴 (rokbj.me) - 一键停止" -ForegroundColor Yellow
Write-Host "==============================================" -ForegroundColor Yellow
Write-Host ""

# ============================================================
# 1. Cloudflare Tunnel
# ============================================================
Write-Host "[1/4] 停止 Cloudflare Tunnel..." -ForegroundColor Cyan

$cfProcs = Get-Process cloudflared -ErrorAction SilentlyContinue
if (-not $cfProcs) {
    Write-Host "  ✅ cloudflared 未在运行" -ForegroundColor Gray
} else {
    $count = @($cfProcs).Count
    try {
        Stop-Process -Name cloudflared -Force -ErrorAction Stop
        Start-Sleep -Milliseconds 500
        $remain = Get-Process cloudflared -ErrorAction SilentlyContinue
        if (-not $remain) {
            Write-Host "  ✅ cloudflared 已停止 (终止 $count 个进程)" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️ 仍有 $($remain.Count) 个 cloudflared 进程残留，重试..." -ForegroundColor Yellow
            Stop-Process -Name cloudflared -Force -ErrorAction SilentlyContinue
        }
    } catch {
        Write-Host "  ❌ 停止失败: $_" -ForegroundColor Red
    }
}

# ============================================================
# 2. Nginx
# ============================================================
Write-Host ""
Write-Host "[2/4] 停止 Nginx..." -ForegroundColor Cyan

$nginxDir = "D:\nginx"
$nginxProcs = Get-Process nginx -ErrorAction SilentlyContinue

if (-not $nginxProcs) {
    Write-Host "  ✅ Nginx 未在运行" -ForegroundColor Gray
} else {
    try {
        # 先尝试优雅退出
        if (Test-Path "$nginxDir\nginx.exe") {
            Write-Host "  执行 nginx -s quit（优雅退出）..."
            & "$nginxDir\nginx.exe" -s quit 2>$null
            Start-Sleep -Seconds 2
        }
        $remain = Get-Process nginx -ErrorAction SilentlyContinue
        if ($remain) {
            Write-Host "  优雅退出未完成，强制停止..."
            Stop-Process -Name nginx -Force -ErrorAction Stop
            Start-Sleep -Milliseconds 500
        }
        $remain = Get-Process nginx -ErrorAction SilentlyContinue
        if (-not $remain) {
            Write-Host "  ✅ Nginx 已停止" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️ Nginx 仍有 $($remain.Count) 个进程，请手动结束" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ❌ 停止失败: $_" -ForegroundColor Red
    }
}

# ============================================================
# 3. 后端 Spring Boot (java.exe，端口 8080)
# ============================================================
Write-Host ""
Write-Host "[3/4] 停止后端 Spring Boot (端口 8080)..." -ForegroundColor Cyan

$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if (-not $port8080) {
    Write-Host "  ✅ 8080 端口未被占用（后端未运行）" -ForegroundColor Gray
} else {
    $uniquePids = $port8080 | Select-Object -ExpandProperty OwningProcess -Unique
    Write-Host "  发现占用 8080 端口的 PID: $($uniquePids -join ', ')"
    foreach ($pid in $uniquePids) {
        $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "  → PID: $pid, 名称: $($proc.ProcessName)"
            try {
                Stop-Process -Id $pid -Force -ErrorAction Stop
                Write-Host "    ✅ 已停止 PID $pid" -ForegroundColor Green
            } catch {
                Write-Host "    ❌ 停止 PID $pid 失败: $_" -ForegroundColor Red
            }
        }
    }
    Start-Sleep -Seconds 1
    $remain = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
    if (-not $remain) {
        Write-Host "  ✅ 后端已停止，8080 端口已释放" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️ 8080 端口仍被占用，请手动检查" -ForegroundColor Yellow
    }
}

# ============================================================
# 4. Redis（可选，默认不停止，因为 Redis 轻量）
# ============================================================
Write-Host ""
Write-Host "[4/4] Redis（默认保持运行）" -ForegroundColor Cyan

$redisProc = Get-Process redis-server -ErrorAction SilentlyContinue
if (-not $redisProc) {
    Write-Host "  ✅ Redis 未在运行" -ForegroundColor Gray
} else {
    Write-Host "  ℹ️  Redis 仍在运行 (PID: $($redisProc.Id))" -ForegroundColor Gray
    Write-Host "     Redis 很轻量（约 <10MB 内存），一般不需要停止" -ForegroundColor Gray
    Write-Host "     如需停止，执行: .\stop-all.ps1 -stopRedis" -ForegroundColor Gray

    if ($args -contains "-stopRedis") {
        try {
            Stop-Process -Name redis-server -Force -ErrorAction Stop
            Start-Sleep -Milliseconds 500
            $remain = Get-Process redis-server -ErrorAction SilentlyContinue
            if (-not $remain) {
                Write-Host "  ✅ Redis 已停止" -ForegroundColor Green
            }
        } catch {
            Write-Host "  ❌ Redis 停止失败: $_" -ForegroundColor Red
        }
    }
}

# ============================================================
# 总结
# ============================================================
Write-Host ""
Write-Host "==============================================" -ForegroundColor Yellow
Write-Host "  停止命令执行完毕" -ForegroundColor Yellow
Write-Host "==============================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "💡 再次启动:    .\start-all.ps1" -ForegroundColor Gray
Write-Host "💡 查看状态:    .\status-all.ps1" -ForegroundColor Gray
Write-Host ""
