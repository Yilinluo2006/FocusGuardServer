package com.luoyilin.focusguardserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String senderAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderAddress
    ) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }
    // Spring 自动注入邮件发送器和配置中的发件邮箱地址

    @Async("mailTaskExecutor")
    public void sendPasswordResetCode(
            String recipientAddress,
            String plainCode
    ) {
        if (senderAddress == null || senderAddress.isBlank()) {
            throw new IllegalStateException("邮件发送账号尚未配置");
        }
        // 未设置 MAIL_USERNAME 时立即提示配置问题

        SimpleMailMessage message = new SimpleMailMessage();
        // 创建一封纯文本邮件

        message.setFrom(senderAddress);
        message.setTo(recipientAddress);
        message.setSubject("FocusGuard 密码重置验证码");
        // 设置发件人、收件人和邮件标题

        message.setText(
                "你正在重置 FocusGuard 账号密码。\n\n"
                        + "验证码：" + plainCode + "\n\n"
                        + "验证码将在 10 分钟后失效，请勿转发给他人。\n"
                        + "如果不是你本人操作，请忽略这封邮件。"
        );
        // 邮件正文只在内存中使用一次六位明文验证码

        mailSender.send(message);
        // 通过已配置的 SMTP 服务器发送邮件
    }

    @Async("mailTaskExecutor")
    public void sendEmailVerificationCode(
            String recipientAddress,
            String plainCode
    ) {
        if (senderAddress == null || senderAddress.isBlank()) {
            throw new IllegalStateException("邮件发送账号尚未配置");
        }
        // 未设置 MAIL_USERNAME 时立即提示配置问题

        SimpleMailMessage message = new SimpleMailMessage();
        // 创建一封注册邮箱验证的纯文本邮件

        message.setFrom(senderAddress);
        message.setTo(recipientAddress);
        message.setSubject("FocusGuard 注册邮箱验证码");
        // 设置发件人、注册收件邮箱和邮件标题

        message.setText(
                "你正在验证 FocusGuard 账号的注册邮箱。\n\n"
                        + "验证码：" + plainCode + "\n\n"
                        + "验证码将在 10 分钟后失效，请勿转发给他人。\n"
                        + "如果不是你本人操作，请忽略这封邮件。"
        );
        // 邮件正文只在内存中使用一次六位明文验证码

        mailSender.send(message);
        // 通过已配置的 SMTP 服务器发送注册邮箱验证码
    }
}
