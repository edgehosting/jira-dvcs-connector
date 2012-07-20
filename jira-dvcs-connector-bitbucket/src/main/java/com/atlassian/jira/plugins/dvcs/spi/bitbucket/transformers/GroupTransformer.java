package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * GroupTransformer
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class GroupTransformer {
    private GroupTransformer() {}

    
    public static List<Group> fromBitbucketGroups(List<BitbucketGroup> bitbucketGroups)
    {
        return Lists.transform(bitbucketGroups, new Function<BitbucketGroup, Group>() {

            @Override
            public Group apply(BitbucketGroup bitbucketGroup)
            {
                return new Group(bitbucketGroup.getSlug(), bitbucketGroup.getName());
            }
        });
    }
}
