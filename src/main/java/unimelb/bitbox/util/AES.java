package unimelb.bitbox.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
     
import java.util.Base64;
/*
 * process the AES key Initialization
 * process the AES encryption and decryption
 * 
 */

public class AES {
	
    
    private static String ALGORITHM = "AES";
    
    private static String ALGORITHM_STR = "AES/ECB/ISO10126Padding";
	
    public static byte[] aesInitKey() throws Exception{
    	//Initialization
    	KeyGenerator kgen = KeyGenerator.getInstance(ALGORITHM);
    	//Set the length of the secret key
    	kgen.init(128);
    	//Generate the secret key
    	SecretKey skey = kgen.generateKey();
    	//Return the AES secret key in byte list
    	return skey.getEncoded();
    }
    
    public static String aesEncrypt(String plainText, byte[] key) throws Exception {
    	
    	//create key
    	SecretKeySpec skey = new SecretKeySpec (key, ALGORITHM);
    	
    	Cipher cipher = Cipher.getInstance(ALGORITHM_STR);
        cipher.init(Cipher.ENCRYPT_MODE, skey);
        byte[] p = plainText.getBytes("UTF-8");
        byte[] result = cipher.doFinal(p);
        String aesEncrypted = Base64.getEncoder().encodeToString(result);
        return aesEncrypted;

    }
    
    public static String aesDecrypt(String aesEncrypted, byte[] key) throws Exception {
    	//byte[] aesEncryptedBase64Bytes = aesEncrypted.getBytes();
    	byte[] aesEncryptedBase64Bytes = Base64.getDecoder().decode(aesEncrypted);
    	SecretKeySpec skey = new SecretKeySpec (key,ALGORITHM);
    	Cipher cipher = Cipher.getInstance(ALGORITHM_STR);
    	cipher.init(Cipher.DECRYPT_MODE, skey);
    	byte[] cipherByte = cipher.doFinal(aesEncryptedBase64Bytes);
    	//String aesDecrypted = Base64.getEncoder().encodeToString(cipherByte);
    	String aesDecrypted = new String(cipherByte, "UTF-8");
    	return aesDecrypted;
    } 
    
    public static void main(String[] args) throws Exception{
    	
    	String clearText = "Hello, my name's David. and nice to meet you!";
    	System.out.print("Original text message:");
    	System.out.println(clearText);
        //Return the binary code of the secret key
        byte[] key = AES.aesInitKey();
        
        //print Base64 encode key
    	String key_str = Base64.getEncoder().encodeToString(key);
    	System.out.print("Secret key encoeded in Base64:");
        System.out.println(key_str);
    	
        //print cipher text
        String aesEncryptedText = AES.aesEncrypt(clearText, key);
        System.out.print("Encrypted string:");
        System.out.println(aesEncryptedText);
       	
        //print decipher text
        String decreptedText = AES.aesDecrypt(aesEncryptedText, key);
        System.out.print("Decrypted string:");
        System.out.println(decreptedText);
    }
}
