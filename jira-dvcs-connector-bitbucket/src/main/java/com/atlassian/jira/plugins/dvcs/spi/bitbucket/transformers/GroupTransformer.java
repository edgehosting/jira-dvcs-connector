package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * GroupTransformer
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class GroupTransformer
{

    @SuppressWarnings("unchecked")
    public static List<Group> fromBitbucketGroups(List<BitbucketGroup> bitbucketGroups)
    {
        Collection<Group> transformedGroups = CollectionUtils.collect(bitbucketGroups, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                BitbucketGroup bitbucketGroup = (BitbucketGroup) input;
                
                return new Group(StringUtils.trim(bitbucketGroup.getSlug()), bitbucketGroup.getName());
            }
        });

        return new LinkedList<Group>(transformedGroups);
    }
}
