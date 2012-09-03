package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceField;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.DetailedChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.DvcsUserTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.GroupTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.RepositoryTransformer;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.Retryer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.PluginAccessor;

/**
 * The Class BitbucketCommunicator.
 * 
 * TODO get changesets remains in 'old-style'
 */
public class BitbucketCommunicator implements DvcsCommunicator
{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(BitbucketCommunicator.class);

    /** The Constant BITBUCKET. */
    public static final String BITBUCKET = "bitbucket";

    private static final String PLUGIN_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";

    private final BitbucketLinker bitbucketLinker;

    private final String pluginVersion;

    private final BitbucketOAuth oauth;

    private final BitbucketClientRemoteFactory bitbucketClientRemoteFactory;

    /**
     * The Constructor.
     * 
     * @param authenticationFactory
     *            the authentication factory
     * @param requestHelper
     *            the request helper
     */
    public BitbucketCommunicator(AuthenticationFactory authenticationFactory,
            @Qualifier("defferedBitbucketLinker") BitbucketLinker bitbucketLinker, PluginAccessor pluginAccessor,
            BitbucketOAuth oauth, BitbucketClientRemoteFactory bitbucketClientRemoteFactory)
    {
        this.bitbucketLinker = bitbucketLinker;
        this.oauth = oauth;
        this.bitbucketClientRemoteFactory = bitbucketClientRemoteFactory;
        this.pluginVersion = getPluginVersion(pluginAccessor);
    }

