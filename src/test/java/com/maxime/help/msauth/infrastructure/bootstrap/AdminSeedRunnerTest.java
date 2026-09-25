package com.maxime.help.msauth.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.maxime.help.msauth.application.service.AdminProvisioningResult;
import com.maxime.help.msauth.application.service.AdminProvisioningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSeedRunnerTest {

    @Mock private AdminProvisioningService adminProvisioningService;

    @Test
    void isDisabledWhenNothingIsConfigured() {
        new AdminSeedRunner(new AdminSeedProperties("", null, false), adminProvisioningService).run(null);

        verifyNoInteractions(adminProvisioningService);
    }

    @Test
    void failsWhenRequiredButNothingIsConfigured() {
        AdminSeedRunner runner = new AdminSeedRunner(new AdminSeedProperties("", "", true), adminProvisioningService);

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");
        verifyNoInteractions(adminProvisioningService);
    }

    @Test
    void failsWhenOnlyOneValueIsConfigured() {
        AdminSeedRunner runner =
                new AdminSeedRunner(new AdminSeedProperties("admin@example.com", " ", false), adminProvisioningService);

        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(adminProvisioningService);
    }

    @Test
    void delegatesWhenBothValuesAreConfigured() {
        when(adminProvisioningService.ensureAdminExists("admin@example.com", "Admin123!!"))
                .thenReturn(AdminProvisioningResult.CREATED);

        new AdminSeedRunner(new AdminSeedProperties("admin@example.com", "Admin123!!", true), adminProvisioningService)
                .run(null);

        verify(adminProvisioningService).ensureAdminExists("admin@example.com", "Admin123!!");
    }
}
