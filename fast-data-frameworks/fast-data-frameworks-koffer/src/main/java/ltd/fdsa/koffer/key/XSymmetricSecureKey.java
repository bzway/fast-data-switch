package ltd.fdsa.koffer.key;

/**
 * 对称密钥
 */
public final class XSymmetricSecureKey extends XSecureKey implements XSymmetricKey {
    private static final long serialVersionUID = -2932869368903909669L;

    private final byte[] secretKey;
    private final byte[] iv;

    public XSymmetricSecureKey(
            String algorithm, int keysize, int ivsize, String password, byte[] key, byte[] iv) {
        super(algorithm, keysize, ivsize, password);
        this.secretKey = key;
        this.iv = iv;
    }

    public byte[] getEncryptKey() {
        return secretKey;
    }

    public byte[] getDecryptKey() {
        return secretKey;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public byte[] getIvParameter() {
        return iv;
    }
}