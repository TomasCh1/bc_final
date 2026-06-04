package com.basketball.app.util;

import com.basketball.app.model.User;

/**
 * Formats player names for exports that expect "Priezvisko meno"
 */
public final class PlayerNameFormatter {

    private PlayerNameFormatter() {
    }

    public static String toSurnameAndGivenName(User user) {
        return UserNames.toSurnameAndGivenName(user);
    }

    public static String toSurnameAndGivenName(String fullName) {
        if (fullName == null) {
            return "";
        }
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String[] split = UserNames.splitFullName(trimmed);
        return UserNames.toSurnameAndGivenName(split[0], split[1]);
    }
}
