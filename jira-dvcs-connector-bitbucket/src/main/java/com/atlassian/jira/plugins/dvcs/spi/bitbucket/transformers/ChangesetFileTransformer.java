package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
    public static List<ChangesetFileDetail> fromBitbucketChangesetsWithDiffstat(List<BitbucketChangesetWithDiffstat> diffstats)
    {
        return ImmutableList.copyOf(Lists.transform(diffstats, new Function<BitbucketChangesetWithDiffstat, ChangesetFileDetail>()
        {
            @Override
            public ChangesetFileDetail apply(BitbucketChangesetWithDiffstat diffstat)
            {
                ChangesetFileAction fileAction = ChangesetFileAction.valueOf(diffstat.getType().toUpperCase());

                int added = 0;
                int removed = 0;
                if (diffstat.getDiffstat() != null)
                {
                    if (diffstat.getDiffstat().getAdded() != null)
                    {
                        added = diffstat.getDiffstat().getAdded();
                    }
                    if (diffstat.getDiffstat().getRemoved() != null)
                    {
                        removed = diffstat.getDiffstat().getRemoved();
                    }
                }
                return new ChangesetFileDetail(fileAction, diffstat.getFile(), added, removed);
            }
        }));
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

                return new ChangesetFile(fileAction, changesetFile.getFile());
            }
        });
    }
}
