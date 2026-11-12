package com.becafinance.msusers.adapter.out.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BcryptPasswordHasherTest {
    @Test void hashesWithSaltAndPreservesPasswordExactly() {
        var passwords = new BcryptPasswordHasher();
        var hash = passwords.hash(" Secret123 ");
        assertThat(hash).startsWith("$2").isNotEqualTo(" Secret123 ");
        assertThat(passwords.hash(" Secret123 ")).isNotEqualTo(hash);
        assertThat(passwords.matches(" Secret123 ", hash)).isTrue();
        assertThat(passwords.matches("Secret123", hash)).isFalse();
        assertThat(passwords.matches("wrong", hash)).isFalse();
    }
}
