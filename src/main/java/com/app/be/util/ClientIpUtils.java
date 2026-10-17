package com.app.be.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIpUtils {

    private ClientIpUtils() {}

    /**
     * Trích xuất địa chỉ IP thực tế của client từ request (xử lý qua proxy, reverse proxy, NGINX hoặc Cloudflare).
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String xfHeader = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xfHeader)) {
            String[] ips = xfHeader.split(",");
            if (ips.length > 0 && StringUtils.hasText(ips[0])) {
                return ips[0].trim();
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
