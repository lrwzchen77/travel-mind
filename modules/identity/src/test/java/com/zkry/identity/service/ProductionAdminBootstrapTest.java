package com.zkry.identity.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionAdminBootstrapTest {

    @Test
    void provisionsRequiredInitialAdministrator() throws Exception {
        IdentityService identities = mock(IdentityService.class);
        MockEnvironment environment = new MockEnvironment()
            .withProperty("TRAVELMIND_BOOTSTRAP_ADMIN_USERNAME", "initial.admin")
            .withProperty("TRAVELMIND_BOOTSTRAP_ADMIN_PASSWORD", "a-secure-bootstrap-password")
            .withProperty("TRAVELMIND_BOOTSTRAP_ADMIN_NICKNAME", "首位管理员");

        new ProductionAdminBootstrap(identities, environment).run(new DefaultApplicationArguments());

        verify(identities).provisionInitialAdministrator(argThat(payload ->
            "initial.admin".equals(payload.get("username"))
                && "admin".equals(payload.get("role"))));
    }

    @Test
    void existingAdministratorSkipsBootstrapCredentials() throws Exception {
        IdentityService identities = mock(IdentityService.class);
        when(identities.hasAdministrator()).thenReturn(true);

        new ProductionAdminBootstrap(identities, new MockEnvironment()).run(new DefaultApplicationArguments());

        verify(identities, never()).provisionInitialAdministrator(argThat(payload -> true));
    }
}
