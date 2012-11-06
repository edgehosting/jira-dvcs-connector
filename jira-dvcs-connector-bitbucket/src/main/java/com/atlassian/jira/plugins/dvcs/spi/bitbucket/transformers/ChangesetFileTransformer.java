package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetFile;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;

/**
 * ChangesetFileTransformer
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetFileTransformer {
    private ChangesetFileTransformer() {}


    @SuppressWarnings("unchecked")
    public static List<ChangesetFile> fromBitbucketChangesetsWithDiffstat(List<BitbucketChangesetWithDiffstat> diffstats)
    {
        return (List<ChangesetFile>) CollectionUtils.collect(diffstats, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                BitbucketChangesetWithDiffstat diffstat = (BitbucketChangesetWithDiffstat) input;

                ChangesetFileAction fileAction = ChangesetFileAction.valueOf(diffstat.getType().toUpperCase());

                return new ChangesetFile(fileAction,
                                         diffstat.getFile(),
                                         diffstat.getDiffstat().getAdded(),
                                         diffstat.getDiffstat().getRemoved());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static List<ChangesetFile> fromBitbucketChangesetFiles(List<BitbucketChangesetFile> changesetFiles)
    {
        return (List<ChangesetFile>) CollectionUtils.collect(changesetFiles, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                BitbucketChangesetFile changesetFile = (BitbucketChangesetFile) input;

                ChangesetFileAction fileAction = ChangesetFileAction.valueOf(changesetFile.getType().toUpperCase());

                return new ChangesetFile(fileAction,
                                         changesetFile.getFile(),
                                         0,  // zero additions
                                         0); // zero deletions
            }
        });
    }
}
