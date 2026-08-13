package com.makefriends.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RateLimitService {

    private final RedisCacheService cache;

    public RateLimitService(RedisCacheService cache) {
        this.cache = cache;
    }

    @Value("${mf.rate-limit.ip-limit-per-min:180}")
    private int ipLimitPerMin;

    @Value("${mf.rate-limit.login-fail-threshold:5}")
    private int loginFailThreshold;

    @Value("${mf.rate-limit.login-fail-window-seconds:60}")
    private int loginFailWindow;

    @Value("${mf.rate-limit.login-lock-seconds:600}")
    private int loginLockSeconds;

    /* ---- 1. IP 1 分钟分桶计数 ---- */
    public boolean isIpOverLimit(String realIp) {
        if (realIp == null || realIp.isEmpty()) return false;
        String minute = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString()
                .replace("T", "-").substring(0, 16);
        String key = "mf:rl:ip:" + minute + ":" + realIp;
        long now = cache.incrEx(key, Duration.ofMinutes(2));
        return now > ipLimitPerMin;
    }

    /* ---- 2. 登录失败锁定 ---- */
    private String lockKey(String phone) { return "mf:rl:login:lock:" + phone; }
    private String failBucketKey(String phone, Instant t) {
        String m = t.truncatedTo(ChronoUnit.MINUTES).toString().replace("T","-").substring(0,16);
        return "mf:rl:login:fail:" + m + ":" + phone;
    }

    public boolean isLoginLocked(String phone) {
        return phone != null && !phone.isEmpty() && cache.hasKey(lockKey(phone));
    }
    public long loginLockRemainSeconds(String phone) {
        return cache.ttl(lockKey(phone));
    }

    public void onLoginFailed(String phone) {
        if (phone == null || phone.isEmpty()) return;
        Instant now = Instant.now();
        String curKey = failBucketKey(phone, now);
        long cur = cache.incrEx(curKey, Duration.ofSeconds(loginFailWindow + 10L));
        String prevKey = failBucketKey(phone, now.minus(1, ChronoUnit.MINUTES));
        String pv = cache.get(prevKey);
        long prev = (pv == null || pv.isEmpty()) ? 0L : Long.parseLong(pv);
        long combined = Math.max(cur, prev + cur);
        if (combined >= loginFailThreshold) {
            cache.set(lockKey(phone), "1", Duration.ofSeconds(loginLockSeconds));
        }
    }

    public void onLoginSuccess(String phone) {
        if (phone == null || phone.isEmpty()) return;
        cache.delete(lockKey(phone));
        Instant now = Instant.now();
        cache.delete(failBucketKey(phone, now));
        cache.delete(failBucketKey(phone, now.minus(1, ChronoUnit.MINUTES)));
    }

    /* ---- 3. 短信/注册：每小时 N 次 通用 ---- */
    public boolean isHourLimitReached(String key, int maxTimes) {
        if (key == null || key.isEmpty()) return false;
        String h = Instant.now().truncatedTo(ChronoUnit.HOURS).toString()
                .replace("T","-").substring(0,13);
        long n = cache.incrEx(key + ":" + h, Duration.ofHours(2));
        return n > maxTimes;
    }
}