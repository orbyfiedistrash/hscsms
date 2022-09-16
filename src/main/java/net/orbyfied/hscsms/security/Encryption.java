package net.orbyfied.hscsms.security;

import java.util.UUID;

public class Encryption {

    /* ----- Password ----- */

    public static byte[] createUserKey(UUID userId,
                                       byte[] userPasswordHash,
                                       byte[] serverUEK) {
        return new byte[0]; // TODO
    }

    /* ----- RSA ----- */

    static int RSA_KEY_LENGTH = 4096;
    static String ALGORITHM_NAME = "RSA";
    static String PADDING_SCHEME = "OAEPWITHSHA-512ANDMGF1PADDING";
    static String MODE_OF_OPERATION = "ECB";


}
