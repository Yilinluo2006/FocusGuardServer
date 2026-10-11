package com.luoyilin.focusguardserver.repository;

import com.luoyilin.focusguardserver.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AccountIdentityRepository extends JpaRepository<AccountIdentity, Long> {
    Optional<AccountIdentity> findByProviderAndIssuerAndSubject(AccountIdentity.Provider provider, String issuer, String subject);
    Optional<AccountIdentity> findByUserAndProviderAndIssuer(User user, AccountIdentity.Provider provider, String issuer);
    List<AccountIdentity> findAllByUser(User user);
}
