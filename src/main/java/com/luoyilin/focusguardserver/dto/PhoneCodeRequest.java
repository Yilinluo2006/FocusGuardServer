package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
public record PhoneCodeRequest(@NotBlank @Pattern(regexp="1[3-9][0-9]{9}") String phone) { }