    protected String getPluginVersion(PluginAccessor pluginAccessor)
    {
        return pluginAccessor.getPlugin(PLUGIN_KEY).getPluginInformation().getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDvcsType()
    {
        return BITBUCKET;
    }

    @Override
    public boolean isOauthConfigured()
    {
        return StringUtils.isNotBlank(oauth.getClientId()) && StringUtils.isNotBlank(oauth.getClientSecret());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getNoAuthClient(hostUrl);

            // just to call the rest
            remoteClient.getAccountRest().getUser(accountName);
            boolean requiresOauth = StringUtils.isBlank(oauth.getClientId())
                    || StringUtils.isBlank(oauth.getClientSecret());

            return new AccountInfo(BitbucketCommunicator.BITBUCKET, requiresOauth);
        } catch (BitbucketRequestException e)
        {
            return null;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Repository> getRepositories(Organization organization)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForOrganization(organization);

            List<BitbucketRepository> repositories = remoteClient.getRepositoriesRest().getAllRepositories(
                    organization.getName());

            return RepositoryTransformer.fromBitbucketRepositories(repositories);
        }

        catch (BitbucketRequestException.Unauthorized_401 e)
        {
            log.debug("Invalid credentials", e);
            throw new SourceControlException("Invalid credentials");
        } catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Changeset getDetailChangeset(Repository repository, Changeset changeset)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForRepository(repository);
            List<BitbucketChangesetWithDiffstat> changesetDiffStat = remoteClient.getChangesetsRest()
                    .getChangesetDiffStat(repository.getOrgName(), // owner
                            repository.getSlug(), changeset.getNode(), Changeset.MAX_VISIBLE_FILES); // limit

            return DetailedChangesetTransformer.fromChangesetAndBitbucketDiffstats(changeset, changesetDiffStat);
        } catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get result", e);
        }

    }

    public List<Changeset> getChangesets(final Repository repository, final String startNode, final int limit,
            final Date lastCommitDate)
    {
        return new Retryer<List<Changeset>>().retry(new Callable<List<Changeset>>()
        {
            @Override
            public List<Changeset> call()
            {
                return getChangesetsInternal(repository, startNode, limit, lastCommitDate);
            }
        });

    }

    private List<Changeset> getChangesetsInternal(final Repository repository, String startNode, int limit,
            Date lastCommitDate)
    {
        String owner = repository.getOrgName();
        String slug = repository.getSlug();

        BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForRepository(repository);

        log.debug("Parse bitbucket changesets [ {} ] [ {} ] [ {} ] [ {} ]", new Object[] { owner, slug, startNode,
                limit });

        Map<String, String> params = new HashMap<String, String>();
        params.put("limit", String.valueOf(limit));

        if (startNode != null)
        {
            params.put("start", startNode);
        }

        List<Changeset> changesets = new ArrayList<Changeset>();

        try
        {
            RemoteResponse remoteResponse = remoteClient.getRequestor().get(
                    "/repositories/" + CustomStringUtils.encode(owner) + "/" + CustomStringUtils.encode(slug)
                            + "/changesets", params);

            JSONArray list = new JSONObject(IOUtils.toString(remoteResponse.getResponse())).getJSONArray("changesets");
            for (int i = 0; i < list.length(); i++)
            {
                JSONObject json = list.getJSONObject(i);

                final Changeset changeset = BitbucketChangesetFactory.parse(repository.getId(), json);
                if (lastCommitDate == null || lastCommitDate.before(changeset.getDate()))
                {
                    changesets.add(changeset);
                }
            }

        }

        catch (BitbucketRequestException.NotFound_404 e)
        {
            return Collections.emptyList();
        } catch (BitbucketRequestException.Unauthorized_401 e)
        {
            throw new SourceControlException("Incorrect credentials");
        } catch (BitbucketRequestException e)
        {
            log.warn("Could not get changesets from node: {}", startNode);
            throw new SourceControlException("Error requesting changesets. Node: " + startNode + ". [" + e.getMessage()
                    + "]", e);
        } catch (IOException ioe)
        {
            log.warn("Could not get changesets from node: {}", startNode);
            throw new SourceControlException("Error requesting changesets. Node: " + startNode + ". ["
                    + ioe.getMessage() + "]", ioe);
        } catch (JSONException e)
        {
            throw new SourceControlException("Could not parse json object", e);
        }
        return changesets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Changeset> getChangesets(final Repository repository, final Date lastCommitDate)
    {
        /*
         * try { BitbucketRemoteClient remoteClient =
         * bitbucketClientRemoteFactory.getForRepository(repository);
         * Iterable<BitbucketChangeset> changesets =
         * remoteClient.getChangesetsRest
         * ().getChangesets(repository.getOrgName(), //owner
         * repository.getSlug(), null);
         * 
         * return
         * ChangesetIterableTransformer.fromBitbucketChangesetIterable(repository
         * , changesets); } catch (BitbucketRequestException e) {
         * log.debug(e.getMessage(), e); throw new
         * SourceControlException("Could not get changesets", e); }
         */
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                return new BitbucketChangesetIterator(BitbucketCommunicator.this, repository, lastCommitDate);
            }
        };

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupPostcommitHook(Repository repository, String postCommitUrl)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForRepository(repository);
            remoteClient.getServicesRest().addPOSTService(repository.getOrgName(), // owner
                    repository.getSlug(), postCommitUrl);

            bitbucketLinker.linkRepository(repository);
        } catch (BitbucketRequestException e)
        {
            log.debug("Could not add postcommit hook", e);
            throw new SourceControlException("Could not add postcommit hook", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePostcommitHook(Repository repository, String postCommitUrl)
    {
        try
        {
            bitbucketLinker.unlinkRepository(repository);

            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForRepository(repository);
            List<BitbucketServiceEnvelope> services = remoteClient.getServicesRest().getAllServices(
                    repository.getOrgName(), // owner
                    repository.getSlug());

            for (BitbucketServiceEnvelope bitbucketServiceEnvelope : services)
            {
                for (BitbucketServiceField serviceField : bitbucketServiceEnvelope.getService().getFields())
                {
                    boolean fieldNameIsUrl = serviceField.getName().equals("URL");
                    boolean fieldValueIsRequiredPostCommitUrl = serviceField.getValue().equals(postCommitUrl);

                    if (fieldNameIsUrl && fieldValueIsRequiredPostCommitUrl)
                    {
                        remoteClient.getServicesRest().deleteService(repository.getOrgName(), // owner
                                repository.getSlug(), bitbucketServiceEnvelope.getId());
                    }
                }
            }
        } catch (BitbucketRequestException e)
        {
            log.debug("Could not remove postcommit hook", e);
            throw new SourceControlException("Could not remove postcommit hook", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}/{2}/changeset/{3}?dvcsconnector={4}", repository.getOrgHostUrl(),
                repository.getOrgName(), repository.getSlug(), changeset.getNode(), pluginVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index)
    {
        return MessageFormat.format("{0}#chg-{1}", getCommitUrl(repository, changeset), file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DvcsUser getUser(Repository repository, String username)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForRepository(repository);

            BitbucketAccount bitbucketAccount = remoteClient.getAccountRest().getUser(username);

            return DvcsUserTransformer.fromBitbucketAccount(bitbucketAccount);
        } catch (BitbucketRequestException e)
        {
            log.debug("Could not load user [" + username + "]", e);
            return DvcsUser.UNKNOWN_USER;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}", repository.getOrgHostUrl(), changeset.getAuthor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Group> getGroupsForOrganization(Organization organization)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForOrganization(organization);
            Set<BitbucketGroup> groups = remoteClient.getGroupsRest().getGroups(organization.getName()); // owner


            return GroupTransformer.fromBitbucketGroups(groups);
        } catch (BitbucketRequestException e)
        {
            log.debug("Could not get groups for organization [" + organization.getName() + "]");
            throw new SourceControlException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateCredentials(Organization organization)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsInvitation(Organization organization)
    {
        return organization.getDvcsType().equalsIgnoreCase(BITBUCKET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForOrganization(organization);

            for (String groupSlug : groupSlugs)
            {
                log.debug("Going invite " + userEmail + " to group " + groupSlug + " of bitbucket organization "
                        + organization.getName());

                remoteClient.getAccountRest().inviteUser(organization.getName(), userEmail, organization.getName(),
                        groupSlug);
            }
        } catch (BitbucketRequestException exception)
        {
            log.warn("Failed to invite user {} to organization {}. Response HTTP code {}", new Object[] { userEmail,
                    organization.getName(), exception.getClass().getName() });
        }

    }

    public static String getApiUrl(String hostUrl)
    {
        return hostUrl + "/!api/1.0";
    }

}
