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
        List<ChangesetFile> files = ChangesetFileTransformer.fromBitbucketChangesetsWithDiffstat(diffstats);
        
        Changeset changeset = copyChangeset(inputChangeset);
        changeset.setIssueKey(null); //TODO missing various other transformations for fields, did we forget?
        changeset.setFiles(files);
        
        return changeset;
    }
    
    private static Changeset copyChangeset(Changeset changesetToCopy)
    {
        Changeset changeset = new Changeset(changesetToCopy.getRepositoryId(),
                                            changesetToCopy.getNode(),
                                            changesetToCopy.getIssueKey(),
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
