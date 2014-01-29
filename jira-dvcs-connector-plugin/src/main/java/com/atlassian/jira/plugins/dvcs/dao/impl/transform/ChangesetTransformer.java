package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Query;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.FileData;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.google.common.collect.ImmutableList;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ChangesetTransformer
{
    public static final Logger log = LoggerFactory.getLogger(ChangesetTransformer.class);
    private final ActiveObjects activeObjects;

    public ChangesetTransformer(final ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    public Changeset transform(final ChangesetMapping changesetMapping, final int mainRepositoryId, final String dvcsType)
    {
        if (changesetMapping == null)
        {
            return null;
        }
        final Collection<Changeset> changeset = transformAll(Lists.newArrayList(changesetMapping), mainRepositoryId, dvcsType);
        if (changeset.isEmpty())
        {
            return null;
        }
        return changeset.iterator().next();
    }

    public List<Changeset> transformAll(final Collection<ChangesetMapping> changesetMappings, final int mainRepositoryId,
            final String dvcsType)
    {
        //
        RepoCache repoCache = new RepoCache().populate(changesetMappings);
        //
        final List<Changeset> transformed = Lists.newArrayList();

        for (final ChangesetMapping changesetMapping : changesetMappings)
        {
            if (changesetMapping == null)
            {
                continue;
            }
            final Changeset transformedChangeset = processChangeset(repoCache, changesetMapping, mainRepositoryId, dvcsType);
            if (transformedChangeset != null)
            {
                transformed.add(transformedChangeset);
            }

        }
        return transformed;
    }

    class RepoCache 
    {
       
        final Map<Integer, RepositoryMapping> repos = Maps.newHashMap();

        public RepoCache populate(final Collection<ChangesetMapping> changesetMappings)
        {
            final Set<Integer> repoIdsSet = getRepositoriesIds(changesetMappings);
            if (!repoIdsSet.isEmpty())
            {
                if (repoIdsSet.size() == 1)
                {
                    final int repoId = repoIdsSet.iterator().next();
                    repos.put(repoId, activeObjects.get(RepositoryMapping.class, repoId));
                }
                else
                {
                    final String where = ActiveObjectsUtils.renderListNumbersOperator("ID", "IN", "OR", repoIdsSet).toString();
                    final Query query = Query.select().from(RepositoryMapping.class).where(where);
                    final RepositoryMapping[] reposFromDb = activeObjects.find(RepositoryMapping.class, query);
                    log.debug("Preloaded {} repositories repositories for {} changesets. ", reposFromDb.length, changesetMappings.size());
                    for (final RepositoryMapping repo : reposFromDb)
                    {
                        repos.put(repo.getID(), repo);
                    }
                }
            }
            return this;
        }
        
        private Set<Integer> getRepositoriesIds(final Collection<ChangesetMapping> changesetMappings)
        {
            final Set<Integer> ids = Sets.newHashSet();
            for (final ChangesetMapping changeset : changesetMappings)
            {
                ids.addAll(Collections2.transform(Sets.newHashSet(changeset.getRepositoryIdMappings()),
                        new Function<RepositoryToChangesetMapping, Integer>()
                        {
                            public Integer apply(final RepositoryToChangesetMapping mapping)
                            {
                                return mapping.getRepositoryId();
                            }
                        }));
            }
            return ids;
        }

        public Collection<RepositoryMapping> get(final ChangesetMapping changesetMapping)
        {
            final RepositoryToChangesetMapping[] repositoryIds = changesetMapping.getRepositoryIdMappings();
            final Collection<RepositoryMapping> reposCached = Lists.newArrayList();
            for (final RepositoryToChangesetMapping repositoryToChangesetMapping : repositoryIds)
            {
                reposCached.add(repos.get(repositoryToChangesetMapping.getRepositoryId()));
            }
            return reposCached;
        }
    }

    private Changeset processChangeset(RepoCache repoCache, final ChangesetMapping changesetMapping, final int mainRepositoryId, final String dvcsType)
    {
        if (changesetMapping == null)
        {
            return null;
        }

        final FileData fileData = FileData.from(changesetMapping);
        final List<String> parents = parseParentsData(changesetMapping.getParentsData());

        final Changeset changeset = transformEntity(mainRepositoryId, changesetMapping, fileData, parents);

        List<Integer> repositories = changeset.getRepositoryIds();
        int firstRepository = 0;

        for (final RepositoryMapping repositoryMapping : repoCache.get(changesetMapping))
        {
            if (repositoryMapping.isDeleted() || !repositoryMapping.isLinked())
            {
                continue;
            }

            if (!StringUtils.isEmpty(dvcsType))
            {
                final OrganizationMapping organizationMapping = activeObjects.get(OrganizationMapping.class,
                        repositoryMapping.getOrganizationId());

                if (!dvcsType.equals(organizationMapping.getDvcsType()))
                {
                    continue;
                }
            }

            if (repositories == null)
            {
                repositories = new ArrayList<Integer>();
                changeset.setRepositoryIds(repositories);

                // mark first repository
                firstRepository = repositoryMapping.getID();
            }

            // we found repository that is not fork and no main repository is
            // set on changeset,let's use it
            if (changeset.getRepositoryId() == 0 && !repositoryMapping.isFork())
            {
                changeset.setRepositoryId(repositoryMapping.getID());
            }

            repositories.add(repositoryMapping.getID());
        }

        // no main repository was assigned, let's use the first one
        if (changeset.getRepositoryId() == 0)
        {
            changeset.setRepositoryId(firstRepository);
        }
        return CollectionUtils.isEmpty(changeset.getRepositoryIds()) ? null : changeset;
    }

    private Changeset transformEntity(final int repositoryId, final ChangesetMapping changesetMapping, final FileData fileData,
            final List<String> parents)
    {
        final Changeset changeset = new Changeset(repositoryId, changesetMapping.getNode(), changesetMapping.getRawAuthor(),
                changesetMapping.getAuthor(), changesetMapping.getDate(), changesetMapping.getRawNode(), changesetMapping.getBranch(),
                changesetMapping.getMessage(), parents, fileData.getFiles(), fileData.getFileCount(), changesetMapping.getAuthorEmail());

        changeset.setId(changesetMapping.getID());
        changeset.setVersion(changesetMapping.getVersion());
        changeset.setSmartcommitAvaliable(changesetMapping.isSmartcommitAvailable());

        // prefer the file details info
        List<ChangesetFileDetail> fileDetails = ChangesetFileDetails.fromJSON(changesetMapping.getFileDetailsJson());
        changeset.setFiles(fileDetails != null ? ImmutableList.<ChangesetFile>copyOf(fileDetails) : changeset.getFiles());
        changeset.setFileDetails(fileDetails);

        return changeset;
    }

    private List<String> parseParentsData(final String parentsData)
    {
        if (ChangesetMapping.TOO_MANY_PARENTS.equals(parentsData))
        {
            return null;
        }

        final List<String> parents = new ArrayList<String>();

        if (StringUtils.isBlank(parentsData))
        {
            return parents;
        }

        try
        {
            final JSONArray parentsJson = new JSONArray(parentsData);
            for (int i = 0; i < parentsJson.length(); i++)
            {
                parents.add(parentsJson.getString(i));
            }
        }
        catch (final JSONException e)
        {
            log.error("Failed parsing parents from ParentsJson data.");
        }

        return parents;
    }
}
