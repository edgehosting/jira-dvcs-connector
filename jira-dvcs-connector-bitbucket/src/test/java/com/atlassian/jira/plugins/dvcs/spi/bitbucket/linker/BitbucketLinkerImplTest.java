package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import static org.testng.Assert.*;

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

    @InjectMocks
    BitbucketLinkerImpl bitbucketLinker;
    String thisJiraURL = "https://jira.example.com/";

    Repository repository;




    @BeforeMethod
    public void setUp() throws Exception
    {

    }

    @Test
    public void testUnlinkRepository() throws Exception
    {

    }

    @Test
    public void testLinkRepository() throws Exception
    {

    }

    private BitbucketRepositoryLink createLink(String projectKey){
        BitbucketRepositoryLink repositoryLink = new BitbucketRepositoryLink();


    }

    private BitbucketRepositoryLinkHandler createHandler(){
        BitbucketRepositoryLinkHandler handler = new BitbucketRepositoryLinkHandler();
        handler.setUrl(thisJiraURL);
        handler.setName("jira");
        handler.
    }
}