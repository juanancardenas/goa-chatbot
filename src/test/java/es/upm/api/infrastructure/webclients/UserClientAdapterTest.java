package es.upm.api.infrastructure.webclients;

import es.upm.api.domain.model.platform.UserSummary;
import es.upm.api.infrastructure.webclients.user.UserFeignMapper;
import es.upm.api.infrastructure.webclients.user.dto.UserResponseDto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserClientAdapterTest {

    @Test
    void readByIdShouldMapFeignDtoToDomain() {
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        UserClientAdapter adapter = new UserClientAdapter(userFeignClient, new UserFeignMapper());
        String userId = "user-123";
        UserResponseDto responseDto = new UserResponseDto(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "Ana",
                "Ocana",
                "user@goa.test",
                "600000001"
        );
        when(userFeignClient.readById(userId)).thenReturn(responseDto);

        UserSummary result = adapter.readById(userId);

        assertThat(result).isNotSameAs(responseDto);
        assertThat(result.getId()).isEqualTo(responseDto.getId());
        assertThat(result.getFirstName()).isEqualTo("Ana");
        assertThat(result.getFamilyName()).isEqualTo("Ocana");
        assertThat(result.getEmail()).isEqualTo("user@goa.test");
        assertThat(result.getMobile()).isEqualTo("600000001");
        verify(userFeignClient).readById(userId);
    }

    @Test
    void readByIdShouldReturnNullWhenFeignReturnsNull() {
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        UserClientAdapter adapter = new UserClientAdapter(userFeignClient, new UserFeignMapper());
        when(userFeignClient.readById("missing-user")).thenReturn(null);

        UserSummary result = adapter.readById("missing-user");

        assertThat(result).isNull();
    }

    @Test
    void readByIdShouldPropagateFeignErrors() {
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        UserClientAdapter adapter = new UserClientAdapter(userFeignClient, new UserFeignMapper());
        RuntimeException expected = new RuntimeException("user service unavailable");
        when(userFeignClient.readById("user-error")).thenThrow(expected);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.readById("user-error")
        );

        assertThat(exception).isSameAs(expected);
    }
}
