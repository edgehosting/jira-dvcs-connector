package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.project.ProjectImpl;
import com.atlassian.sal.api.UrlMode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketConstants;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLinkHandler;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import java.util.Arrays;

public class BitbucketLinkerImplTest
{

    @Mock
    Logger log;

    @Mock
    RepositoryService repositoryService;

    @Mock
    ProjectManager projectManager;

    @Mock
    RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint;

    @Mock
    ApplicationProperties applicationProperties;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    BitbucketClientBuilderFactory builderFactory;

    BitbucketLinkerImpl bitbucketLinker;

    String thisJiraURL = "https://jira.example.com/";

    @Mock
    Project project1;

    @Mock
    Project project2;

    List<Project> projectList;

    BitbucketRepositoryLink link1 = createLink(("TEST"));
    BitbucketRepositoryLink link2 = createLink(("ASDF"));
    List<BitbucketRepositoryLink> links;
    List<String> projectKeys = ImmutableList.of("TEST", "ASDF");
    private String regex = "(?<!\\w)((TEST|ASDF)-\\d+)(?!\\w)" ;


    Repository repository;


    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        repository = setUpRepository();
        links = new ArrayList<BitbucketRepositoryLink>();
        links.add(link1);
        links.add(link2);

        projectList = setUpProjectList();
        when(repositoryLinkRemoteRestpoint.getRepositoryLinks
                (repository.getOrgName(), repository.getSlug())).thenReturn(new ArrayList<BitbucketRepositoryLink>());

        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(thisJiraURL);
        bitbucketLinker = new BitbucketLinkerImpl(builderFactory, applicationProperties, projectManager, repositoryService);
        when(builderFactory.forRepository(repository).
                closeIdleConnections().build().getRepositoryLinksRest()).thenReturn(repositoryLinkRemoteRestpoint);
        when(builderFactory.forRepository(repository).
                build().getRepositoryLinksRest()).thenReturn(repositoryLinkRemoteRestpoint);

        when(projectManager.getProjectObjects()).thenReturn(projectList);

    }

    @Test
    public void testUnlinkRepository() throws Exception
    {
        when(repositoryLinkRemoteRestpoint.getRepositoryLinks
                (repository.getOrgName(), repository.getSlug())).thenReturn(links);
        bitbucketLinker.unlinkRepository(repository);
        verify(repositoryLinkRemoteRestpoint).getRepositoryLinks(repository.getOrgName(),repository.getSlug());
        verify(repositoryLinkRemoteRestpoint).removeRepositoryLink(repository.getOrgName(), repository.getSlug(), link1.getId());
        verify(repositoryLinkRemoteRestpoint).removeRepositoryLink(repository.getOrgName(), repository.getSlug(), link2.getId());
        verifyNoMoreInteractions(repositoryLinkRemoteRestpoint);
    }

    @Test
    public void testLinkRepositoryRemovalOfExistingKeys() throws Exception
    {
        when(repositoryLinkRemoteRestpoint.getRepositoryLinks
                (repository.getOrgName(), repository.getSlug())).thenReturn(links);

        List<String> projectKeysSubset = Arrays.asList(projectKeys.get(0));
        when(repositoryService.getPreviouslyLinkedProjects(repository)).thenReturn(projectKeysSubset);
        bitbucketLinker.linkRepository(repository, new HashSet<String>(projectKeys)); //necessary because link repository mutates the set it works on
        verify(repositoryLinkRemoteRestpoint).removeRepositoryLink(repository.getOrgName(), repository.getSlug(), link1.getId());
        verify(repositoryLinkRemoteRestpoint).removeRepositoryLink(repository.getOrgName(), repository.getSlug(), link2.getId());
    }

    @Test
    public void testLinkRepositoryWhenNoNewKeys() throws Exception
    {
        when(repositoryService.getPreviouslyLinkedProjects(repository)).thenReturn(projectKeys);

        bitbucketLinker.linkRepository(repository, new HashSet<String>(projectKeys)); //necessary because link repository mutates the set it works on
        verify(repositoryLinkRemoteRestpoint, never()).addCustomRepositoryLink(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testLinkRepositoryWhenNewKeysInChangesets() throws Exception
    {
        List<String> projectKeysSubset = Arrays.asList(projectKeys.get(0));
        when(repositoryService.getPreviouslyLinkedProjects(repository)).thenReturn(projectKeysSubset);
        bitbucketLinker.linkRepository(repository, new HashSet<String>(projectKeys));
        verify(repositoryLinkRemoteRestpoint).addCustomRepositoryLink(eq(repository.getOrgName()), eq(repository.getSlug())
                , eq(thisJiraURL + "browse/\\1"), anyString());
    }

    private BitbucketRepositoryLink createLink(String projectKey)
    {
        BitbucketRepositoryLink repositoryLink = new BitbucketRepositoryLink();
        repositoryLink.setHandler(createHandler(projectKey));
        repositoryLink.setId(projectKey.hashCode());
        return repositoryLink;
    }

    private BitbucketRepositoryLinkHandler createHandler(String key)
    {
        BitbucketRepositoryLinkHandler handler = new BitbucketRepositoryLinkHandler();
        handler.setUrl(thisJiraURL);
        handler.setDisplayTo(thisJiraURL);
        handler.setName("custom");
        handler.setKey(key);
        return handler;

    }

    private Repository setUpRepository()
    {
        repository = new Repository();
        repository.setOrgName("some account name");
        repository.setSlug("myrepo");
        return repository;
    }

    private List<Project> setUpProjectList()
    {
        projectList = new ArrayList<Project>();
        when(project1.getKey()).thenReturn("ASDF");
        when(project2.getKey()).thenReturn("TEST");

        projectList.add(project1);
        projectList.add(project2);
        return projectList;
    }


}