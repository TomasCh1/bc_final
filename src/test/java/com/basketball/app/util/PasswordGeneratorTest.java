package com.basketball.app.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class PasswordGeneratorTest {

    @Test
    void generate_DefaultLength_IsTwelve() {
        String password = PasswordGenerator.generate();
        assertEquals(12, password.length());
    }

    @Test
    void generate_TooShortLength_FallsBackToTwelve() {
        String password = PasswordGenerator.generate(4);
        assertEquals(12, password.length());
    }

    @Test
    void generate_ContainsAllRequiredCharacterTypes() {
        String password = PasswordGenerator.generate(14);
        assertTrue(password.chars().anyMatch(Character::isUpperCase));
        assertTrue(password.chars().anyMatch(Character::isLowerCase));
        assertTrue(password.chars().anyMatch(Character::isDigit));
        assertTrue(password.matches(".*[!@#$%^&*].*"));
    }
}
