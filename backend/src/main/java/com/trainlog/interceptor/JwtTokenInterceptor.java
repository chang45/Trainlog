package com.trainlog.interceptor;

import com.trainlog.constant.JwtClaimsConstant;
import com.trainlog.context.BaseContext;
import com.trainlog.exception.BizException;
import com.trainlog.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtTokenInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BizException(401, "请先登录");
        }
        try {
            Claims claims = jwtUtil.parseToken(header.substring(7).trim());
            Object uid = claims.get(JwtClaimsConstant.UID);
            if (!(uid instanceof Number number)) {
                throw new BizException(401, "登录状态已失效，请重新登录");
            }
            BaseContext.setCurrentId(number.longValue());
            BaseContext.setUsername(claims.getSubject());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(401, "登录状态已失效，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.clear();
    }
}
