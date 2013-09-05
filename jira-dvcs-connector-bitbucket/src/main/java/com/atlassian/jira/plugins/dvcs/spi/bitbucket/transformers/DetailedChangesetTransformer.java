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
            // if total modified files count is less than limited size, we use at least the actual limited size
            // one case of this is when total files equal zero, because the value has not been gained from Bitbucket's commits endpoint
            if (changeset.getAllFileCount() < files.size())
            {
                changeset.setAllFileCount(files.size());
            }

            // if we downloaded more then MAX_VISIBLE_FILES, to find out, whether there are some, we cut the end of the list
            if (files.size() >= Changeset.MAX_VISIBLE_FILES)
            {
                files.subList(Changeset.MAX_VISIBLE_FILES, files.size()).clear();
            }

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
