package com.luoyilin.focusguardserver.dto;
import jakarta.validation.constraints.*;
public record EmailBindingRequest(@NotBlank @Email @Size(max = 100) String email, @NotBlank @Size(min = 8, max = 64) String password,
        @Size(max=36) String phoneChallengeId, @Pattern(regexp="[0-9]{6}") String phoneCode) {
    public EmailBindingRequest(String email,String password) { this(email,password,null,null); }
}
