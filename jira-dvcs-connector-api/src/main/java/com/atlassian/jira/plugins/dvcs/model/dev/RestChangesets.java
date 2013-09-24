package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestChangesets
{
    private int count;
    private List<RestObject> objects;
    private RestType type;

    public RestChangesets()
    {
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(final int count)
    {
        this.count = count;
    }

    public List<RestObject> getObjects()
    {
        return objects;
    }

    public void setObjects(final List<RestObject> objects)
    {
        this.objects = objects;
    }

    public RestType getType()
    {
        return type;
    }

    public void setType(final RestType type)
    {
        this.type = type;
    }
}
