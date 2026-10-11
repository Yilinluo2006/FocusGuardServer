package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
public record PhoneBindingRequest(@NotBlank @Pattern(regexp="1[3-9][0-9]{9}") String phone,
        @Size(max=64) String password, @Size(max=36) String proofChallengeId, @Pattern(regexp="[0-9]{6}") String proofCode) { }
