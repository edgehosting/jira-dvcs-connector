package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang3.StringUtils;


/**
 * Describes a user 
 */
public class DvcsUser
{
    
    public static class UnknownUser extends DvcsUser
    {
        public UnknownUser(String author, String raw_author, String hostUrl)
        {
            super(author, extractFullNameFromRawAuthor(raw_author), raw_author, "https://secure.gravatar.com/avatar/unknown?d=mm", hostUrl);
        }
        
        /**
         * Converts "First Last <email@domain.com>" to "First Last"
         * @param raw_author
         * @return
         */
        private static String extractFullNameFromRawAuthor(String raw_author)
        {
            if (StringUtils.isBlank(raw_author))
            {
                return raw_author;
            }
            return raw_author.replaceAll("<.*>", "").trim();
        }
    }

    private final String username;
    private final String fullName;
    private final String avatar;
    private final String url;
    private String rawAuthor;

    public DvcsUser(String username, String fullName, String rawAuthor, String avatar, String hostUrl)
    {
        this.username = username;
        this.fullName = fullName;
        this.rawAuthor = rawAuthor;
        this.avatar = avatar;
        this.url = hostUrl +"/"+ username;
    }

    public String getUsername()
    {
        return username;
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getAvatar()
    {
        return avatar;
    }
    
    public String getUrl()
    {
        return url;
    }

    public void setRawAuthor(String rawAuthor)
    {
        this.rawAuthor = rawAuthor;
    }

    public String getRawAuthor()
    {
        return rawAuthor;
    }

}
