package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.AppLimit;
import com.luoyilin.focusguardserver.model.User;
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
    // 创建假的 Repository，不连接真实 MySQL

    @InjectMocks
    private AppLimitService appLimitService;
    // 创建真实 Service，并自动注入假的 Repository

    private final User currentUser = new User(
            "testuser",
            "test@example.com",
            "hashed-password"
    );
    // 所有测试使用的模拟当前用户

    @Test
    void addLimitSavesWhenPackageNameDoesNotExist() {
        AppLimit appLimit = new AppLimit(
                "com.example.test",
                30,
                true
        );

        when(
                appLimitRepository.existsByPackageNameAndUser(
                        "com.example.test",
                        currentUser
                )
        ).thenReturn(false);
        // 模拟当前用户没有添加过该应用

        when(
                appLimitRepository.save(appLimit)
        ).thenReturn(appLimit);

        AppLimit result =
                appLimitService.addLimit(appLimit, currentUser);

        assertSame(appLimit, result);
        assertSame(currentUser, result.getUser());
        // 验证 Service 已经将记录绑定到当前用户

        verify(appLimitRepository)
                .existsByPackageNameAndUser(
                        "com.example.test",
                        currentUser
                );

        verify(appLimitRepository).save(appLimit);
    }

    @Test
    void addLimitThrowsConflictWhenPackageNameAlreadyExists() {
        AppLimit appLimit = new AppLimit(
                "com.instagram.android",
                30,
                true
        );

        when(
                appLimitRepository.existsByPackageNameAndUser(
                        "com.instagram.android",
                        currentUser
                )
        ).thenReturn(true);
        // 模拟当前用户已经添加过该应用

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appLimitService.addLimit(
                        appLimit,
                        currentUser
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals(
                "该用户已经存在这个应用的限制记录",
                exception.getReason()
        );

        verify(
                appLimitRepository,
                never()
        ).save(any(AppLimit.class));
        // 包名重复时不能保存
    }

    @Test
    void updateLimitUpdatesAndSavesWhenPackageNameIsAvailable() {
        Long id = 1L;

        AppLimit existingAppLimit = new AppLimit(
                "com.example.old",
                30,
                true
        );
        existingAppLimit.setUser(currentUser);

        AppLimit updatedAppLimit = new AppLimit(
                "com.example.updated",
                60,
                false
        );

        when(
                appLimitRepository.findByIdAndUser(
                        id,
                        currentUser
                )
        ).thenReturn(Optional.of(existingAppLimit));

        when(
                appLimitRepository
                        .existsByPackageNameAndUserAndIdNot(
                                "com.example.updated",
                                currentUser,
                                id
                        )
        ).thenReturn(false);
        // 模拟新包名没有被当前用户的其他记录占用

        when(
                appLimitRepository.save(existingAppLimit)
        ).thenReturn(existingAppLimit);

        AppLimit result = appLimitService.updateLimit(
                id,
                updatedAppLimit,
                currentUser
        );

        assertSame(existingAppLimit, result);
        assertEquals(
                "com.example.updated",
                result.getPackageName()
        );
        assertEquals(60, result.getLimitMinutes());
        assertFalse(result.isEnabled());
        assertSame(currentUser, result.getUser());

        verify(appLimitRepository)
                .findByIdAndUser(id, currentUser);

        verify(appLimitRepository)
                .existsByPackageNameAndUserAndIdNot(
                        "com.example.updated",
                        currentUser,
                        id
                );

        verify(appLimitRepository).save(existingAppLimit);
    }

    @Test
    void updateLimitThrowsConflictWhenPackageNameBelongsToAnotherRecord() {
        Long id = 1L;

        AppLimit existingAppLimit = new AppLimit(
                "com.example.old",
                30,
                true
        );
        existingAppLimit.setUser(currentUser);

        AppLimit updatedAppLimit = new AppLimit(
                "com.instagram.android",
                60,
                false
        );

        when(
                appLimitRepository.findByIdAndUser(
                        id,
                        currentUser
                )
        ).thenReturn(Optional.of(existingAppLimit));

        when(
                appLimitRepository
                        .existsByPackageNameAndUserAndIdNot(
                                "com.instagram.android",
                                currentUser,
                                id
                        )
        ).thenReturn(true);
        // 模拟当前用户的其他记录已经使用该包名

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appLimitService.updateLimit(
                        id,
                        updatedAppLimit,
                        currentUser
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals(
                "该用户已经存在这个应用的其他限制记录",
                exception.getReason()
        );

        assertEquals(
                "com.example.old",
                existingAppLimit.getPackageName()
        );
        assertEquals(30, existingAppLimit.getLimitMinutes());
        assertTrue(existingAppLimit.isEnabled());
        // 发生冲突后，原记录不能被修改

        verify(
                appLimitRepository,
                never()
        ).save(any(AppLimit.class));
    }

    @Test
    void getLimitByIdReturnsLimitWhenRecordExists() {
        Long id = 1L;

        AppLimit appLimit = new AppLimit(
                "com.example.test",
                30,
                true
        );
        appLimit.setUser(currentUser);

        when(
                appLimitRepository.findByIdAndUser(
                        id,
                        currentUser
                )
        ).thenReturn(Optional.of(appLimit));

        AppLimit result =
                appLimitService.getLimitById(id, currentUser);

        assertSame(appLimit, result);

        verify(appLimitRepository)
                .findByIdAndUser(id, currentUser);
    }

    @Test
    void getLimitByIdThrowsNotFoundWhenRecordDoesNotExist() {
        Long id = 99L;

        when(
                appLimitRepository.findByIdAndUser(
                        id,
                        currentUser
                )
        ).thenReturn(Optional.empty());
        // 不存在或不属于当前用户时都返回空

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appLimitService.getLimitById(
                        id,
                        currentUser
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals(
                "未找到该应用限制",
                exception.getReason()
        );

        verify(appLimitRepository)
                .findByIdAndUser(id, currentUser);
    }

    @Test
    void deleteLimitDeletesWhenRecordExists() {
        Long id = 1L;

        AppLimit appLimit = new AppLimit(
                "com.example.test",
                30,
                true
        );
        appLimit.setUser(currentUser);

        when(
                appLimitRepository.findByIdAndUser(
                        id,
                        currentUser
                )
        ).thenReturn(Optional.of(appLimit));

        appLimitService.deleteLimit(id, currentUser);

        verify(appLimitRepository)
                .findByIdAndUser(id, currentUser);

        verify(appLimitRepository).delete(appLimit);
        // 删除查询到的当前用户实体
    }

    @Test
    void deleteLimitThrowsNotFoundWhenRecordDoesNotExist() {
        Long id = 99L;

        when(
                appLimitRepository.findByIdAndUser(
                        id,
                        currentUser
                )
        ).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appLimitService.deleteLimit(
                        id,
                        currentUser
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals(
                "未找到该应用限制",
                exception.getReason()
        );

        verify(
                appLimitRepository,
                never()
        ).delete(any(AppLimit.class));
        // 找不到当前用户的记录时，不能执行删除
    }
}