package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;

/**
 * DetailedChangesetTransformer
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class DetailedChangesetTransformer {
    private DetailedChangesetTransformer() {}


    public static Changeset fromChangesetAndBitbucketDiffstats(Changeset inputChangeset,
            List<BitbucketChangesetWithDiffstat> diffstats)
    {
        Changeset changeset = copyChangeset(inputChangeset);
        if (diffstats!=null)
        {
            List<ChangesetFile> files = ChangesetFileTransformer.fromBitbucketChangesetsWithDiffstat(diffstats);
            if (changeset.getAllFileCount() < files.size())
            {
                changeset.setAllFileCount(files.size());
            }

            if (files.size() >= Changeset.MAX_VISIBLE_FILES)
            {
                files.subList(Changeset.MAX_VISIBLE_FILES, files.size()).clear();
            }

            //TODO commits-view.vm and activityentry-summary.vm to be fixed not displaying exact number of rest files
            changeset.setFiles(files);

        }

        return changeset;
    }

    private static Changeset copyChangeset(Changeset changesetToCopy)
    {
        Changeset changeset = new Changeset(changesetToCopy.getRepositoryId(),
                                            changesetToCopy.getNode(),
                                            changesetToCopy.getRawAuthor(),
                                            changesetToCopy.getAuthor(),
                                            changesetToCopy.getDate(),
                                            changesetToCopy.getRawNode(),
                                            changesetToCopy.getBranch(),
                                            changesetToCopy.getMessage(),
                                            changesetToCopy.getParents(),
                                            changesetToCopy.getFiles(),
                                            changesetToCopy.getAllFileCount(),
                                            changesetToCopy.getAuthorEmail());

        return changeset;
    }
}
