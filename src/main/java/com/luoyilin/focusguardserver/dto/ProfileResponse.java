package com.luoyilin.focusguardserver.dto;
import java.util.List;
public record ProfileResponse(Long userId, String displayName, String avatarKey, String email, boolean emailVerified, List<String> linkedMethods,
                              String phone, boolean phoneVerified, boolean hasPassword) { }
