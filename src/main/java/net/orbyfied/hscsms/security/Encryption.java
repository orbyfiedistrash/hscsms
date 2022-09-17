package net.orbyfied.hscsms.security;

import java.util.UUID;

public class Encryption {

    /* ----- Password ----- */

    public static byte[] createUserKey(UUID userId,
                                       byte[] userPasswordHash,
                                       byte[] serverUEK) {
        return new byte[0]; // TODO
    }

}
