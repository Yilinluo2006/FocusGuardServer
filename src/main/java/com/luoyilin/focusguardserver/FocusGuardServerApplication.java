//负责业务逻辑，是 Controller 和数据来源之间的中间层
//启动 Spring Boot
//启动内置 Web 服务器
//扫描 Controller 和 Service
//创建并管理相关对象
//监听 8080 端口
package com.luoyilin.focusguardserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FocusGuardServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FocusGuardServerApplication.class, args);
    }

}
