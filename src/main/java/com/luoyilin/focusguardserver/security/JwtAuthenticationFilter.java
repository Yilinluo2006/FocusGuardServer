package com.luoyilin.focusguardserver.security;

import com.luoyilin.focusguardserver.model.User;
import com.luoyilin.focusguardserver.repository.UserRepository;
import com.luoyilin.focusguardserver.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }
    // Spring 自动注入 JWT 服务和用户数据库操作对象

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);
        // 读取请求头中的 Authorization 内容

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }
        // 没有 Bearer Token 时继续交给安全规则处理

        String token = authorizationHeader.substring(7);
        // 删除开头的 "Bearer "，只保留真正的 JWT

        if (SecurityContextHolder.getContext()
                .getAuthentication() == null) {

            // Existing JWTs already contain userId; changing a verified login identity never changes ownership.
            Long userId = jwtService.extractUserId(token);
            User user = userId == null ? null : userRepository.findById(userId).orElse(null);

            if (user != null && user.hasVerifiedLogin()) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.emptyList()
                        );
                // 创建一个已经通过验证的 Spring Security 身份对象

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );
                // 保存本次请求的 IP、Session 等附加信息

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
                // 告诉 Spring Security：当前请求属于这个用户
            }
        }

        filterChain.doFilter(request, response);
        // 过滤器处理完成，让请求继续进入后面的过滤器或 Controller
    }
}
