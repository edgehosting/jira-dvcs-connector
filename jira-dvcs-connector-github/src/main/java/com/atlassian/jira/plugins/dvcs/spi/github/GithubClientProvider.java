package com.atlassian.jira.plugins.dvcs.spi.github;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_API;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_GISTS;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Date;

import org.eclipse.egit.github.core.client.DateFormatter;
import org.eclipse.egit.github.core.client.EventFormatter;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.EventService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.auth.impl.OAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.plugin.PluginAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GithubClientProvider
{
    private final AuthenticationFactory authenticationFactory;
    private final String userAgent;

    public GithubClientProvider(AuthenticationFactory authenticationFactory, PluginAccessor pluginAccessor)
    {
        this.authenticationFactory = authenticationFactory;
        this.userAgent = DvcsConstants.getUserAgent(pluginAccessor);
    }

    public GitHubClient createClient(Repository repository)
    {
        GitHubClient client = createClient(repository.getOrgHostUrl(), userAgent);
        OAuthAuthentication auth = (OAuthAuthentication) authenticationFactory.getAuthentication(repository);
        client.setOAuth2Token(auth.getAccessToken());

        return client;
    }

    public GitHubClient createClient(String hostUrl)
    {
        return createClient(hostUrl, userAgent);
    }
    
    public GitHubClient createClient(Organization organization)
    {
        GitHubClient client = createClient(organization.getHostUrl(), userAgent);
        Authentication authentication = authenticationFactory.getAuthentication(organization);
        if (authentication instanceof OAuthAuthentication)
        {
            OAuthAuthentication oAuth = (OAuthAuthentication) authentication;
            client.setOAuth2Token(oAuth.getAccessToken());
        } else
        {
            throw new SourceControlException("Failed to get proper OAuth instance for github client.");
        }
        return client;
    }

    public CommitService getCommitService(Repository repository)
    {
        return new CommitService(createClient(repository));
    }

    public UserService getUserService(Repository repository)
    {
        return new UserService(createClient(repository));
    }

    public RepositoryService getRepositoryService(Repository repository)
    {
        return new RepositoryService(createClient(repository));
    }

    public RepositoryService getRepositoryService(Organization organization)
    {
        return new RepositoryService(createClient(organization));
    }

    public PullRequestService getPullRequestService(Repository repository)
    {
        return new PullRequestService(createClient(repository));
    }

    public EventService getEventService(Repository repository)
    {
        return new EventService(createClient(repository));
    }

    /**
     * Create a GitHubClient to connect to the api.
     *
     * It uses the right host in case we're calling the github.com api.
     * It uses the right protocol in case we're calling the GitHub Enterprise api.
     *
     * @param url is the GitHub's oauth host.
     * @param userAgent 
     * @return a GitHubClient
     */
    public static GitHubClient createClient(String url, String userAgent)
    {
        try
        {
            URL urlObject = new URL(url);
            String host = urlObject.getHost();

            if (HOST_DEFAULT.equals(host) || HOST_GISTS.equals(host))
            {
                host = HOST_API;
            }

            GitHubClient result = new GitHubEnterpriseClient(host, -1, urlObject.getProtocol());
            result.setUserAgent(userAgent);
            return result;
        } catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
    
    private static class GitHubEnterpriseClient extends GitHubClient
    {
    	public GitHubEnterpriseClient(final String hostname, final int port,
    			final String scheme)
    	{
    		super(hostname,port,scheme);
    		gson = createGson(true);
		}
    	
    	public static final Gson createGson(final boolean serializeNulls)
    	{
    		final GsonBuilder builder = new GsonBuilder();
    		builder.registerTypeAdapter(Date.class, new ISODateFormatter());
    		builder.registerTypeAdapter(Event.class, new EventFormatter());
    		builder.setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES);
    		if (serializeNulls)
    			builder.serializeNulls();
    		return builder.create();
    	}
    }
    
	private static class ISODateFormatter implements JsonDeserializer<Date>, JsonSerializer<Date>
	{

		private final Logger log = LoggerFactory.getLogger(ISODateFormatter.class);
		
		private final DateFormatter dateFormatter = new DateFormatter();
		
		
		/**
		 * Create date formatter
		 */
		public ISODateFormatter()
		{
		}

		public Date deserialize(JsonElement json, Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException
		{
			final String value = json.getAsString();
			
			DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
			try
			{
				return fmt.parseDateTime(value).toDate();
			} catch (IllegalArgumentException e)
			{
				log.debug("Could not parse '" + value + "'.", e);
			}
			
			// let's try eGit dateFormatter
			return dateFormatter.deserialize(json, typeOfT, context);
		}

		@Override
		public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context)
		{
			
			return dateFormatter.serialize(src, typeOfSrc, context);
		}
	}
}
