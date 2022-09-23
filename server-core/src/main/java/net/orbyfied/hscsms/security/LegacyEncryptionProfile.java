package net.orbyfied.hscsms.security;

import net.orbyfied.hscsms.service.Logging;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class LegacyEncryptionProfile {

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
        try {
            return KeyFactory.getInstance(str);
        } catch (NoSuchAlgorithmException ignored) { }

        return null;
    }

    public static SecretKeyFactory getSecretKeyFactorySafe(String str) {
        try {
            return SecretKeyFactory.getInstance(str);
        } catch (NoSuchAlgorithmException ignored) { }

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

    public static KeyGenerator getKeyGenSafe(String str) {
        try {
            return KeyGenerator.getInstance(str);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Unknown key pair gen (algorithm): " + str);
            e.printStackTrace(Logging.ERR);
        }

        return null;
    }

    public static final LegacyEncryptionProfile RSA_UTILITY_1024
            = new LegacyEncryptionProfile("RSA", "ECB", "PKCS1Padding", "RSA", "RSA", 1024);
    public static final LegacyEncryptionProfile AES_UTILITY_128
            = new LegacyEncryptionProfile("AES", "ECB", "PKCS5Padding", "DES", "AES", 128);

    ////////////////////////////////////////////

    // the key pair
    private PublicKey  publicKey;
    private PrivateKey privateKey;
    private SecretKey  secretKey;

    // the cipher and key factory
    private Cipher cipher;
    private KeyFactory keyFactory;
    private SecretKeyFactory secretKeyFactory;

    // properties
    private String algorithm;
    private String keyAlgorithm;
    private String keyFactoryId;
    private String mode;
    private String padding;
    private int keyLength;

    public LegacyEncryptionProfile(String algorithm, String mode, String padding, String keyFactory, String keyAlgorithm, int keyLength) {
        this(
                getCipherSafe(algorithm + "/" + mode + "/" + padding),
                getKeyFactorySafe(keyFactory),
                getSecretKeyFactorySafe(keyFactory),
                keyLength
        );

        this.algorithm    = algorithm;
        this.keyAlgorithm = keyAlgorithm;
        this.keyFactoryId = keyFactory;
        this.mode         = mode;
        this.padding      = padding;
    }

    public LegacyEncryptionProfile(Cipher cipher, KeyFactory keyFactory,
                                   SecretKeyFactory secretKeyFactory, int keyLength) {
        if (cipher == null)
            throw new IllegalArgumentException();
        this.cipher = cipher;

        this.keyFactory = keyFactory;
        this.secretKeyFactory = secretKeyFactory;
        this.keyLength  = keyLength;

        this.algorithm    = cipher.getAlgorithm();
        this.keyAlgorithm = (keyFactory != null ? keyFactory.getAlgorithm() : secretKeyFactory.getAlgorithm());
    }

    public LegacyEncryptionProfile generateAsymmetricKeyPair(KeyPairGenerator generator, int len) {
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

    public LegacyEncryptionProfile generateAsymmetricKeyPair(int len) {
        return generateAsymmetricKeyPair(getKeyPairGenSafe(keyAlgorithm), len);
    }

    public LegacyEncryptionProfile generateAsymmetricKeyPair() {
        return generateAsymmetricKeyPair(getKeyPairGenSafe(keyAlgorithm), keyLength);
    }

    public LegacyEncryptionProfile generateSymmetricKey(KeyGenerator generator, int len) {
        try {
            generator.init(len);
            this.secretKey = generator.generateKey();
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            return this;
        }
    }

    public LegacyEncryptionProfile generateSymmetricKey(int len) {
        return generateSymmetricKey(getKeyGenSafe(keyAlgorithm), len);
    }

    public LegacyEncryptionProfile generateSymmetricKey() {
        return generateSymmetricKey(keyLength);
    }

    public LegacyEncryptionProfile withKeyPair(KeyPair pair) {
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

    public synchronized SecretKey getSecretKey() {
        return secretKey;
    }

    public synchronized Key getEncryptKey() {
        Key sk = getSecretKey();
        if (sk != null)
            return sk;
        return getPublicKey();
    }

    public synchronized Key getDecryptKey() {
        Key sk = getSecretKey();
        if (sk != null)
            return sk;
        return getPrivateKey();
    }

    public LegacyEncryptionProfile withPublicKey(PublicKey key) {
        this.publicKey = key;
        return this;
    }

    public LegacyEncryptionProfile withPrivateKey(PrivateKey key) {
        this.privateKey = privateKey;
        return this;
    }

    public synchronized LegacyEncryptionProfile withSecretKey(SecretKey key) {
        this.secretKey = key;
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
            Key key = getEncryptKey();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(in);
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
            return null;
        }
    }

    public byte[] decrypt(byte[] in) {
        try {
            Key key = getDecryptKey();
            cipher.init(Cipher.DECRYPT_MODE, key);
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

    public String encodeSecretKey(SecretKey key) {
        return toBase64(key.getEncoded());
    }

    public String encodeSecretKey() {
        return toBase64(secretKey.getEncoded());
    }

    public LegacyEncryptionProfile loadKeys(String priv, String pub) {
        loadPublicKey(pub);
        loadPrivateKey(priv);
        return this;
    }

    public LegacyEncryptionProfile loadPrivateKey(String str) {
        this.privateKey = decodePrivateKey(str);
        return this;
    }

    public LegacyEncryptionProfile loadPublicKey(String str) {
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

    public SecretKey decodeSecretKey(String str) {
        try {
            byte[] keyBytes = fromBase64(str);
            SecretKeySpec spec = new SecretKeySpec(keyBytes, keyAlgorithm);
            return spec;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] baAppend(byte[] arr, byte[] append) {
        // allocate new array
        byte[] res = new byte[arr.length + append.length];

        // copy into array
        System.arraycopy(arr, 0, res, 0, arr.length);
        System.arraycopy(append, 0, res, arr.length, append.length);

        // return
        return res;
    }

    public byte[] cipherBigData(byte[] bytes,
                                int mode){
        return switch (algorithm) {
            case "RSA" -> cipherBigDataRSA(bytes, mode);
            case "AES" -> cipherBigDataAES(bytes, mode);
            default -> { throw new IllegalStateException("Unknown algorithm: " + algorithm); }
        };
    }

    public byte[] cipherBigDataAES(byte[] bytes,
                                   int mode) {
        try {
            // initialize mode
            cipher.init(mode, getSecretKey());

            // do final
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] cipherBigDataRSA(byte[] bytes,
                                   int mode) {
        try {
            // initialize mode
            Key key = (mode == Cipher.ENCRYPT_MODE ? publicKey : privateKey);
            cipher.init(mode, key);

            // intermediate buffer
            byte[] intermediate;
            // final result
            byte[] result = new byte[0];

            // block size
            int blockLen = (mode == Cipher.ENCRYPT_MODE) ? 117 : 128;
            // block buffer
            byte[] buf = new byte[blockLen];

            // for each byte
            for (int i = 0; i < bytes.length; i++) {
                int blockI = i % blockLen;

                // check if we filled our buffer array
                if (i > 0 && blockI == 0) {
                    // encrypt buffer
                    intermediate = cipher.doFinal(buf);
                    // copy into result buffer
                    baAppend(result, intermediate);

                    // clear buffer array
                    int newBlockLen = blockLen;
                    if (i + blockLen > bytes.length)
                        newBlockLen = bytes.length - i;
                    buf = new byte[newBlockLen];
                }

                // copy byte to buffer
                buf[blockI] = bytes[i];
            }

            // append trailing block
            intermediate = cipher.doFinal(buf);
            baAppend(result, intermediate);

            // return
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    class EncryptionOutputStream extends OutputStream {
        final int blockSize = 117;

        // the output stream
        final OutputStream stream;
        public EncryptionOutputStream(OutputStream stream) {
            this.stream = stream;
        }

        // the encrypted data buffer
        byte[] buf = new byte[blockSize];
        // the index in the buffer to read to
        int bi = 0;

        @Override
        public void write(int b) throws IOException {
            // put byte
            byte by = (byte) b;
            buf[bi++] = by;

            // flush and write encrypted if
            // the buffer is full
            if (bi >= buf.length) {
                flush();

                // allocate new empty buffer
                buf = new byte[blockSize];
                // reset buffer index
                bi = 0;
            }
        }

        @Override
        public void flush() throws IOException {
            // encrypt and write buffer
//            System.out.println("W/DEC: " + Base64.getEncoder().encodeToString(buf));
            byte[] encrypted = encrypt(buf);
//            System.out.println("W/ENC: " + Base64.getEncoder().encodeToString(encrypted));
            stream.write(encrypted);
        }
    }

    class DecryptionInputStream extends InputStream {
        final int blockSize = 128;

        // the input stream
        InputStream stream;
        public DecryptionInputStream(InputStream stream) {
            this.stream = stream;
        }

        // the decrypted data buffer
        byte[] buf = new byte[blockSize];
        // the amount of indices read from the buffer
        int used = 0;

        @Override
        public int read() throws IOException {
            // decrypt next block if used
            if (used == 0 || used >= buf.length) {
                // read next encrypted block
                byte[] encrypted = stream.readNBytes(blockSize);
//                System.out.println("R/ENC: " + Base64.getEncoder().encodeToString(encrypted));
                // decrypt and put in buffer
                buf = decrypt(encrypted);
//                System.out.println("R/DEC: " + Base64.getEncoder().encodeToString(buf));
            }

            // read decrypted byte
            byte by = buf[used++];

            // return
            return by;
        }
    }

    public DataOutputStream encryptedOutputStream(final OutputStream stream) {
        return new DataOutputStream(new EncryptionOutputStream(stream));
    }

    public DataInputStream encryptedInputStream(final InputStream stream) {
        return new DataInputStream(new DecryptionInputStream(stream));
    }

}
