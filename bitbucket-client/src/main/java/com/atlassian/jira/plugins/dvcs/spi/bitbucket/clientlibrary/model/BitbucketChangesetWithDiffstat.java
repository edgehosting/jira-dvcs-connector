package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

/**
 * BitbucketChangesetDiffstatEnvelope
 * 
 * <pre>
 * 
 *  {
 *       "type": "modified", 
 *       "file": "piston/emitters.py", 
 *       "diffstat": {
 *          "removed": 2, 
 *          "added": 2
 *      }
 * }
 * 
 * </pre>
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class BitbucketChangesetWithDiffstat {

    private String type;
    private String file;
    private BitbucketDiffstat diffstat;

    
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getFile()
    {
        return file;
    }

    public void setFile(String file)
    {
        this.file = file;
    }

    public BitbucketDiffstat getDiffstat()
    {
        return diffstat;
    }

    public void setDiffstat(BitbucketDiffstat diffstat)
    {
        this.diffstat = diffstat;
    }
}
