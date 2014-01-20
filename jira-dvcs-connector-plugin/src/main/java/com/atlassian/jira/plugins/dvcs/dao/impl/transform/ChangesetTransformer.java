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
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ChangesetTransformer
{
    public static final Logger log = LoggerFactory.getLogger(ChangesetTransformer.class);
    private final ActiveObjects activeObjects;

    private final Map<Integer, RepositoryMapping> REPO_CACHE = Maps.newConcurrentMap();

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
        preloadRepositories(changesetMappings);
        //
        final List<Changeset> transformed = Lists.newArrayList();

        for (final ChangesetMapping changesetMapping : changesetMappings)
        {
            if (changesetMapping == null)
            {
                continue;
            }
            final Changeset transformedChangeset = processChangeset(changesetMapping, mainRepositoryId, dvcsType);
            if (transformedChangeset != null)
            {
                transformed.add(transformedChangeset);
            }

        }
        return transformed;
    }

    private void preloadRepositories(final Collection<ChangesetMapping> changesetMappings)
    {
        final Set<Integer> repoIdsSet = getRepositoriesIds(changesetMappings);
        REPO_CACHE.clear();
        if (!repoIdsSet.isEmpty())
        {
            if (repoIdsSet.size() == 1)
            {
                final int repoId = repoIdsSet.iterator().next();
                REPO_CACHE.put(repoId, activeObjects.get(RepositoryMapping.class, repoId));
            }
            else
            {
                final String where = ActiveObjectsUtils.renderListNumbersOperator("ID", "IN", "OR", repoIdsSet).toString();
                final Query query = Query.select().from(RepositoryMapping.class).where(where);
                final RepositoryMapping[] repos = activeObjects.find(RepositoryMapping.class, query);
                log.debug("Preloaded {} repositories repositories for {} changesets. ", repos.length, changesetMappings.size());
                for (final RepositoryMapping repo : repos)
                {
                    REPO_CACHE.put(repo.getID(), repo);
                }
            }
        }
    }

    private Set<Integer> getRepositoriesIds(final Collection<ChangesetMapping> changesetMappings)
    {
        final Set<Integer> ids = Sets.newHashSet();
        for (final ChangesetMapping changeset : changesetMappings)
        {
            ids.addAll(Collections2.transform(Sets.newHashSet(changeset.getRepositoryIds()),
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

    private Changeset processChangeset(final ChangesetMapping changesetMapping, final int mainRepositoryId, final String dvcsType)
    {
        if (changesetMapping == null)
        {
            return null;
        }

        final FileData fileData = parseFilesData(changesetMapping.getFilesData());
        final List<String> parents = parseParentsData(changesetMapping.getParentsData());

        final Changeset changeset = transformEntity(mainRepositoryId, changesetMapping, fileData, parents);

        List<Integer> repositories = changeset.getRepositoryIds();
        int firstRepository = 0;

        for (final RepositoryMapping repositoryMapping : getRepositoriesFromCache(changesetMapping))
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

    protected Collection<RepositoryMapping> getRepositoriesFromCache(final ChangesetMapping changesetMapping)
    {
        final RepositoryToChangesetMapping[] repositoryIds = changesetMapping.getRepositoryIds();
        final Collection<RepositoryMapping> repos = Lists.newArrayList();
        for (final RepositoryToChangesetMapping repositoryToChangesetMapping : repositoryIds)
        {
            repos.add(REPO_CACHE.get(repositoryToChangesetMapping.getRepositoryId()));
        }
        return repos;
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

    private FileData parseFilesData(final String filesData)
    {
        final List<ChangesetFile> files = new ArrayList<ChangesetFile>();
        int fileCount = 0;

        if (StringUtils.isNotBlank(filesData))
        {
            try
            {
                final JSONObject filesDataJson = new JSONObject(filesData);
                fileCount = filesDataJson.getInt("count");
                final JSONArray filesJson = filesDataJson.getJSONArray("files");

                for (int i = 0; i < filesJson.length(); i++)
                {
                    final JSONObject file = filesJson.getJSONObject(i);
                    final String filename = file.getString("filename");
                    final String status = file.getString("status");
                    final int additions = file.getInt("additions");
                    final int deletions = file.getInt("deletions");

                    files.add(new ChangesetFile(CustomStringUtils.getChangesetFileAction(status), filename, additions, deletions));
                }

            }
            catch (final JSONException e)
            {
                log.error("Failed parsing files from FileJson data.");
            }
        }

        return new FileData(files, fileCount);
    }

    private static class FileData
    {
        private final List<ChangesetFile> files;
        private final int fileCount;

        FileData(final List<ChangesetFile> files, final int fileCount)
        {
            this.files = files;
            this.fileCount = fileCount;
        }

        public List<ChangesetFile> getFiles()
        {
            return files;
        }

        public int getFileCount()
        {
            return fileCount;
        }

    }
}
