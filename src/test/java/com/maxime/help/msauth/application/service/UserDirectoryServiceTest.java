package com.maxime.help.msauth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pure application-service test — every port is mocked, no Spring, no database. */
@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserDirectoryService service;

    @Test
    void findById_returnsTheUser() {
        User user = user(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(service.findById(user.getId())).isSameAs(user);
    }

    @Test
    void findById_throwsWhenUserIsUnknown() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void findByIds_delegatesToTheRepository() {
        User known = user(UUID.randomUUID());
        List<UUID> ids = List.of(known.getId(), UUID.randomUUID());
        when(userRepository.findAllByIds(ids)).thenReturn(List.of(known));

        assertThat(service.findByIds(ids)).containsExactly(known);
    }

    private static User user(UUID id) {
        return User.reconstitute(id, "alice@example.com", "hash", Role.USER, null, false, null, null, null, null);
    }
}
