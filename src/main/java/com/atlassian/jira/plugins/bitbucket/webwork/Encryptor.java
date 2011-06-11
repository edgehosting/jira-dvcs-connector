package com.atlassian.jira.plugins.bitbucket.webwork;

/**
 * Created by IntelliJ IDEA.
 * User: michaelbuckbee
 * Date: 6/10/11
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */

import com.atlassian.jira.ComponentManager;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class Encryptor {

    final Logger logger = LoggerFactory.getLogger(Encryptor.class);

    final PluginSettingsFactory pluginSettingsFactory;
    private ComponentManager cm = ComponentManager.getInstance();

    public Encryptor(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

  public byte[] encrypt(String inputPlaintext, String projectKey, String repoURL){

    try{

        String projectID = cm.getProjectManager().getProjectObjByKey(projectKey).getId().toString();

        // Get the Key
        byte[] key = (projectID + repoURL).getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bit

        // Generate the secret key specs.
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // Instantiate the cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

        byte[] encrypted = cipher.doFinal((inputPlaintext).getBytes());
        logger.debug("encrypted string: " + encrypted.toString());

        return encrypted;

    }catch (Exception e){
        logger.debug("Encryptor.encrypt");
        e.printStackTrace();
    }

    return "".getBytes();

  }

    public static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public String decrypt(byte[] encrypted, String projectKey, String repoURL){
        try{
            String projectID = cm.getProjectManager().getProjectObjByKey(projectKey).getId().toString();

            // Get the Key
            byte[] key = (projectID + repoURL).getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit

            // Generate the secret key specs.
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            // Instantiate the cipher
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] original = cipher.doFinal(encrypted);
            String originalString = new String(original);
            //logger.debug("Original string: " + originalString + "\nOriginal string (Hex): " + original.toString());

            logger.debug("Decrypted: " + original);
            return originalString;

        }catch (Exception e){
            logger.debug("Encryptor.decrypt");
            e.printStackTrace();
        }

        return "";



    }

}
