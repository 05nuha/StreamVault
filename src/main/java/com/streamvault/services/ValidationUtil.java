package com.streamvault.services;

import java.util.regex.Pattern;

// server-side input validation shared by the auth servlets
public final class ValidationUtil {

    // pragmatic email shape check (full RFC validation is intentionally out of scope)
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_FIELD_LENGTH = 255;

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        return email != null
                && email.length() <= MAX_FIELD_LENGTH
                && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null
                && password.length() >= MIN_PASSWORD_LENGTH
                && password.length() <= MAX_FIELD_LENGTH;
    }

    public static boolean isValidName(String name) {
        return name != null
                && !name.trim().isEmpty()
                && name.length() <= MAX_FIELD_LENGTH;
    }
}
