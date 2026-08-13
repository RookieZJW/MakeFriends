package com.makefriends.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存统一工具类（Cache-Aside 模式）。
 * 用法：
 *   @Autowired private RedisCacheService cache;
 *   cache.set("mf:user:" + id, json, Duration.ofMinutes(10));
 *   String v = cache.get("mf:user:" + id);
 */
@Service
public class RedisCacheService {

    private final StringRedisTemplate redis;

    public RedisCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    public void set(String key, String value) {
        redis.opsForValue().set(key, value);
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    public void delete(String key) {
        redis.delete(key);
    }

    public void delete(Collection<String> keys) {
        redis.delete(keys);
    }

    public boolean hasKey(String key) {
        Boolean b = redis.hasKey(key);
        return Boolean.TRUE.equals(b);
    }

    /** 原子自增 + 首次设置过期（典型用于限流计数器） */
    public long incrEx(String key, Duration ttl) {
        Long cnt = redis.opsForValue().increment(key);
        if (cnt != null && cnt == 1L) {
            redis.expire(key, ttl);
        }
        return cnt == null ? 1L : cnt;
    }

    public long incr(String key, Duration ttl) {
        Long cnt = redis.opsForValue().increment(key);
        redis.expire(key, ttl);
        return cnt == null ? 1L : cnt;
    }

    public long ttl(String key) {
        Long t = redis.getExpire(key, TimeUnit.SECONDS);
        return t == null ? -2L : t;
    }
}