package com.atlassian.jira.plugins.bitbucket.mapper;


import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.util.concurrent.BlockingReference;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class Progress
{
    public interface State
    {
        public String render();
    }

    class Starting implements State
    {
        public String render()
        {
            return renderTemplate("progress-starting.vm", createMap());
        }
    }

    class InProgress implements State
    {
        final int revision;
        final int jiraCount;

        public InProgress(int revision, int jiraCount)
        {
            this.revision = revision;
            this.jiraCount = jiraCount;
        }

        public int getRevision()
        {
            return revision;
        }

        public int getJiraCount()
        {
            return jiraCount;
        }

        public String render()
        {
            Map<String, Object> map = createMap();
            map.put("revision", revision);
            map.put("jiraCount", jiraCount);
            return renderTemplate("progress-inprogress.vm", map);
        }

    }

    private final TemplateRenderer templateRenderer;
    private final SynchronizationKey key;
    private final Future<OperationResult> future;
    private final BlockingReference<State> progress = BlockingReference.newMRSW();

    public Progress(TemplateRenderer templateRenderer, SynchronizationKey key, Future<OperationResult> future)
    {
        this.templateRenderer = templateRenderer;
        this.key = key;
        this.future = future;
        this.progress.set(new Starting());
    }

    public void inProgress(int revision, int jiraCount)
    {
        this.progress.set(new InProgress(revision, jiraCount));
    }

    public State getProgress()
    {
        return progress.peek();
    }

    public SynchronizationKey getKey()
    {
        return key;
    }

    public boolean matches(String projectKey, RepositoryUri repositoryUri)
    {
        return key.matches(projectKey, repositoryUri);
    }

    private Map<String, Object> createMap()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", key);
        map.put("future", future);
        return map;
    }

    private String renderTemplate(String templateName, Map<String, Object> context)
    {
        try
        {
            StringWriter buf = new StringWriter();
            templateRenderer.render("templates/com/atlassian/jira/plugins/bitbucket/"+templateName, context, buf);
            return buf.toString();
        }
        catch (IOException e)
        {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

}