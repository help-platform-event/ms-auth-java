package com.maxime.help.msauth.infrastructure.persistence;

import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts Spring Data JPA to the {@link UserRepository} port. */
@Component
class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    UserRepositoryAdapter(SpringDataUserJpaRepository jpa, UserPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity;
        if (user.getId() == null) {
            entity = mapper.toEntity(user);
        } else {
            entity = jpa.findById(user.getId())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + user.getId()));
            mapper.updateEntity(entity, user);
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByGoogleSub(String googleSub) {
        return jpa.findByGoogleSub(googleSub).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }
}
