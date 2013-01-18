package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;


/**
 *
 */
public class GithubEnterpriseOAuthConfigPage extends GithubOAuthConfigPage
{
    @ElementBy(id = "hostUrl")
    PageElement hostUrlInput;
    
    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureGithubEnterpriseOAuth!default.jspa";
    }
    
    public void setCredentials(String hostUrl, String clientID, String clientSecret)
    {
        hostUrlInput.clear();
        hostUrlInput.type(hostUrl);
        clientIDInput.clear();
        clientIDInput.type(clientID);
        clientSecretInput.clear();
        clientSecretInput.type(clientSecret);
        submitButton.click();
        Poller.waitUntilTrue("Expected success of setting credentials", ghMessagesDiv.timed().hasText("GitHub Host URL And Client Identifiers Set Correctly"));
    }
}
