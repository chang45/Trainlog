package com.trainlog.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    /**
     * 取客户端 IP。只有在反向代理可信时才会用到 X-Forwarded-For / X-Real-IP。
     */
    public static String getIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String ip = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            return ip.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}
