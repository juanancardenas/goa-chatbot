package es.upm.api.domain.model.platform;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserSummaryTest {

    @Test
    void displayNameShouldTrimAndJoinFirstAndFamilyName() {
        UserSummary userSummary = new UserSummary(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "  Ana  ",
                "  Ocaña  ",
                "ana@example.com",
                "+34600111222"
        );

        assertThat(userSummary.displayName()).isEqualTo("Ana Ocaña");
    }

    @Test
    void displayNameShouldReturnFallbackWhenNamesAreNullOrBlank() {
        UserSummary withNullNames = new UserSummary(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                null,
                null,
                "user@example.com",
                null
        );
        UserSummary withBlankNames = new UserSummary(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "   ",
                "\t",
                "blank@example.com",
                null
        );

        assertThat(withNullNames.displayName()).isEqualTo("usuario del encargo");
        assertThat(withBlankNames.displayName()).isEqualTo("usuario del encargo");
    }
}
