package com.domu.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void itShouldHashPasswordsWithRandomSalt() {
        String hashOne = hasher.hash("Sup3rSecr3t!");
        String hashTwo = hasher.hash("Sup3rSecr3t!");

        assertThat(hashOne).isNotBlank();
        assertThat(hashTwo).isNotBlank();
        assertThat(hashOne).isNotEqualTo(hashTwo);
    }

    @Test
    void itShouldVerifyMatchingPasswords() {
        String hash = hasher.hash("UltraSeguro123!");

        assertThat(hasher.matches("UltraSeguro123!", hash)).isTrue();
        assertThat(hasher.matches("OtraClave", hash)).isFalse();
    }

    @Test
    void itShouldRejectBlankPasswords() {
        assertThatThrownBy(() -> hasher.hash(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be blank");
    }
}
