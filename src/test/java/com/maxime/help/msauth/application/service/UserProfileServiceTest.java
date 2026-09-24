package com.maxime.help.msauth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maxime.help.msauth.application.exception.UserNotFoundException;
import com.maxime.help.msauth.domain.model.Address;
import com.maxime.help.msauth.domain.model.Profile;
import com.maxime.help.msauth.domain.model.Role;
import com.maxime.help.msauth.domain.model.User;
import com.maxime.help.msauth.domain.port.out.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pure application-service test — every port is mocked, no Spring, no database. */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;

    @InjectMocks private UserProfileService service;

    @Test
    void getProfile_returnsTheUsersProfile() {
        User user = userWithProfile(Profile.reconstitute("Alice", "Smith", null, null, null, null));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThat(service.getProfile(USER_ID).getFirstName()).isEqualTo("Alice");
    }

    @Test
    void getProfile_throwsWhenUserIsUnknown() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(USER_ID)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfile_replacesEveryFieldAndSaves() {
        Address oldAddress = new Address("1", "Old Street", null, "Paris", "75000", "France", null, null);
        User user = userWithProfile(Profile.reconstitute("Alice", "Smith", "http://a/old.png", "0600000000", "bio", oldAddress));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile updated = service.updateProfile(USER_ID, "Alicia", "Smythe", null, null, null, null);

        assertThat(updated.getFirstName()).isEqualTo("Alicia");
        assertThat(updated.getLastName()).isEqualTo("Smythe");
        assertThat(updated.getAvatarUrl()).isNull();
        assertThat(updated.getPhone()).isNull();
        assertThat(updated.getBio()).isNull();
        assertThat(updated.getAddress()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_throwsWhenUserIsUnknown() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(USER_ID, "A", "B", null, null, null, null))
                .isInstanceOf(UserNotFoundException.class);
    }

    private static User userWithProfile(Profile profile) {
        return User.reconstitute(USER_ID, "alice@example.com", "hash", Role.USER, null, false, null, profile, null, null);
    }
}
