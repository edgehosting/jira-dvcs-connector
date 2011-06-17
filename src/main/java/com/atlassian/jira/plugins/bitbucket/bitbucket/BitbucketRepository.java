package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.resource.RemoteResource;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.util.*;

/**
 * Describes a repository on bitbucket
 */
public class BitbucketRepository
{
    public static final int PAGE_SIZE = 15;

    public static BitbucketRepository parse(RemoteResource resource, JSONObject json)
    {
        try
        {
            return new BitbucketRepository(
                    resource,
                    json.getString("website"),
                    json.getString("name"),
                    json.getInt("followers_count"),
                    json.getString("owner"),
                    json.getString("logo"),
                    json.getString("resource_uri"),
                    json.getString("slug"),
                    json.getString("description")
            );
        }
        catch (JSONException e)
        {
            throw new BitbucketException("invalid json object");
        }
    }

    private final RemoteResource remoteResource;
    private final String website;
    private final String name;
    private final int followers;
    private final String owner;
    private final String logo;
    private final String resourceUri;
    private final String slug;
    private final String description;

    public BitbucketRepository(RemoteResource remoteResource,
                               String website, String name, int followers, String owner,
                               String logo, String resourceUri, String slug, String description)
    {
        this.remoteResource = remoteResource;
        this.website = website;
        this.name = name;
        this.followers = followers;
        this.owner = owner;
        this.logo = logo;
        this.resourceUri = resourceUri;
        this.slug = slug;
        this.description = description;
    }

    public String getWebsite()
    {
        return website;
    }

    public String getName()
    {
        return name;
    }

    public int getFollowers()
    {
        return followers;
    }

    public String getOwner()
    {
        return owner;
    }

    public String getLogo()
    {
        return logo;
    }

    public String getResourceUri()
    {
        return resourceUri;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getDescription()
    {
        return description;
    }

    /**
     * Makes a call to the changesets resource on the bitbucket remote api, retrieving a specific changeset
     * based on the specified changeset id.
     * <p/>
     * See:
     * <a href="http://confluence.atlassian.com/display/BBDEV/Changesets">http://confluence.atlassian.com/display/BBDEV/Changesets</a>
     *
     * @param id the commit id to retrieve
     * @return the changeset information
     */
    public BitbucketChangeset changeset(String id)
    {
        return BitbucketChangeset.parse(remoteResource.get("changesets/" + id));
    }

    /**
     * Makes a call to the changesets resource on the bitbucket remote api, retrieving all changesets from the
     * specified revision back to the first revision.
     * <p/>
     * See:
     * <a href="http://confluence.atlassian.com/display/BBDEV/Changesets">http://confluence.atlassian.com/display/BBDEV/Changesets</a>
     *
     * @param revision the revision to start from
     * @return a list of changeset information
     */
    public List<BitbucketChangeset> changesets(int revision)
    {
        List<BitbucketChangeset> changesets = new ArrayList<BitbucketChangeset>();

        int currentRevision = revision;
        do
        {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("start", currentRevision > 0 ? currentRevision : "tip");
            params.put("limit", currentRevision > 0 ? Math.min(PAGE_SIZE, currentRevision) : PAGE_SIZE);

            JSONObject page = remoteResource.get("changesets", params);
            try
            {
                currentRevision = currentRevision < 0 ? (page.getInt("count") - 1) : currentRevision;

                JSONArray list = page.getJSONArray("changesets");
                for (int i = 0; i < list.length(); i++)
                    changesets.add(BitbucketChangeset.parse(list.getJSONObject(i)));
            }
            catch (JSONException e)
            {
                throw new BitbucketException("invalid json returned from bitbucket", e);
            }

            currentRevision = currentRevision - PAGE_SIZE;
        } while (currentRevision > 0);

        Collections.sort(changesets, new Comparator<BitbucketChangeset>()
        {
            public int compare(BitbucketChangeset a, BitbucketChangeset b)
            {
                return a.getRevision() - b.getRevision();
            }
        });

        return changesets;

    }

    /**
     * Makes a call to the changesets resource on the bitbucket remote api, retrieving all changesets from the
     * tip back to the first revision.
     * <p/>
     * See:
     * <a href="http://confluence.atlassian.com/display/BBDEV/Changesets">http://confluence.atlassian.com/display/BBDEV/Changesets</a>
     *
     * @return a list of changeset information
     */
    public List<BitbucketChangeset> changesets()
    {
        return changesets(-1);
    }
}
