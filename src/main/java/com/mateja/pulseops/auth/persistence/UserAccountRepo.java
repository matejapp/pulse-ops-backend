package com.mateja.pulseops.auth.persistence;

import com.mateja.pulseops.auth.domain.Role;
import com.mateja.pulseops.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Extending JpaRepository gives CRUD (save, findById, delete, ...) for free — no implementation.
// <UserAccount, UUID> = entity type + primary-key type. Spring Data generates the proxy at startup.
@Repository
public interface UserAccountRepo extends JpaRepository<UserAccount, UUID> {

    // "Derived query": Spring parses the METHOD NAME into SQL. findBy + Email + IgnoreCase
    // => WHERE lower(email) = lower(?). No @Query needed. Returns Optional (may not exist).
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    // existsBy... compiles to a lightweight SELECT 1 / COUNT — cheaper than loading the row
    // when we only need a yes/no (used by the register duplicate check).
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);



}
