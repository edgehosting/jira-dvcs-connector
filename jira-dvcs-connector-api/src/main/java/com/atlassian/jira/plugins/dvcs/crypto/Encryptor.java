package com.atlassian.jira.plugins.dvcs.crypto;

/**
 * An encryption service for storing passwords
 */
public interface Encryptor
{
    /**
     * Encrypt the input into a hex encoded string;
     * @param input the input to encrypt
     * @param organizationName the project key
     * @param hostUrl the repository url
     * @return the encrypted string
     */
    public String encrypt(String input, String organizationName, String hostUrl);

    /**
     * decrypt the hex encoded input
     * @param input the hex encoded inpud
     * @param organizationName the project key
     * @param hostUrl the repository url
     * @return the decrypted sting
     */
    public String decrypt(String input, String organizationName, String hostUrl);

}
