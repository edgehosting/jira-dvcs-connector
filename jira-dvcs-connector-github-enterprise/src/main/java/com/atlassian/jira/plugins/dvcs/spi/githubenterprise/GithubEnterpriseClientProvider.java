package com.atlassian.jira.plugins.dvcs.spi.githubenterprise;

import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientWithTimeout;
import com.atlassian.jira.plugins.dvcs.spi.github.RateLimit;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.eclipse.egit.github.core.client.DateFormatter;
import org.eclipse.egit.github.core.client.EventFormatter;
import org.eclipse.egit.github.core.event.Event;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Date;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_API;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_GISTS;

@Component ("githubEnterpriseClientProvider")
public class GithubEnterpriseClientProvider extends GithubClientProvider
{
    private static final String RATE_LIMIT_URI = "/api/v3/rate_limit";

    @Autowired
    public GithubEnterpriseClientProvider(AuthenticationFactory authenticationFactory,
            @ComponentImport PluginAccessor pluginAccessor)
    {
        super(authenticationFactory, pluginAccessor);
    }

    @Override
    public GithubClientWithTimeout createClientInternal(String url, String userAgent)
    {
        return createClientForGithubEnteprise(url, userAgent);
    }

    public static GithubClientWithTimeout createClientForGithubEnteprise(String url, String userAgent)
    {
        try
        {
            URL urlObject = new URL(url);
            String host = urlObject.getHost();

            if (HOST_DEFAULT.equals(host) || HOST_GISTS.equals(host))
            {
                host = HOST_API;
            }

            GithubClientWithTimeout result = new GitHubEnterpriseClient(host, -1, urlObject.getProtocol());
            result.setUserAgent(userAgent);
            return result;
        } catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }


    private static class GitHubEnterpriseClient extends GithubClientWithTimeout
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

        /**
         * @return null for GHE because there is no rate limits
         */
        @Override
        public RateLimit getRateLimit() {
            return null;
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

        @Override
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
