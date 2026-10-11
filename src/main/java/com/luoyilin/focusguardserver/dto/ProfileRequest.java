package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
public record ProfileRequest(@NotBlank @Size(max = 96) String displayName, @NotBlank @Size(max = 32) String avatarKey) { }
