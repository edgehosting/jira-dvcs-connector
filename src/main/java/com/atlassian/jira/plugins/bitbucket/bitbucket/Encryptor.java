package com.atlassian.jira.plugins.bitbucket.bitbucket;

/**
 * An encryption service for storing passwords
 */
public interface Encryptor
{
    /**
     * Encrypt the input into a hex encoded string;
     * @param input the input to encrypt
     * @param projectKey the project key
     * @param repoURL the repository url
     * @return the encrypted string
     */
    public String encrypt(String input, String projectKey, String repoURL);

    /**
     * decrypt the hex encoded input
     * @param input the hex encoded inpud
     * @param projectKey the project key
     * @param repoURL the repository url
     * @return the decrypted sting
     */
    public String decrypt(String input, String projectKey, String repoURL);

}
