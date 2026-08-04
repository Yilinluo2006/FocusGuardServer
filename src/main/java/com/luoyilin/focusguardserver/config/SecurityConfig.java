package com.luoyilin.focusguardserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                // 关闭 CSRF，因为我们的客户端通过 JWT 访问 REST API

                .formLogin(AbstractHttpConfigurer::disable)
                // 关闭 Spring Security 自带的网页登录页面

                .httpBasic(AbstractHttpConfigurer::disable)
                // 关闭浏览器弹出的 HTTP Basic 用户名密码验证

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                // 不在服务器中保存登录 Session，每次请求都依靠 JWT

                .authorizeHttpRequests(authorize -> authorize
                                .requestMatchers(
                                        "/api/auth/**",
                                        "/api/health",
                                        "/error"
                                ).permitAll()
                                // 注册、登录和健康检查不需要携带 JWT

                                .anyRequest().authenticated()
                        // 其他所有接口都必须经过身份验证
                );

        return http.build();
        // 构建并交给 Spring 管理这条安全过滤器链
    }
}