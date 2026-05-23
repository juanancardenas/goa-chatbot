package es.upm.api.adapter.out.webclient.user.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserResponseDtoTest {

    @Test
    void allArgsConstructorShouldSetFields() {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");

        UserResponseDto dto = new UserResponseDto(
                id,
                "Ana",
                "Ocana",
                "ana@goa.test",
                "600000000"
        );

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getFirstName()).isEqualTo("Ana");
        assertThat(dto.getFamilyName()).isEqualTo("Ocana");
        assertThat(dto.getEmail()).isEqualTo("ana@goa.test");
        assertThat(dto.getMobile()).isEqualTo("600000000");
    }

    @Test
    void settersShouldUpdateFields() {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UserResponseDto dto = new UserResponseDto();

        dto.setId(id);
        dto.setFirstName("Luis");
        dto.setFamilyName("Perez");
        dto.setEmail("luis@goa.test");
        dto.setMobile("600000001");

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getFirstName()).isEqualTo("Luis");
        assertThat(dto.getFamilyName()).isEqualTo("Perez");
        assertThat(dto.getEmail()).isEqualTo("luis@goa.test");
        assertThat(dto.getMobile()).isEqualTo("600000001");
    }

    @Test
    void shouldIgnoreUnknownJsonProperties() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        UserResponseDto dto = objectMapper.readValue("""
                {
                  "id": "55555555-5555-5555-5555-555555555555",
                  "firstName": "Marta",
                  "familyName": "Sanz",
                  "email": "marta@goa.test",
                  "mobile": "600000002",
                  "externalOnlyField": "ignored"
                }
                """, UserResponseDto.class);

        assertThat(dto.getId()).isEqualTo(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        assertThat(dto.getFirstName()).isEqualTo("Marta");
        assertThat(dto.getFamilyName()).isEqualTo("Sanz");
        assertThat(dto.getEmail()).isEqualTo("marta@goa.test");
        assertThat(dto.getMobile()).isEqualTo("600000002");
    }
}
