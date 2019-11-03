package website;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
 
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Cryption{
	
	private String QRPath;
	private static SecretKeySpec secretKey;
	private static byte[] key;
	private static int QRSize;
	
	public void setQRPath (String QRPath, int QRSize) {
		this.QRPath = QRPath;
		this.QRSize = QRSize;
	}
	
	public static void setKey(String myKey) {
		MessageDigest sha = null;
		try {
			key = myKey.getBytes("UTF-8");
			sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key,  16);
			secretKey = new SecretKeySpec(key, "AES");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public static String encrypt(String strInput, String secret) {
		try {
			setKey(secret);
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strInput.getBytes("UTF-8")));
		}
		catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}
	
	public static String decrypt(String strInput, String secret) {
		try {
			setKey(secret);
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strInput)));
		}
		catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		return null;
	}
	
	public static String encryptProfile(Profile profile, String passward) {
		String pidStr = Integer.toString(profile.getPid());
		String pidStrFilled = String.format("%0"+ (8 - pidStr.length() )+"d%s",0 ,pidStr);
		String strToEncrypt = pidStrFilled + profile.getDescription();
		String encryptedProfile = encrypt(strToEncrypt, passward);
		return encryptedProfile;
	}
	
	
	/*
	 * String first eight digits: pid
	 */
	public static Profile decryptProfile(String encrypted, String passward) {
		String decrypted = decrypt(encrypted, passward);
		
		String description = decrypted.substring(8);
		String pidStr = decrypted.substring(0,8);
		int pid = Integer.parseInt(pidStr);
		
		Profile profile = new Profile(pid, description);
		return profile;
	}
	
	public static String encodeQR(Profile profile, String passward) throws IOException {
		String strForQR = encryptProfile(profile, passward);
		String requestTemplate = "https://api.qrserver.com/v1/create-qr-code/?data=%s&size=%dx%d";
		String requestUrlStr = String.format(requestTemplate, strForQR, QRSize, QRSize);
		Document htmlPage = Jsoup.connect(requestUrlStr).get();
		Elements img = htmlPage.getElementsByTag("img");
		String imgSrc = img.attr("src");
		return imgSrc;
	}
	
	/*
	 * argument 
	 * resultJsonStr: the returned (json form) string from QR Api
	 * passward: patient password used for decryption
	 * correctPid: patient id. If the pid in decrypted string does not agree, raise exception
	 */
	public static String decodeQR(String resultJsonStr, String password, int correctPid) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(resultJsonStr);
		String encrypted = (String) json.get("data");
		String decrypted = decrypt(encrypted, password);
		String pidStr = decrypted.substring(0,8);
		int pid = Integer.parseInt(pidStr);
		if(pid != correctPid) {
			throw new IllegalArgumentException("Wrong patient ID");
		}
		String description = decrypted.substring(8);
		return description;
	}
}