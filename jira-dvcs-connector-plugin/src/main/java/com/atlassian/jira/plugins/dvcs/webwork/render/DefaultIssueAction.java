package com.atlassian.jira.plugins.dvcs.webwork.render;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;

public class DefaultIssueAction implements IssueAction
{
    private final Logger log = LoggerFactory.getLogger(DefaultIssueAction.class);

    private final Map<String, Object> context;
    private final String template;
    private final TemplateRenderer templateRenderer;
    private final Date timePerformed;
    private final int id;

    public DefaultIssueAction(TemplateRenderer templateRenderer, String template, Map<String, Object> context,
            Date timePerformed)
    {
        this(templateRenderer, template, context, timePerformed, 0);
    }

    public DefaultIssueAction(TemplateRenderer templateRenderer, String template, Map<String, Object> context,
            Date timePerformed, int id)
    {
        this.templateRenderer = templateRenderer;
        this.template = template;
        this.context = context;
        this.timePerformed = timePerformed;
        this.id = id;
    }

    @Override
    public String getHtml()
    {
        StringWriter stringWriter = new StringWriter();
        try
        {
            templateRenderer.render(template, context, stringWriter);
        }
        catch (IOException e)
        {
            log.warn(e.getMessage(), e);
        }
        return stringWriter.toString();
    }

    @Override
    public Date getTimePerformed()
    {
        return timePerformed;
    }

    @Override
    public boolean isDisplayActionAllTab()
    {
        return true;
    }

    public int getId()
    {
        return id;
    }
}
