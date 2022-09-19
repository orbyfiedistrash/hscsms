package net.orbyfied.hscsms.security;

import net.orbyfied.hscsms.service.Logging;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptionProfile {

    public static Cipher getCipherSafe(String str) {
        try {
            return Cipher.getInstance(str);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Unknown cipher (algorithm): " + str);
            e.printStackTrace(Logging.ERR);
        } catch (NoSuchPaddingException e) {
            System.err.println("Unknown cipher (padding): " + str);
            e.printStackTrace(Logging.ERR);
        }

        return null;
    }

    public static KeyFactory getKeyFactorySafe(String str) {
        if (str.equals("AES"))
            str = "RSA";
        try {
            return KeyFactory.getInstance(str);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Unknown key factory (algorithm): " + str);
            e.printStackTrace(Logging.ERR);
        }

        return null;
    }

    public static KeyPairGenerator getKeyPairGenSafe(String str) {
        try {
            return KeyPairGenerator.getInstance(str);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Unknown key pair gen (algorithm): " + str);
            e.printStackTrace(Logging.ERR);
        }

        return null;
    }

    public static final EncryptionProfile RSA_UTILITY_1024
            = new EncryptionProfile("RSA", "ECB", "PKCS1Padding", 1024);
    public static final EncryptionProfile AES_UTILITY_128
            = new EncryptionProfile("AES", "ECB", "PKCS5Padding", 128);

    ////////////////////////////////////////////

    // the key pair
    private PublicKey  publicKey;
    private PrivateKey privateKey;

    // the cipher and key factory
    private Cipher cipher;
    private KeyFactory keyFactory;

    // properties
    private String algorithm;
    private String mode;
    private String padding;
    private int keyLength;

    public EncryptionProfile(String algorithm, String mode, String padding, int keyLength) {
        this(
                getCipherSafe(algorithm + "/" + mode + "/" + padding),
                getKeyFactorySafe(algorithm),
                keyLength
        );

        this.algorithm = algorithm;
        this.mode      = mode;
        this.padding   = padding;
    }

    public EncryptionProfile(Cipher cipher, KeyFactory keyFactory, int keyLength) {
        if (cipher == null || keyFactory == null)
            throw new IllegalArgumentException();
        this.cipher     = cipher;
        this.keyFactory = keyFactory;
        this.keyLength  = keyLength;

        this.algorithm = cipher.getAlgorithm();
    }

    public EncryptionProfile generateKeyPair(KeyPairGenerator generator, int len) {
        try {
            generator.initialize(len);
            KeyPair pair = generator.generateKeyPair();
            this.publicKey  = pair.getPublic();
            this.privateKey = pair.getPrivate();
            return this;
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
            return this;
        }
    }

    public EncryptionProfile generateKeyPair(int len) {
        return generateKeyPair(getKeyPairGenSafe(algorithm), len);
    }

    public EncryptionProfile withKeyPair(KeyPair pair) {
        this.publicKey  = pair.getPublic();
        this.privateKey = pair.getPrivate();
        return this;
    }

    public Cipher getCipher() {
        return cipher;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public String getMode() {
        return mode;
    }

    public String getPadding() {
        return padding;
    }

    public KeyFactory getKeyFactory() {
        return keyFactory;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public EncryptionProfile withPublicKey(PublicKey key) {
        this.publicKey = key;
        return this;
    }

    public EncryptionProfile withPrivateKey(PrivateKey key) {
        this.privateKey = privateKey;
        return this;
    }

    public String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public byte[] fromBase64(String str) {
        return Base64.getDecoder().decode(str);
    }

    public String encryptToBase64(String str) {
        return toBase64(encrypt(str));
    }

    public String encryptToBase64(byte[] bytes) {
        return toBase64(encrypt(bytes));
    }

    public byte[] encrypt(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        return encrypt(bytes);
    }

    public byte[] encrypt(byte[] in) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(in);
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
            return null;
        }
    }

    public byte[] decrypt(byte[] in) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return cipher.doFinal(in);
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
            return null;
        }
    }

    public String decryptToUTF8(byte[] bytes) {
        bytes = decrypt(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] decryptFromBase64(String str) {
        return decrypt(fromBase64(str));
    }

    public String decryptFromBase64ToUTF8(String str) {
        return decryptToUTF8(fromBase64(str));
    }

    public String encodePublicKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String encodePrivateKey() {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public String encodePrivateKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public EncryptionProfile loadKeys(String priv, String pub) {
        loadPublicKey(pub);
        loadPrivateKey(priv);
        return this;
    }

    public EncryptionProfile loadPrivateKey(String str) {
        this.privateKey = decodePrivateKey(str);
        return this;
    }

    public EncryptionProfile loadPublicKey(String str) {
        try {
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(str));
            publicKey = keyFactory.generatePublic(publicSpec);
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
        }
        return this;
    }

    public PrivateKey decodePrivateKey(String str) {
        try {
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(str));
            return keyFactory.generatePrivate(privateSpec);
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
            return null;
        }
    }

    public PublicKey decodePublicKey(String str) {
        try {
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(str));
            return keyFactory.generatePublic(publicSpec);
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
            return null;
        }
    }

}
