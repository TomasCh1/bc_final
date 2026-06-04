package com.basketball.app.util;

import java.security.SecureRandom;

/**
 * Utility class for generating secure random passwords
 */
public class PasswordGenerator {
    
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL_CHARS = UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL;
    
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Generate a secure random password
     */
    public static String generate(int length) {
        if (length < 8) {
            length = 12;
        }
        
        StringBuilder password = new StringBuilder();
        password.append(UPPER_CASE.charAt(random.nextInt(UPPER_CASE.length())));
        password.append(LOWER_CASE.charAt(random.nextInt(LOWER_CASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
        
        int remaining = length - 4;
        for (int i = 0; i < remaining; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }
        
        return shuffle(password.toString());
    }
    
    /**
     * Generate a default 12-character password
     */
    public static String generate() {
        return generate(12);
    }
    
    /**
     * Shuffle a string randomly
     */
    private static String shuffle(String input) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }
}

