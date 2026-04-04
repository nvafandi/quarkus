package com.sales.util;

import io.quarkus.elytron.security.common.BcryptUtil;

/**
 * Password encoder utility using BCrypt
 * Wraps Quarkus's built-in BcryptUtil for easier usage
 */
public class PasswordEncoder {

    private static final int BCRYPT_ITERATIONS = 12;

    /**
     * Encode a raw password using BCrypt
     * 
     * @param rawPassword the plain text password
     * @return the BCrypt hashed password
     */
    public static String encode(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword, BCRYPT_ITERATIONS);
    }

    /**
     * Verify a raw password against an encoded hash
     * 
     * @param rawPassword the plain text password
     * @param encodedPassword the BCrypt hashed password
     * @return true if the password matches, false otherwise
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return BcryptUtil.matches(rawPassword, encodedPassword);
    }
}
