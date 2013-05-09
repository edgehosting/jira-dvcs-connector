package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;

import java.util.List;

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
