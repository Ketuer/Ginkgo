package net.ginkgo.server.security;

import net.ginkgo.server.core.GinkgoSessionManager;
import net.ginkgo.server.entity.PacketHandshake;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

/**
 * RSA 非对称加密算法（默认）
 *
 *   Server             Client
 *    生成 --> 发布公钥 --> 公钥
 *  私钥(加密)   -->    公钥(解密)
 *  私钥(解密)   <--    公钥(加密)
 */
public class RSAPacketEncryption extends AbstractPacketEncryption{

    private byte[] publicKey;
    private byte[] privateKey;

    public RSAPacketEncryption(){
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(1024, new SecureRandom());
            KeyPair keyPair = keyPairGen.generateKeyPair();
            privateKey = keyPair.getPrivate().getEncoded();
            publicKey = keyPair.getPublic().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] encode(byte[] message) {
        try {
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return segmentDoFinal(message, 117, cipher);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption error!");
        }
    }

    /**
     * 公钥加密 -> 私钥解密
     * @param bytes 密文
     * @return 明文
     */
    @Override
    public byte[] decode(byte[] bytes)  {
        try {
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec =  new PKCS8EncodedKeySpec(privateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return segmentDoFinal(bytes, 128, cipher);
        }catch (GeneralSecurityException e){
            throw new IllegalStateException("Decryption error: "+e.getMessage());
        }
    }

    @Override
    public PacketHandshake handshake() {
        PacketHandshake packet = new PacketHandshake();
        packet.set("RSA", publicKey, GinkgoSessionManager.createNewSession().getID());
        return packet;
    }

    /**
     * 分段加密/解密
     * @param bytes 数据
     * @param limit 段大小
     * @return 密文/明文
     */
    private byte[] segmentDoFinal(byte[] bytes, int limit, Cipher cipher) throws GeneralSecurityException{
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
    }
}
