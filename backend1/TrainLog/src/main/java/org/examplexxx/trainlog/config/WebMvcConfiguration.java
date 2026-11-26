package org.examplexxx.trainlog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.examplexxx.trainlog.interceptor.JwtTokenAdminInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenAdminInterceptor)
                // /admin/** 是旧的示例路径，/api/training/** 是训练接口，
                // /api/auth/me、/api/auth/profile 需要登录，登录/注册保持放行
                .addPathPatterns("/admin/**", "/api/training/**", "/api/ai/**",
                        "/api/auth/me", "/api/auth/profile")
                .excludePathPatterns("/admin/employee/login");
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("trainLog项目接口文档")
                        .version("2.0")
                        .description("trainLog项目接口文档"));
    }
}
