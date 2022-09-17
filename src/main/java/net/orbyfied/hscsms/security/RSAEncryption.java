package net.orbyfied.hscsms.security;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAEncryption {

    public static final RSAEncryption UTILITY = new RSAEncryption();

    // the key pair
    private PublicKey  publicKey;
    private PrivateKey privateKey;

    // the cipher and key factory
    private Cipher cipher;
    private KeyFactory keyFactory;

    public RSAEncryption() {
        try {
            this.cipher     = Cipher.getInstance("RSA");
            this.keyFactory = KeyFactory.getInstance("RSA");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RSAEncryption withKeyPair(KeyPair pair) {
        this.publicKey  = pair.getPublic();
        this.privateKey = pair.getPrivate();
        return this;
    }

    public Cipher getCipher() {
        return cipher;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String encrypt(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        bytes = encrypt(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public byte[] encrypt(byte[] in) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decrypt(byte[] in) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return cipher.doFinal(in);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decrypt(String str) {
        byte[] bytes = Base64.getDecoder().decode(str);
        bytes = decrypt(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String encodePublicKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String encodePrivateKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public RSAEncryption loadKeys(String priv, String pub) {
        loadPublicKey(pub);
        loadPrivateKey(priv);
        return this;
    }

    public RSAEncryption loadPrivateKey(String str) {
        this.privateKey = decodePrivateKey(str);
        return this;
    }

    public RSAEncryption loadPublicKey(String str) {
        try {
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(str));
            publicKey = keyFactory.generatePublic(publicSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public PrivateKey decodePrivateKey(String str) {
        try {
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(str));
            return keyFactory.generatePrivate(privateSpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public PublicKey decodePublicKey(String str) {
        try {
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(str));
            return keyFactory.generatePublic(publicSpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
