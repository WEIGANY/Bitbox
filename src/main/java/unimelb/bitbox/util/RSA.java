package unimelb.bitbox.util;

import java.util.Base64;

import javax.crypto.Cipher;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/*
 * process the RSA encryption and decryption
 * the public key is stored in the configuration 
 * the private key should be private_key.der in the root directory
 * 
 */

public class RSA {

    public static final String CHARSET = "UTF-8";
    public static final String RSA_ALGORITHM = "RSA";
    
	public static final String PRIVATE_KEY_PATH = "private_key.der";
	public static final String ALGORITHM = "RSA";
	static String Public =          
		       "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDH+wPrKYG1KVlzQUVtBghR8n9dzcShSZo0+3KgyVdOea7Ei7vQ1U4wRn1zlI5rSqHDzFitblmqnB2anzVvdQxLQ3UqEBKBfMihnLgCSW8Xf7MCH+DSGHNvBg2xSNhcfEmnbLPLnbuz4ySn1UB0lH2eqxy50zstxhTY0binD9Y+rwIDAQAB";
	static String Private = "";
    public static final void main(String[] arg) throws Exception {

        System.out.println("public/private key start");
        String str = "qZmzy9LqvvfPn8+bVgSG7w==";//test key
        System.out.println("\rplaint text：\r\n" + str);
        System.out.println("\rsize：\r\n" + str.getBytes().length);
//        String encodedData = RSATest.publicEncrypt(str, RSATest.getPublicKey(publicKey));
        System.out.println(RSA.strToPublicKey(Configuration.getConfigurationValue("authorized_keys")));
        String encodedData = RSA.publicEncrypt1(str, RSA.strToPublicKey(Configuration.getConfigurationValue("authorized_keys")));
        System.out.println("cyphr text：\r\n" + encodedData);
        System.out.println(Private);
//        String decodedData = RSATest.privateDecrypt(encodedData, RSATest.getPrivateKey(privateKey));
        String decodedData = RSA.privateDecrypt1(encodedData, RSA.getPrivateKey1(PRIVATE_KEY_PATH));
        System.out.println("decode text: \r\n" + decodedData);
    }
    
	public static PrivateKey getPrivateKey1(String filename) throws Exception{
		byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
		
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
		return kf.generatePrivate(spec);
	}
	
    private static PrivateKey strToPrivateKey(String publicKeyString) {
    	PrivateKey privateKey = null;
        try {
        	byte[] key = Base64.getDecoder().decode((publicKeyString.getBytes()));
        	System.out.println(new String(key));
        	PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
    		KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
    		privateKey= kf.generatePrivate(spec);
    		
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
		return  privateKey;
    }
    
    public static PublicKey strToPublicKey(String publicKeyString) {
        PublicKey publicKey = null;
        try {
        	String[] pbkey= publicKeyString.split(" ");
        	System.out.println(pbkey[1]);
        	byte[] key = Base64.getDecoder().decode((pbkey[1].getBytes()));
        	byte[] bs = new byte[3];
        	System.arraycopy(key, 15, bs, 0, 3);
        	
        	BigInteger a = new BigInteger(bs);
        	byte[] bs2 = new byte[key.length-22];
        	System.arraycopy(key, 22, bs2, 0, key.length-22);
        	BigInteger b = new BigInteger(bs2);
            
        	System.out.println(a);
        	System.out.println(b);
        	RSAPublicKeySpec spec = new RSAPublicKeySpec(b,a);
        	publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        return publicKey;
    }

	public static PublicKey getPublicKey1(String filename) throws Exception{
		byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
		System.out.println(keyBytes);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
		return kf.generatePublic(spec);

	}




    
    public static String publicEncrypt1(String data, PublicKey publicKey){
        try{
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            
            byte[] p = data.getBytes(CHARSET);
            byte[] result = cipher.doFinal(p);
            String edcrypted = Base64.getEncoder().encodeToString(result);
            
            return edcrypted;
        }catch(Exception e){
            throw new RuntimeException("encrypt [" + data + "] error", e);
        }
    }

    public static String privateDecrypt1(String data, PrivateKey privateKey){
        try{
        	byte[] aesEncryptedBase64Bytes = Base64.getDecoder().decode(data);
        	
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            
            byte[] cipherByte = cipher.doFinal(aesEncryptedBase64Bytes);
            String aesDecrypted = new String(cipherByte, CHARSET);
            return aesDecrypted;
        }catch(Exception e){
            throw new RuntimeException("decrypt [" + data + "] error", e);
        }
    }
    


}

