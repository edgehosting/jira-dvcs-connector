package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;

import javax.inject.Inject;

/**
 *
 */
public class GithubOAuthConfigPage implements Page
{
    public static final String VALID_CLIENT_ID = "263e39164985144bc755";
    public static final String VALID_CLIENT_SECRET = "81df744eba20038a5db4d3434d28c3cde526c943";

    @Inject
    PageBinder pageBinder;

    @ElementBy(id = "Submit")
    PageElement submitButton;

    @ElementBy(name = "clientID")
    SelectElement clientIDInput;

    @ElementBy(id = "clientSecret")
    PageElement clientSecretInput;

    @ElementBy(id = "gh_messages")
    PageElement ghMessagesDiv;

    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureGithubOAuth!default.jspa";
    }

    public void setCredentials(String clientID, String clientSecret)
    {
        clientIDInput.clear();
        clientIDInput.type(clientID);
        clientSecretInput.clear();
        clientSecretInput.type(clientSecret);
        submitButton.click();
        Poller.waitUntilTrue("Expected success of setting credentials", ghMessagesDiv.timed().hasText("GitHub Client Identifiers Set Correctly"));
    }
}
