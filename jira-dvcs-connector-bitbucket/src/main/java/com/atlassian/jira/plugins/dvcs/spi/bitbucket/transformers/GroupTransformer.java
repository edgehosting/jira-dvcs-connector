package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

/**
 * GroupTransformer
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class GroupTransformer
{

    public static Set<Group> fromBitbucketGroups(Set<BitbucketGroup> bitbucketGroups)
    {
        Collection<Group> collection = Collections2.transform(bitbucketGroups,
                new Function<BitbucketGroup, Group>()
                {
                    @Override
                    public Group apply(BitbucketGroup bitbucketGroup)
                    {
                        return new Group(StringUtils.trim(bitbucketGroup.getSlug()));
                    }
                });
        return new HashSet<Group>(collection);

    }
}
