package com.atlassian.jira.plugins.dvcs.spi.github;

import com.atlassian.jira.bc.user.UserService;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.springframework.stereotype.Component;

@Component
public class UserServiceFactory
{
    public org.eclipse.egit.github.core.service.UserService createUserService(GitHubClient gitHubClient){
        return new org.eclipse.egit.github.core.service.UserService(gitHubClient);
    }

}
