package com.atlassian.jira.plugins.bitbucket.api.impl;

import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.project.ProjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * An encryption service for storing passwords
 */
public class DefaultEncryptor implements Encryptor
{
    final Logger logger = LoggerFactory.getLogger(DefaultEncryptor.class);
    private final ProjectManager projectManager;

    public DefaultEncryptor(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    /**
     * Encrypt the input into a hex encoded string;
     * @param input the input to encrypt
     * @param projectKey the project key
     * @param repoURL the repository url
     * @return the encrypted string
     */
    public String encrypt(String input, String projectKey, String repoURL)
    {
        byte[] encrypted;
        try
        {
        	// TODO - Do we need projectID? Can we use projectKey instead? 
            String projectID = projectManager.getProjectObjByKey(projectKey).getId().toString();

            byte[] key = (projectID + repoURL).getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit

            // Generate the secret key specs.
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            // Instantiate the cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            encrypted = cipher.doFinal((input).getBytes());
        }
        catch (Exception e)
        {
            logger.debug("error encrypting",e);
            encrypted = new byte[0];
        }

        BigInteger bi = new BigInteger(1, encrypted);
        return String.format("%0" + (encrypted.length << 1) + "X", bi);
    }

    private static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public String decrypt(String password, String projectKey, String repoURL)
    {
    	if (password == null) 
    		return null; 
    			
        try
        {
            byte[] ciphertext = DefaultEncryptor.hexStringToByteArray(password);
            String projectID = projectManager.getProjectObjByKey(projectKey).getId().toString();

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
            byte[] original = cipher.doFinal(ciphertext);
            String originalString = new String(original);
            //logger.debug("Original string: " + originalString + "\nOriginal string (Hex): " + original.toString());

//            logger.debug("decrypted password [ " + original+" ]");
            return originalString;

        }
        catch (Exception e)
        {
            logger.debug("error encrypting",e);
        }
        return "";
    }

}
