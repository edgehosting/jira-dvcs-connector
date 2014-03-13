package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.FileData;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

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
        final ListMultimap<Integer, RepositoryToChangesetMapping> changesetToRepo = LinkedListMultimap.create();

        public RepoCache populate(final Collection<ChangesetMapping> changesetMappings)
        {
            populateRepositoryIds(changesetMappings);
            return this;
        }

        private void populateRepositoryIds(final Collection<ChangesetMapping> changesetMappings)
        {
            final Iterable<Integer> changesetMappingsIds = Iterables.transform(changesetMappings, new Function<ChangesetMapping, Integer>()
            {
                @Override
                public Integer apply(@Nullable final ChangesetMapping input)
                {
                    return input.getID();
                }
            });

            final List<RepositoryToChangesetMapping> repositoryToChangesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<RepositoryToChangesetMapping>>()
            {
                @Override
                public List<RepositoryToChangesetMapping> doInTransaction()
                {
                    final RepositoryToChangesetMapping[] mappings = activeObjects
                            .find(RepositoryToChangesetMapping.class,
                                    Query.select("ID, *")
                                            .alias(ChangesetMapping.class, "CHANGESET")
                                            .alias(RepositoryToChangesetMapping.class, "RCH")
                                            .alias(RepositoryMapping.class, "REPOSITORY")
                                            .join(ChangesetMapping.class,
                                                    "CHANGESET.ID = RCH." + RepositoryToChangesetMapping.CHANGESET_ID)
                                            .join(RepositoryMapping.class,
                                                    "REPOSITORY.ID = RCH." + RepositoryToChangesetMapping.REPOSITORY_ID)
                                            .where("REPOSITORY." + RepositoryMapping.DELETED + " = ? AND REPOSITORY." + RepositoryMapping.LINKED + " = ? AND "
                                                    + ActiveObjectsUtils.renderListNumbersOperator("CHANGESET.ID", "IN", "OR", changesetMappingsIds).toString(),
                                                    Boolean.FALSE, Boolean.TRUE));

                    return Arrays.asList(mappings);
                }
            });

            changesetToRepo.putAll(Multimaps.index(repositoryToChangesetMappings, new Function<RepositoryToChangesetMapping, Integer>()
            {
                @Override
                public Integer apply(@Nullable final RepositoryToChangesetMapping input)
                {
                    return input.getChangeset().getID();
                }
            }));
        }

        public List<Integer> getRepositoryIds(final ChangesetMapping changesetMapping)
        {
            final List<Integer> repositoryIds = new ArrayList<Integer>();
            for (RepositoryToChangesetMapping repositoryToChangesetMapping : changesetToRepo.get(changesetMapping.getID()))
            {
                repositoryIds.add(repositoryToChangesetMapping.getRepositoryId());
            }
            return repositoryIds;
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

        List<Integer> repositoryIds = repoCache.getRepositoryIds(changesetMapping);

        if (changeset.getRepositoryIds() == null)
        {
            changeset.setRepositoryIds(repositoryIds);
        }
        if (changeset.getRepositoryId() == 0)
        {
            changeset.setRepositoryId(repositoryIds.get(0));
        }
        return changeset;
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
