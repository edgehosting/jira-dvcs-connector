package com.atlassian.jira.plugins.dvcs.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class SystemUtils
{
    private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);

    private static final boolean SAFE_REDIRECT_EXISTS;
    private static final boolean URL_VALIDATOR_EXISTS;
    private static final boolean GET_ALL_ISSUE_KEYS_EXISTS;
    private static final boolean GET_ALL_PROJECT_KEYS_EXISTS;

    static
    {
        SAFE_REDIRECT_EXISTS = getRedirectExists();
        URL_VALIDATOR_EXISTS = getUrlValidatorExists();
        GET_ALL_ISSUE_KEYS_EXISTS = getAllIssueKeysExists();
        GET_ALL_PROJECT_KEYS_EXISTS = getAllProjectKeysExists();
    }

    private static boolean getRedirectExists()
    {
        // Detect whether getRedirect(String, boolean) is present
        return getMethodExists(JiraWebActionSupport.class, "getRedirect", String.class, boolean.class);
    }

    private static boolean getUrlValidatorExists()
    {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("com.atlassian.jira.util.UrlValidator");
        } catch (ClassNotFoundException e)
        {
        }
        if ( clazz != null )
        {
            return getMethodExists(clazz, "isValid", String.class);
        }

        return false;
    }

    private static boolean getAllIssueKeysExists()
    {
        // Detect whether getAllIssueKeys(Long) is present
        return getMethodExists(IssueManager.class, "getAllIssueKeys", Long.class);
    }

    private static boolean getAllProjectKeysExists()
    {
        // Detect whether getAllProjectKeys(Long) is present
        return getMethodExists(ProjectManager.class, "getAllProjectKeys", Long.class);
    }

    public static boolean getMethodExists(Class<?> clazz, String method, Class<?>... parameterTypes)
    {
        try
        {
            clazz.getMethod(method, parameterTypes);
        } catch (NoSuchMethodException nsme)
        {
            return false;
        }

        return true;
    }

    public static String getRedirect(JiraWebActionSupport action, String url, boolean unsafe)
    {
        if ( SAFE_REDIRECT_EXISTS )
        {
            return action.getRedirect(url, unsafe);
        } else
        {
            return action.getRedirect(url);
        }
    }

    public static boolean isValid(String url)
    {
        if ( URL_VALIDATOR_EXISTS )
        {
            return com.atlassian.jira.util.UrlValidator.isValid(url);
        } else
        {
        	try
        	{
        		new URL(url);
        	} catch (MalformedURLException e)
        	{
        		return false;
        	}
           return true;
        }
    }


    @SuppressWarnings("deprecation")
    public static Set<String> getAllIssueKeys(IssueManager issueManager, ChangeHistoryManager changeHistoryManager, Issue issue)
    {
        if (GET_ALL_ISSUE_KEYS_EXISTS)
        {
            return issueManager.getAllIssueKeys(issue.getId());
        }
        else
        {
            Set<String> allIssueKeys = new HashSet<String>();
            if (issue != null)
            {
                // adding current issue key
                allIssueKeys.add(issue.getKey());
                // Adding previous issue keys
                allIssueKeys.addAll(changeHistoryManager.getPreviousIssueKeys(issue.getId()));
            }
            return allIssueKeys;
        }
    }

    public static Set<String> getAllProjectKeys(ProjectManager projectManager, Project project)
    {
        if (GET_ALL_PROJECT_KEYS_EXISTS)
        {
            return projectManager.getAllProjectKeys(project.getId());
        }
        else
        {
            return Collections.singleton(project.getKey());
        }
    }

}
