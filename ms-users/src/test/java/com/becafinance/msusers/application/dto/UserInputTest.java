package com.becafinance.msusers.application.dto;

import jakarta.validation.*;
import org.junit.jupiter.api.*;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;

class UserInputTest {
    static ValidatorFactory factory;
    @BeforeAll static void openValidator() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void closeValidator() { factory.close(); }

    @Test void normalizesBeforeBeanValidationUsedByHttp() {
        var input = new UserInput("Ana", " \tANA@EXAMPLE.COM\r\n");
        assertThat(input.email()).isEqualTo("ana@example.com");
        assertThat(factory.getValidator().validate(input)).isEmpty();
    }

    @Test void normalizationDoesNotDependOnDefaultLocale() {
        var original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(new UserInput("Iris", "IRIS@EXAMPLE.COM").email()).isEqualTo("iris@example.com");
        } finally { Locale.setDefault(original); }
    }

    @Test void nullAndBlankEmailsRemainInvalid() {
        assertThat(factory.getValidator().validate(new UserInput("Ana", null))).isNotEmpty();
        assertThat(factory.getValidator().validate(new UserInput("Ana", "  "))).isNotEmpty();
    }

    @Test void enforcesMaximumLengths() {
        assertThat(factory.getValidator().validate(new UserInput("a".repeat(151), "ana@example.com"))).isNotEmpty();
        assertThat(factory.getValidator().validate(new UserInput("Ana", "a".repeat(245) + "@example.com"))).isNotEmpty();
    }
}
