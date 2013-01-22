package com.atlassian.jira.plugins.dvcs.spi.github.model.review;

import java.util.Date;

/**
 * Base class for the all kinds of review items.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class ReviewItem
{

    /**
     * @see #getTimeStamp()
     */
    private Date timeStamp;

    /**
     * Constructor.
     */
    public ReviewItem()
    {
    }

    /**
     * @return Timestamp of the action.
     */
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * @param timeStamp
     *            {@link #getTimeStamp()}
     */
    public void setTimeStamp(Date timeStamp)
    {
        this.timeStamp = timeStamp;
    }

}
