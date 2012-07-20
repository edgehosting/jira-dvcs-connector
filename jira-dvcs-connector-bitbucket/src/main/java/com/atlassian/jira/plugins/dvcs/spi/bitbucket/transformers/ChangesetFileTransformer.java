package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
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

    
    public static List<ChangesetFile> fromBitbucketCahngeetWithDiffstat(List<BitbucketChangesetWithDiffstat> diffstats)
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
}
