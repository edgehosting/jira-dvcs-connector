package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

/**
 * BitbucketSSHKey
 * 
 * <pre>
 * 
 *  {
 *      "pk": 1, 
 *      "key": "ssh-dss AAAAB3NzaC1kc3MA ... .atlassian.com"
 *  }
 * 
 * </pre>
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class BitbucketSSHKey 
{
    private Integer pk;
    private String key;
    
    public Integer getPk()
    {
        return pk;
    }

    public void setPk(Integer pk)
    {
        this.pk = pk;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }
}
