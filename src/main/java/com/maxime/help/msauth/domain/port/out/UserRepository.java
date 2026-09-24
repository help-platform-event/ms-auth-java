package com.maxime.help.msauth.domain.port.out;

import com.maxime.help.msauth.domain.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for user persistence. Implemented in {@code infrastructure.persistence}. */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    boolean existsById(UUID id);

    /** Users whose id is in {@code ids}; unknown ids are silently skipped. */
    List<User> findAllByIds(Collection<UUID> ids);

    List<User> findAll();
}
