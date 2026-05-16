package es.upm.api.infrastructure.webclients;

import es.upm.api.domain.model.platform.UserSummary;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserClientAdapterTest {

    @Test
    void readByIdShouldDelegateToFeignClient() {
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        UserClientAdapter adapter = new UserClientAdapter(userFeignClient);
        String userId = "user-123";
        UserSummary expected = UserSummary.builder()
                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .mobile("600000001")
                .email("user@goa.test")
                .firstName("Ana")
                .familyName("Ocana")
                .build();
        when(userFeignClient.readById(userId)).thenReturn(expected);

        UserSummary result = adapter.readById(userId);

        assertThat(result).isSameAs(expected);
        verify(userFeignClient).readById(userId);
    }
}
