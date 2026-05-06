package com.afitnerd.tnra.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupNameValidatorTest {

    @Test
    void validName() {
        assertNull(GroupNameValidator.validate("recovery-guys"));
    }

    @Test
    void validNameDigits() {
        assertNull(GroupNameValidator.validate("group42"));
    }

    @Test
    void validMinLength() {
        assertNull(GroupNameValidator.validate("abc"));
    }

    @Test
    void tooShort() {
        assertNotNull(GroupNameValidator.validate("ab"));
    }

    @Test
    void nullName() {
        assertNotNull(GroupNameValidator.validate(null));
    }

    @Test
    void emptyName() {
        assertNotNull(GroupNameValidator.validate(""));
    }

    @Test
    void uppercase() {
        assertNotNull(GroupNameValidator.validate("MyGroup"));
    }

    @Test
    void underscores() {
        assertNotNull(GroupNameValidator.validate("my_group"));
    }

    @Test
    void startsWithDigit() {
        assertNotNull(GroupNameValidator.validate("1group"));
    }

    @Test
    void reservedName() {
        String error = GroupNameValidator.validate("www");
        assertNotNull(error);
        assertTrue(error.contains("reserved"));
    }

    @Test
    void reservedAdmin() {
        assertNotNull(GroupNameValidator.validate("admin"));
    }

    @Test
    void specialChars() {
        assertNotNull(GroupNameValidator.validate("my.group"));
    }
}
