package com.basketball.app.util;

import com.basketball.app.model.User;


public final class UserNames {

    private UserNames() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /** Display order: given name then surname */
    public static String getDisplayName(User user) {
        if (user == null) {
            return "";
        }
        return getDisplayName(user.getName(), user.getSurname());
    }

    public static String getDisplayName(String name, String surname) {
        String given = normalize(name);
        String sur = normalize(surname);
        if (!given.isEmpty() && !sur.isEmpty()) {
            return given + " " + sur;
        }
        if (!given.isEmpty()) {
            return given;
        }
        return sur;
    }

    /** Export order: surname then given name */
    public static String toSurnameAndGivenName(User user) {
        if (user == null) {
            return "";
        }
        return toSurnameAndGivenName(user.getName(), user.getSurname());
    }

    public static String toSurnameAndGivenName(String name, String surname) {
        String given = normalize(name);
        String sur = normalize(surname);
        if (!sur.isEmpty() && !given.isEmpty()) {
            return sur + " " + given;
        }
        if (!sur.isEmpty()) {
            return sur;
        }
        return given;
    }

    public static String[] splitFullName(String fullName) {
        String trimmed = normalize(fullName);
        if (trimmed.isEmpty()) {
            return new String[]{"", ""};
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        String surname = parts[parts.length - 1];
        StringBuilder given = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                given.append(' ');
            }
            given.append(parts[i]);
        }
        return new String[]{given.toString(), surname};
    }

    public static void applyFullName(User user, String fullName) {
        String[] split = splitFullName(fullName);
        user.setName(split[0]);
        user.setSurname(split[1]);
    }
}
