package com.luoyilin.focusguardserver.config;

import com.luoyilin.focusguardserver.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    // Spring 自动注入我们编写的 JWT 验证过滤器

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                // REST API 使用 JWT，因此关闭 CSRF

                .formLogin(AbstractHttpConfigurer::disable)
                // 关闭 Spring Security 默认登录页面

                .httpBasic(AbstractHttpConfigurer::disable)
                // 关闭 HTTP Basic 用户名密码认证

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                // 不保存服务器 Session，每个请求都验证 JWT

                .authorizeHttpRequests(authorize -> authorize
                                .requestMatchers(
                                        "/api/auth/**",
                                        "/api/health",
                                        "/error"
                                ).permitAll()
                                // 注册、登录、健康检查不需要 JWT

                                .anyRequest().authenticated()
                        // 其他接口必须具有经过验证的身份
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        // 在默认用户名密码过滤器之前执行 JWT 过滤器

        return http.build();
        // 创建最终的 Spring Security 过滤器链
    }
}