package com.vaavud.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

public final class UUIDUtil {

	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}
	
	public static String md5Hash(String text) {
		String digest = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(text.getBytes("UTF-8")); 
			StringBuilder sb = new StringBuilder(2*hash.length);
			for(byte b : hash){
				sb.append(String.format("%02x", b&0xff)); 
			}
			digest = sb.toString().toUpperCase(Locale.US);
		}
		catch (UnsupportedEncodingException e) {
//			Log.e("UUIDUtil",e.toString());
		}
		catch (NoSuchAlgorithmException e) {
//			Log.e("UUIDUtil",e.toString());
		}
		return digest;
	}

	private UUIDUtil() {
	}
}
