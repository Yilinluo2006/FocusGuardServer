package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.repository.AppLimitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppLimitServiceTest {

    @Mock
    private AppLimitRepository appLimitRepository;
    // 创建假的 Repository，不连接 MySQL

    @InjectMocks
    private AppLimitService appLimitService;
    // 创建真实 Service，并注入假的 Repository

    @Test
    void addLimitSavesWhenPackageNameDoesNotExist() {
        AppLimit appLimit = new AppLimit(
                "com.example.test",
                30,
                true
        );

        when(
                appLimitRepository.existsByPackageName(
                        "com.example.test"
                )
        ).thenReturn(false);
        // 模拟数据库中不存在这个包名

        when(
                appLimitRepository.save(appLimit)
        ).thenReturn(appLimit);
        // 模拟保存后返回原对象

        AppLimit result = appLimitService.addLimit(appLimit);

        assertSame(appLimit, result);
        // 验证返回的是同一个对象

        verify(appLimitRepository)
                .existsByPackageName("com.example.test");

        verify(appLimitRepository)
                .save(appLimit);
    }

    @Test
    void addLimitThrowsConflictWhenPackageNameAlreadyExists() {
        AppLimit appLimit = new AppLimit(
                "com.instagram.android",
                30,
                true
        );

        when(
                appLimitRepository.existsByPackageName(
                        "com.instagram.android"
                )
        ).thenReturn(true);
        // 模拟数据库中已经存在这个包名

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appLimitService.addLimit(appLimit)
        );

        assertEquals(
                409,
                exception.getStatusCode().value()
        );

        assertEquals(
                "该应用已经存在限制记录",
                exception.getReason()
        );

        verify(appLimitRepository)
                .existsByPackageName("com.instagram.android");

        verify(
                appLimitRepository,
                never()
        ).save(appLimit);
        // 包名重复时不能保存
    }

    @Test
    void updateLimitUpdatesAndSavesWhenPackageNameIsAvailable() {
        Long id = 1L;
        // 1L 表示 Long 类型的数字 1

        AppLimit existingAppLimit = new AppLimit(
                "com.example.old",
                30,
                true
        );
        // 模拟数据库中的原记录

        AppLimit updatedAppLimit = new AppLimit(
                "com.example.updated",
                60,
                false
        );
        // 模拟客户端提交的新数据

        when(
                appLimitRepository.findById(id)
        ).thenReturn(Optional.of(existingAppLimit));
        // 模拟根据 ID 找到原记录

        when(
                appLimitRepository.existsByPackageNameAndIdNot(
                        "com.example.updated",
                        id
                )
        ).thenReturn(false);
        // 模拟新包名没有被其他记录占用

        when(
                appLimitRepository.save(existingAppLimit)
        ).thenReturn(existingAppLimit);

        AppLimit result = appLimitService.updateLimit(
                id,
                updatedAppLimit
        );

        assertSame(existingAppLimit, result);
        // 验证 Service 修改并返回原实体

        assertEquals(
                "com.example.updated",
                result.getPackageName()
        );

        assertEquals(
                60,
                result.getLimitMinutes()
        );

        assertFalse(result.isEnabled());
        // 验证三个业务字段都已更新

        verify(appLimitRepository).findById(id);

        verify(appLimitRepository)
                .existsByPackageNameAndIdNot(
                        "com.example.updated",
                        id
                );

        verify(appLimitRepository)
                .save(existingAppLimit);
    }

    @Test
    void updateLimitThrowsConflictWhenPackageNameBelongsToAnotherRecord() {
        Long id = 1L;

        AppLimit existingAppLimit = new AppLimit(
                "com.example.old",
                30,
                true
        );
        // 模拟数据库中的原记录

        AppLimit updatedAppLimit = new AppLimit(
                "com.instagram.android",
                60,
                false
        );
        // 模拟客户端想改成已被其他记录使用的包名

        when(
                appLimitRepository.findById(id)
        ).thenReturn(Optional.of(existingAppLimit));

        when(
                appLimitRepository.existsByPackageNameAndIdNot(
                        "com.instagram.android",
                        id
                )
        ).thenReturn(true);
        // true 表示其他记录已经使用了目标包名

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appLimitService.updateLimit(
                        id,
                        updatedAppLimit
                )
        );
        // 验证更新操作抛出冲突异常

        assertEquals(
                409,
                exception.getStatusCode().value()
        );

        assertEquals(
                "该应用已经存在其他限制记录",
                exception.getReason()
        );

        assertEquals(
                "com.example.old",
                existingAppLimit.getPackageName()
        );

        assertEquals(
                30,
                existingAppLimit.getLimitMinutes()
        );

        assertTrue(existingAppLimit.isEnabled());
        // 验证发生冲突后，原对象的三个字段都没有改变

        verify(appLimitRepository).findById(id);

        verify(appLimitRepository)
                .existsByPackageNameAndIdNot(
                        "com.instagram.android",
                        id
                );

        verify(
                appLimitRepository,
                never()
        ).save(any(AppLimit.class));
        // any(AppLimit.class) 表示任意 AppLimit 对象都不允许保存
    }
    @Test
    void getLimitByIdReturnsLimitWhenRecordExists() {
        Long id = 1L;

        AppLimit appLimit = new AppLimit(
                "com.example.test",
                30,
                true
        );
        // 创建一条模拟的应用限制记录

        when(
                appLimitRepository.findById(id)
        ).thenReturn(Optional.of(appLimit));
        // 模拟 Repository 根据 ID 查询到这条记录

        AppLimit result = appLimitService.getLimitById(id);
        // 调用真正的 Service 查询方法

        assertSame(appLimit, result);
        // 验证 Service 返回的是查询到的对象

        verify(appLimitRepository).findById(id);
        // 验证 Repository 的 findById 确实被调用了一次
    }
    @Test
    void getLimitByIdThrowsNotFoundWhenRecordDoesNotExist() {
        Long id = 99L;
        // 使用一个假设不存在的记录 ID

        when(
                appLimitRepository.findById(id)
        ).thenReturn(Optional.empty());
        // Optional.empty() 表示 Repository 没有查询到数据

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appLimitService.getLimitById(id)
        );
        // 验证调用 Service 后会抛出 ResponseStatusException

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
        // 验证异常对应的 HTTP 状态码是 404

        assertEquals(
                "App limit not found",
                exception.getReason()
        );
        // 验证异常提示内容正确

        verify(appLimitRepository).findById(id);
        // 验证 Repository 的查询方法确实被调用
    }
    @Test
    void deleteLimitDeletesWhenRecordExists() {
        Long id = 1L;
        // 准备要删除的记录 ID

        when(
                appLimitRepository.existsById(id)
        ).thenReturn(true);
        // 模拟数据库中存在这条记录

        appLimitService.deleteLimit(id);
        // 调用 Service 的删除方法

        verify(appLimitRepository).existsById(id);
        // 验证 Service 先检查了记录是否存在

        verify(appLimitRepository).deleteById(id);
        // 验证存在记录时，确实调用了删除方法
    }
    @Test
    void deleteLimitThrowsNotFoundWhenRecordDoesNotExist() {
        Long id = 99L;
        // 准备一个不存在的记录 ID

        when(
                appLimitRepository.existsById(id)
        ).thenReturn(false);
        // 模拟数据库中不存在这条记录

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appLimitService.deleteLimit(id)
        );
        // 验证删除操作会抛出 ResponseStatusException

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
        // 验证 HTTP 状态码为 404

        assertEquals(
                "App limit not found",
                exception.getReason()
        );
        // 验证异常提示内容

        verify(appLimitRepository).existsById(id);
        // 验证 Service 检查了记录是否存在

        verify(
                appLimitRepository,
                never()
        ).deleteById(id);
        // 记录不存在时，绝对不能调用删除方法
    }
}
