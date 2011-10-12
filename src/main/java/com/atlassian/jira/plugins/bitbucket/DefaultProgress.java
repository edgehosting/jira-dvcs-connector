package com.atlassian.jira.plugins.bitbucket;


import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.atlassian.jira.plugins.bitbucket.api.OperationResult;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.util.concurrent.BlockingReference;

public class DefaultProgress implements Progress
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
        final String currentNode;
        final int jiraCount;

        public InProgress(String currentNode, int jiraCount)
        {
            this.currentNode = currentNode;
            this.jiraCount = jiraCount;
        }

        public String getCurrentNode()
        {
            return currentNode;
        }

        public int getJiraCount()
        {
            return jiraCount;
        }

        public String render()
        {
            Map<String, Object> map = createMap();
            map.put("revision", currentNode);
            map.put("jiraCount", jiraCount);
            return renderTemplate("progress-inprogress.vm", map);
        }

    }

    private final TemplateRenderer templateRenderer;
    private final SynchronizationKey key;
    private final Future<OperationResult> future;
    private final BlockingReference<State> progress = BlockingReference.newMRSW();

    public DefaultProgress(TemplateRenderer templateRenderer, SynchronizationKey key, Future<OperationResult> future)
    {
        this.templateRenderer = templateRenderer;
        this.key = key;
        this.future = future;
        this.progress.set(new Starting());
    }

    /* (non-Javadoc)
	 * @see com.atlassian.jira.plugins.bitbucket.Progress#inProgress(java.lang.String, int)
	 */
    public void inProgress(String revision, int jiraCount)
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

    public boolean matches(String projectKey, String repositoryUrl)
    {
        return key.matches(projectKey, repositoryUrl);
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