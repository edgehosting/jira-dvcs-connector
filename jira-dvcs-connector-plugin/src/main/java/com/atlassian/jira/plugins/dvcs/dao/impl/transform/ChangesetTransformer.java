package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.FileData;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Component
public class ChangesetTransformer
{
    public static final Logger log = LoggerFactory.getLogger(ChangesetTransformer.class);
    private final ActiveObjects activeObjects;
    private final ChangesetDao changesetDao;

    @Autowired
    public ChangesetTransformer(@ComponentImport final ActiveObjects activeObjects, final ChangesetDao changesetDao)
    {
        this.activeObjects = activeObjects;
        this.changesetDao = changesetDao;
    }

    public Changeset transform(ChangesetMapping changesetMapping, int mainRepositoryId, String dvcsType)
    {

        if (changesetMapping == null)
        {
            return null;
        }

//        log.debug("Changeset transformation: [{}] ", changesetMapping);

        final Changeset changeset = transform(mainRepositoryId, changesetMapping, dvcsType);

        List<Integer> repositories = changeset.getRepositoryIds();
        int firstRepository = 0;

        for (RepositoryMapping repositoryMapping : changesetMapping.getRepositories())
        {
            if (repositoryMapping.isDeleted() || !repositoryMapping.isLinked())
            {
                continue;
            }

            if (!StringUtils.isEmpty(dvcsType))
            {
                OrganizationMapping organizationMapping = activeObjects.get(OrganizationMapping.class, repositoryMapping.getOrganizationId());

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

            // we found repository that is not fork and no main repository is set on changeset,let's use it
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

    public Changeset transform(int repositoryId, ChangesetMapping changesetMapping, String dvcsType)
    {
        if (changesetMapping == null)
        {
            return null;
        }

        // prefer the file details info
        List<ChangesetFileDetail> fileDetails = ChangesetFileDetails.fromJSON(changesetMapping.getFileDetailsJson());

        final Changeset changeset = new Changeset(repositoryId,
                changesetMapping.getNode(),
                changesetMapping.getRawAuthor(),
                changesetMapping.getAuthor(),
                changesetMapping.getDate(),
                changesetMapping.getRawNode(),
                changesetMapping.getBranch(),
                changesetMapping.getMessage(),
                parseParentsData(changesetMapping.getParentsData()),
                fileDetails != null ? ImmutableList.<ChangesetFile>copyOf(fileDetails) : null,
                changesetMapping.getFileCount(),
                changesetMapping.getAuthorEmail());

        changeset.setId(changesetMapping.getID());
        changeset.setVersion(changesetMapping.getVersion());
        changeset.setSmartcommitAvaliable(changesetMapping.isSmartcommitAvailable());

        changeset.setFileDetails(fileDetails);

        if (changesetMapping.getFilesData() != null)
        {
            // file data still there, we need to migrate

            if (changesetMapping.getFileCount() == 0)
            {
                // we can use the file count in file data directly
                // https://jdog.jira-dev.com/browse/BBC-709 migrating file count from file data to separate column
                final FileData fileData = FileData.from(changesetMapping);
                log.debug("Migrating file count from old file data structure for changeset {}.", changeset.getNode());
                changeset.setAllFileCount(fileData.getFileCount());

                if (BitbucketCommunicator.BITBUCKET.equals(dvcsType) && fileData.getFileCount() == Changeset.MAX_VISIBLE_FILES + 1)
                {
                    // file count in file data is 6 for Bitbucket, we need to refetch the diffstat to find out the correct number
                    // https://jdog.jira-dev.com/browse/BBC-719 forcing file details to reload if changed files number is incorrect
                    log.debug("Forcing to refresh file details for changeset {}.", changeset.getNode());
                    changeset.setFileDetails(null);
                }
                else if (changeset.getFileDetails() == null && fileData.hasDetails())
                {
                    log.debug("Migrating file details from old file data structure for changeset {}.", changeset.getNode());
                    changeset.setFileDetails(transfromFileData(fileData));
                }

            }

            changesetDao.update(changeset);
        }

        return changeset;
    }

    private List<ChangesetFileDetail> transfromFileData(final FileData fileData)
    {
        List<ChangesetFileDetail> changesetFileDetails = new LinkedList<ChangesetFileDetail>();
        for (ChangesetFile file : fileData.getFiles())
        {
            int additions = 0;
            int deletions = 0;
            if (file instanceof ChangesetFileDetail)
            {
                additions = ((ChangesetFileDetail) file).getAdditions();
                deletions = ((ChangesetFileDetail) file).getDeletions();
            }
            changesetFileDetails.add(new ChangesetFileDetail(file.getFileAction(), file.getFile(), additions, deletions));
        }
        return changesetFileDetails;
    }

    private List<String> parseParentsData(String parentsData)
    {
        if (ChangesetMapping.TOO_MANY_PARENTS.equals(parentsData))
        {
            return null;
        }

        List<String> parents = new ArrayList<String>();

        if (StringUtils.isBlank(parentsData))
        {
            return parents;
        }

        try
        {
            JSONArray parentsJson = new JSONArray(parentsData);
            for (int i = 0; i < parentsJson.length(); i++)
            {
                parents.add(parentsJson.getString(i));
            }
        }
        catch (JSONException e)
        {
            log.error("Failed parsing parents from ParentsJson data.");
        }

        return parents;
    }
}
