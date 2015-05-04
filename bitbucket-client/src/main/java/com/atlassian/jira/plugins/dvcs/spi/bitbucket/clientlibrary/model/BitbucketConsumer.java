package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * Maps the Bitbucket consumer information about the OAuth, e.g. :
 *   {
 *      "name": "Test_OAuth_1430118361594",
 *      "url": "",
 *      "secret": "8S6wwpSs46KWKUVZkhS3rmxjnjUNZP2j",
 *      "key": "53kdEswM3Se5pPRWCD",
 *      "id": 173461,
 *      "description": "Test OAuth Description [Test_OAuth_1430118361594]"
 *   }
 */
public class BitbucketConsumer implements Serializable
{
    private Long id;
    private String name;
    private String description;
    private String key;
    private String secret;
    private String url;

    public Long getId()
    {
        return id;
    }

    public void setId(final Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(final String key)
    {
        this.key = key;
    }

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(final String secret)
    {
        this.secret = secret;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }
}
