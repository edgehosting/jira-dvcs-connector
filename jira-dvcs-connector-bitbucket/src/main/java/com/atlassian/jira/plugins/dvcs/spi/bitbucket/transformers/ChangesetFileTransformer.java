package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetFile;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * ChangesetFileTransformer
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetFileTransformer {
    private ChangesetFileTransformer() {}

    
    public static List<ChangesetFile> fromBitbucketChangesetWithDiffstat(List<BitbucketChangesetWithDiffstat> diffstats)
    {
        return Lists.transform(diffstats, new Function<BitbucketChangesetWithDiffstat, ChangesetFile>() {

            @Override
            public ChangesetFile apply(BitbucketChangesetWithDiffstat diffstat)
            {
                ChangesetFileAction fileAction = ChangesetFileAction.valueOf(diffstat.getType().toUpperCase());
                
                return new ChangesetFile(fileAction,
                                         diffstat.getFile(),
                                         diffstat.getDiffstat().getAdded(),
                                         diffstat.getDiffstat().getRemoved());
            }
        });
    }
    
    public static List<ChangesetFile> fromBitbucketChangesetFile(List<BitbucketChangesetFile> changesetFiles)
    {
        return Lists.transform(changesetFiles, new Function<BitbucketChangesetFile, ChangesetFile>() {

            @Override
            public ChangesetFile apply(BitbucketChangesetFile changesetFile)
            {
                ChangesetFileAction fileAction = ChangesetFileAction.valueOf(changesetFile.getType().toUpperCase());
                
                return new ChangesetFile(fileAction,
                                         changesetFile.getFile(),
                                         0,  // zero additions
                                         0); // zero deletions
            }
        });
    }
}
