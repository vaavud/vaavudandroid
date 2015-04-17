package com.vaavud.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PasswordUtil {

	private static final String HASH_ALGORITHM = "HmacSHA256";
    // The following constants may be changed without breaking existing hashes.
    
    /**
     * Returns a salted hmacSHA256 hash of the password.
     *
     * @param   password    the password to hash
     * @return              a salted hmacSHA256 hash of the password
     * @throws InvalidKeyException 
     */
    public static String createHash(String password, String salt) throws InvalidKeyException {
        return createHash(password.getBytes(),salt.getBytes());
    }
    /**
     * Returns a salted hmacSHA256 hash of the password.
     *
     * @param   password    the password to hash
     * @return              a salted hmacSHA256 hash of the password
     * @throws InvalidKeyException 
     */
    public static String createHash(String password) throws InvalidKeyException {
        return createHash(password.getBytes(),null);
    }

    /**
     * Returns a salted hash of the password.
     *
     * @param   password    the password to hash
     * @return              a salted hmacSHA256 hash of the password
     * @throws InvalidKeyException 
     */
    public static String createHash(byte[] password, byte[] userSalt) throws InvalidKeyException {
    	try {
            // Hash the password
            byte[] hash = hmacSHA256(password, userSalt);
            return toHex(hash);
    	}
    	catch (InvalidKeySpecException e) {
    		throw new RuntimeException(e);
    	}
    	catch (NoSuchAlgorithmException e) {
    		throw new RuntimeException(e);
		}
    }


    /**
     *  Computes the hmacSHA256 hash of a password.
     *
     * @param   password    the password to hash.
     * @param   salt        the salt
     * @param   iterations  the iteration count (slowness factor)
     * @param   bytes       the length of the hash to compute in bytes
     * @return              the hmacSHA256 hash of the password
     * @throws InvalidKeyException 
     */
    private static byte[] hmacSHA256(byte[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
    	Mac sha256_HMAC = Mac.getInstance(HASH_ALGORITHM);
    	SecretKeySpec secret_key = new SecretKeySpec(salt,HASH_ALGORITHM);
		sha256_HMAC.init(secret_key);
		
		return sha256_HMAC.doFinal(password);
    }

    /**
     * Converts a string of hexadecimal characters into a byte array.
     *
     * @param   hex         the hex string
     * @return              the hex string decoded into a byte array
     */
    private static byte[] fromHex(String hex) {
        byte[] binary = new byte[hex.length() / 2];
        for (int i = 0; i < binary.length; i++) {
            binary[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
        }
        return binary;
    }

    /**
     * Converts a byte array into a hexadecimal string.
     *
     * @param   array       the byte array to convert
     * @return              a length*2 character string encoding the byte array
     */
    private static String toHex(byte[] array) {
    	
    	StringBuffer hexString = new StringBuffer();
    	for (int i=0;i<array.length;i++) {
    		String hex = Integer.toHexString(0xFF & array[i]);
    		if (hex.length() == 1) {
    		    // could use a for loop, but we're only dealing with a single byte
    		    hexString.append('0');
    		}
    		hexString.append(hex);
	    }
    	//Log.d("PasswordUtil", "Hex string with format: "+ hexString.toString().toUpperCase());
    	return hexString.toString().toUpperCase();
    }

}
