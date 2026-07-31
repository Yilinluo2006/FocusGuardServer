package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.repository.AppLimitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppLimitServiceTest {

    @Mock
    private AppLimitRepository appLimitRepository;
    // 创建一个假的 Repository，不连接真实 MySQL

    @InjectMocks
    private AppLimitService appLimitService;
    // 创建 Service，并自动把假的 Repository 注入进去

    @Test
    void addLimitSavesWhenPackageNameDoesNotExist() {
        AppLimit appLimit = new AppLimit(
                "com.example.test",
                30,
                true
        );
        // 准备一条用于测试的应用限制数据

        when(
                appLimitRepository.existsByPackageName(
                        "com.example.test"
                )
        ).thenReturn(false);
        // 规定假的 Repository 查询包名时返回 false，表示包名不存在

        when(
                appLimitRepository.save(appLimit)
        ).thenReturn(appLimit);
        // 规定调用 save() 时，返回传入的 AppLimit 对象

        AppLimit result = appLimitService.addLimit(appLimit);
        // 调用真正需要测试的 Service 方法

        assertSame(appLimit, result);
        // 判断 Service 返回的是否是预期对象

        verify(
                appLimitRepository
        ).existsByPackageName("com.example.test");
        // 验证 Service 确实检查了包名是否重复

        verify(
                appLimitRepository
        ).save(appLimit);
        // 验证检查通过后确实调用了保存方法
    }
}
