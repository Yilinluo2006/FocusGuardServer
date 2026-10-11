package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailBindingChallengeRepository extends JpaRepository<EmailBindingChallenge, Long> {
    Optional<EmailBindingChallenge> findByUser(User user);
}
