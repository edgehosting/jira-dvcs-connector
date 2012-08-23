package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import javax.inject.Inject;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class BitbucketOAuthConfigPage implements Page {

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
        return "/secure/admin/ConfigureBitbucketOAuth!default.jspa";
    }

    public void setCredentials(String clientID, String clientSecret)
    {
        clientIDInput.clear();
        clientIDInput.type(clientID);
        clientSecretInput.clear();
        clientSecretInput.type(clientSecret);
        submitButton.click();
        Poller.waitUntilTrue("Expected success of setting credentials",
                             ghMessagesDiv.timed().hasText("Bitbucket client credentials set correctly."));
    }
}
