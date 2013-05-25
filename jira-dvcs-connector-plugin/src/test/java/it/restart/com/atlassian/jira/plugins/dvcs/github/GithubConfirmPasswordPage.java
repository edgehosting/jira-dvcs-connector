package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class GithubConfirmPasswordPage implements Page
{

    @ElementBy(id = "confirm-password")
    PageElement confirmPasswordInput;

    @ElementBy(xpath = "//div[@class='auth-form-body']/button")
    PageElement confirmPasswordButton;

    @Override
    public String getUrl()
    {
        throw new UnsupportedOperationException();
    }

    public void confirmPassword(String password)
    {
        confirmPasswordInput.type(password);
        confirmPasswordButton.click();
    }
}
