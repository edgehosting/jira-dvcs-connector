package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

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
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceField;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.DetailedChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.DvcsUserTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.GroupTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.RepositoryTransformer;
import com.atlassian.plugin.PluginAccessor;

/**
 * The Class BitbucketCommunicator.
 *
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
     * @param bitbucketLinker
     * @param pluginAccessor
     * @param oauth
     * @param bitbucketClientRemoteFactory
     */
    public BitbucketCommunicator(@Qualifier("defferedBitbucketLinker") BitbucketLinker bitbucketLinker,
            PluginAccessor pluginAccessor, BitbucketOAuth oauth,
            BitbucketClientRemoteFactory bitbucketClientRemoteFactory)
    {
        this.bitbucketLinker = bitbucketLinker;
        this.oauth = oauth;
        this.bitbucketClientRemoteFactory = bitbucketClientRemoteFactory;
        this.pluginVersion = getPluginVersion(pluginAccessor);
    }

    private static String getPluginVersion(PluginAccessor pluginAccessor)
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
        } catch (BitbucketRequestException.Unauthorized_401 e)
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
    private List<Changeset> getChangesetsInternal(final Repository repository, final String startNode, int limit,
            final Date lastCommitDate)
        final List<Changeset> changesets = new ArrayList<Changeset>();
            return remoteClient.getRequestor().get(
                            + "/changesets", params, new ResponseCallback<List<Changeset>>()
                    {
                        @Override
                        public List<Changeset> onResponse(RemoteResponse response)
                        {
                            try
                            {
                                JSONArray list = new JSONObject(IOUtils.toString(response.getResponse()))
                                        .getJSONArray("changesets");
                                for (int i = 0; i < list.length(); i++)
                                {
                                    JSONObject json = list.getJSONObject(i);
                                    final Changeset changeset = BitbucketChangesetFactory.parse(repository.getId(),
                                            json);
                                    if (lastCommitDate == null || lastCommitDate.before(changeset.getDate()))
                                    {
                                        changesets.add(changeset);
                                    }
                                }
                                return changesets;
                            } catch (IOException ioe)
                            {
                                log.warn("Could not get changesets from node: {}", startNodeOrTip(startNode));
                                throw new SourceControlException("Error requesting changesets. Node: "
                                        + startNodeOrTip(startNode) + ". [" + ioe.getMessage() + "]", ioe);
                            } catch (JSONException e)
                            {
                                throw new SourceControlException("Could not parse json object", e);
                            }
                        }
                    });

        } catch (BitbucketRequestException.NotFound_404 e)
        {
                    + e.getMessage() + "]", e);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Changeset> getChangesets(Repository repository, Date lastCommitDate)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientRemoteFactory.getForRepository(repository);
            Iterable<BitbucketChangeset> bitbucketChangesets =
                    remoteClient.getChangesetsRest().getChangesets(repository.getOrgName(),
                                                                   repository.getSlug(),
                                                                   repository.getLastChangesetNode());

            return new ChangesetIterableAdapter(repository, bitbucketChangesets);
        }
        catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get result", e);
        }
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
    public void linkRepository(Repository repository, Set<String> withProjectkeys)
    {
        try
        {
            bitbucketLinker.linkRepository(repository, withProjectkeys);
        } catch (Exception e)
        {
            log.warn("Failed to link repository " + repository.getName() + " : " + e.getClass() + " :: "
                    + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkRepositoryIncremental(Repository repository, Set<String> withPossibleNewProjectkeys)
    {
        try
        {
            bitbucketLinker.linkRepositoryIncremental(repository, withPossibleNewProjectkeys);
        } catch (Exception e)
        {
            log.warn("Failed to do incremental repository linking " + repository.getName() + " : " + e.getClass()
                    + " :: " + e.getMessage());
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
    private static final class ChangesetIterableAdapter implements Iterable<Changeset>, Iterator<Changeset>
    {
        private final Iterator<BitbucketChangeset> bitbucketChangesetIterator;
        private final int repositoryId;


        private ChangesetIterableAdapter(Repository repository, Iterable<BitbucketChangeset> bitbucketChangesetIterable)
        {
            this.bitbucketChangesetIterator = bitbucketChangesetIterable.iterator();
            this.repositoryId = repository.getId();
        }


        @Override
        public boolean hasNext() {
            return bitbucketChangesetIterator.hasNext();
        }

        @Override
        public Changeset next() {
            return ChangesetTransformer.fromBitbucketChangeset(repositoryId,
                                                               bitbucketChangesetIterator.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove operation not supported.");
        }

        @Override
        public Iterator<Changeset> iterator() {
            return this;
        }
    }
}
