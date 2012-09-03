package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;

/**
 * GroupTransformer
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class GroupTransformer
{

    @SuppressWarnings("unchecked")
    public static Set<Group> fromBitbucketGroups(Set<BitbucketGroup> bitbucketGroups)
    {
        Collection<Group> transformedGroups = CollectionUtils.collect(bitbucketGroups, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                BitbucketGroup bitbucketGroup = (BitbucketGroup) input;
                
                return new Group(StringUtils.trim(bitbucketGroup.getSlug()), bitbucketGroup.getName());
            }
        });

        return new HashSet<Group>(transformedGroups);
    }
}
