package com.streamvault.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "a.b@sub.domain.org", "x@y.co"})
    void acceptsValidEmails(String email) {
        assertTrue(ValidationUtil.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plainaddress", "no@tld", "two@@example.com", "sp ace@x.com"})
    void rejectsInvalidEmails(String email) {
        assertFalse(ValidationUtil.isValidEmail(email));
    }

    @Test
    void rejectsNullEmail() {
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @Test
    void rejectsOverlongEmail() {
        String local = "a".repeat(250);
        assertFalse(ValidationUtil.isValidEmail(local + "@x.com"));
    }

    @Test
    void acceptsPasswordAtMinimumLength(){
        assertTrue(ValidationUtil.isValidPassword("12345678"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "1234567"})
    void rejectsShortPasswords(String password) {
        assertFalse(ValidationUtil.isValidPassword(password));
    }

    @Test
    void rejectsNullPassword() {
        assertFalse(ValidationUtil.isValidPassword(null));
    }

    @Test
    void acceptsReasonableName() {
        assertTrue(ValidationUtil.isValidName("Ada Lovelace"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankNames(String name) {
        assertFalse(ValidationUtil.isValidName(name));
    }

    @Test
    void rejectsNullName() {
        assertFalse(ValidationUtil.isValidName(null));
    }
}
