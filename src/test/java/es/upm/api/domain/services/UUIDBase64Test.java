package es.upm.api.domain.services;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDBase64Test {

    @Test
    void encodeShouldGenerateNonEmptyTokenForAllStrategies() {
        assertThat(UUIDBase64.BASIC.encode()).isNotBlank();
        assertThat(UUIDBase64.MIME.encode()).isNotBlank();
        assertThat(UUIDBase64.URL.encode()).isNotBlank();
    }

    @Test
    void decodeShouldRecoverUuidForAllStrategies() {
        UUID value = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        String basicEncoded = java.util.Base64.getEncoder().withoutPadding()
                .encodeToString(toBytes(value));
        String mimeEncoded = java.util.Base64.getMimeEncoder().withoutPadding()
                .encodeToString(toBytes(value));
        String urlEncoded = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(toBytes(value));

        assertThat(UUIDBase64.BASIC.decode(basicEncoded)).isEqualTo(value);
        assertThat(UUIDBase64.MIME.decode(mimeEncoded)).isEqualTo(value);
        assertThat(UUIDBase64.URL.decode(urlEncoded)).isEqualTo(value);
    }

    private byte[] toBytes(UUID value) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(16)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits());
        return buffer.array();
    }
}
