package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
public record PhoneCodeConfirm(@NotBlank @Pattern(regexp="[0-9a-f-]{36}") String challengeId,
                               @NotBlank @Pattern(regexp="[0-9]{6}") String code) { }
