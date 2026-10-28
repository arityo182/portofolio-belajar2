package com.koperasi.auth.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {

    @Test
    void hash_isDeterministicSha256Hex() {
        String token = "refresh-token-value";

        String first = TokenHasher.hash(token);
        String second = TokenHasher.hash(token);

        assertThat(first).hasSize(64).isEqualTo(second).matches("[0-9a-f]{64}");
        assertThat(TokenHasher.hash("another-token")).isNotEqualTo(first);
    }
}
