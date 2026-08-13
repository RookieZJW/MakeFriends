package com.makefriends.config;

import com.makefriends.common.RateLimitService;
import com.makefriends.common.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class GlobalRateLimitFilter implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GlobalRateLimitFilter.class);

    private final RateLimitService rateLimitService;

    public GlobalRateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Value("${mf.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${mf.rate-limit.whitelist-ip:127.0.0.1,0:0:0:0:0:0:0:1}")
    private List<String> whitelistIp;

    @Value("${mf.rate-limit.whitelist-path-prefix:/actuator,/swagger-ui,/v3/api-docs,/files,/upload}")
    private String[] whitelistPrefix;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        if (!enabled) return true;
        String path = req.getRequestURI();
        if (whitelistPrefix != null) {
            for (String prefix : whitelistPrefix) {
                if (path.startsWith(prefix)) return true;
            }
        }
        String ip = resolveRealIp(req);
        if (whitelistIp != null && whitelistIp.contains(ip)) return true;
        if (rateLimitService.isIpOverLimit(ip)) {
            log.warn("[IP-RL] {} blocked on {} (too frequent)", ip, path);
            throw new TooManyRequestsException("请求过于频繁，请稍后再试", 60L);
        }
        return true;
    }

    public static String resolveRealIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            int c = xff.indexOf(',');
            return c > 0 ? xff.substring(0, c).trim() : xff.trim();
        }
        String xr = req.getHeader("X-Real-IP");
        if (xr != null && !xr.isEmpty() && !"unknown".equalsIgnoreCase(xr)) return xr.trim();
        String r = req.getRemoteAddr();
        return r == null ? "" : r;
    }
}