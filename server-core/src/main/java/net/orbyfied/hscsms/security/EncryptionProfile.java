package net.orbyfied.hscsms.security;

import java.security.Key;

/**
 * General utility encryption handler, replacement
 * for {@link LegacyEncryptionProfile}
 */
@SuppressWarnings("rawtypes")
public abstract class EncryptionProfile<S extends EncryptionProfile> {

    /* -------------------------------- */

    public enum CipherType {

        SYMMETRIC,
        ASYMMETRIC

    }

    /* -------------------------------- */

    @SuppressWarnings("unchecked")
    private final S self = (S) this;

    // cipher properties
    private final CipherType cipherType;
    private String algorithm;
    private String mode;
    private String padding;

    public EncryptionProfile(CipherType type) {
        this.cipherType = type;
    }

    public CipherType getCipherType() {
        return cipherType;
    }

    public EncryptionProfile initialize(String algorithm, String mode, String padding,
                                        String keyAlgorithm, int keyLength) {
        // set properties basic
        this.algorithm = algorithm;
        this.mode      = mode;
        this.padding   = padding;

        try {

        } catch (Exception e) {
            e.printStackTrace();
        }

        // return
        return self;
    }

    // keys
    public abstract Key getEncryptionKey();
    public abstract Key getDecryptionKey();

}
