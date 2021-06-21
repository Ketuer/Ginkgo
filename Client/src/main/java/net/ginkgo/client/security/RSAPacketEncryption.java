package net.ginkgo.client.security;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class RSAPacketEncryption extends AbstractPacketEncryption{

    private final byte[] publicKey;
    public RSAPacketEncryption(byte[] publicKey){
        this.publicKey = publicKey;
    }

    @Override
    public byte[] encode(byte[] message) {
        try {
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            return this.segmentDoFinal(message, 117, cipher);
        } catch (GeneralSecurityException  e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Encode error!");
    }

    @Override
    public byte[] decode(byte[] bytes)  {
        try {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return this.segmentDoFinal(bytes, 128, cipher);
        }catch (GeneralSecurityException e){
            e.printStackTrace();
        }
        throw new IllegalStateException("Decode error!");
    }

    /**
     * 分段加密/解密
     * @param bytes 数据
     * @param limit 段大小
     * @return 密文/明文
     */
    private byte[] segmentDoFinal(byte[] bytes, int limit, Cipher cipher){
        try{
            int offSet = 0, inputLength = bytes.length;
            byte[] resultBytes = {}, cache;
            while (inputLength - offSet > 0) {
                if (inputLength - offSet > limit) {
                    cache = cipher.doFinal(bytes, offSet, limit);
                    offSet += limit;
                } else {
                    cache = cipher.doFinal(bytes, offSet, inputLength - offSet);
                    offSet = inputLength;
                }
                resultBytes = Arrays.copyOf(resultBytes, resultBytes.length + cache.length);
                System.arraycopy(cache, 0, resultBytes, resultBytes.length - cache.length, cache.length);
            }
            return resultBytes;
        }catch (GeneralSecurityException e){
            e.printStackTrace();
        }
        throw new IllegalStateException("Decryption error!");
    }
}
