package com.atlassian.jira.plugins.dvcs.auth;

public interface OAuthStore
{
    public class Host
    {
        public static final Host BITBUCKET = new Host("bitbucket", "https://bitbucket.org");
        public static final Host GITHUB = new Host("github", "https://github.com");

        public final String url;
        public final String id;

        public Host(String id, String url)
        {
            this.url = url;
            this.id = id;
        }
    }
    
    void store(Host host, String clientId, String secret);
    String getClientId(String hostId);
    String getSecret(String hostId);
    String getUrl(String hostId);
}
