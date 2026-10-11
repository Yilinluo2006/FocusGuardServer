package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
public record EmailBindingConfirm(@NotBlank @Email @Size(max = 100) String email, @NotBlank @Pattern(regexp = "[0-9]{6}") String code) { }
