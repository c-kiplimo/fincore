package com.collins.platform.commons.jdbc;

import java.security.SecureRandom;

/** Readable, prefixed ids such as {@code pi_7f3k9q2m1x8z} (Stripe style: the prefix tells you what it is). */
public final class Ids {

    private static final char[] ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static String newId(String prefix) {
        char[] chars = new char[14];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return prefix + "_" + new String(chars);
    }
}
