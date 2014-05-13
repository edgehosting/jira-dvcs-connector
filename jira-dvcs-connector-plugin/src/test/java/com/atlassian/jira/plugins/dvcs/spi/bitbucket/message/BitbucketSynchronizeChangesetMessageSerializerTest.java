package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.0
 */
public class BitbucketSynchronizeChangesetMessageSerializerTest
{
    @Mock
    RepositoryService repositoryService;

    @Mock
    Synchronizer synchronizer;

    @Mock
    BitbucketSynchronizeChangesetMessage payload;

    @Mock
    Repository repository;

    BitbucketSynchronizeChangesetMessageSerializer serializer;

    @BeforeClass
    public void initialise()
    {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeTest
    public void given()
    {
        serializer = new BitbucketSynchronizeChangesetMessageSerializer();
    }


    @Test
    public void testSerializeTestNullExclude() throws Exception
    {
        //given
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getRefreshAfterSynchronizedAt()).thenReturn(new Date());
        //when
        String result = serializer.serialize(payload);
        assertThat(result, not(containsString("exclude")));
    }

    @Test
    public void testSerializeTestEmptyExclude() throws Exception
    {
        //given
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getRefreshAfterSynchronizedAt()).thenReturn(new Date());
        when(payload.getExclude()).thenReturn(new ArrayList<String>());
        //when
        String result = serializer.serialize(payload);
        assertThat(result, not(containsString("exclude")));
    }

    @Test
    public void testSerializeTestWithExclude() throws Exception
    {
        //given
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getRefreshAfterSynchronizedAt()).thenReturn(new Date());
        List<String> excludes = new ArrayList<String>();
        excludes.add("blah");
        when(payload.getExclude()).thenReturn(excludes);

        //when
        String result = serializer.serialize(payload);
        assertThat(result, containsString("\"exclude\":\"blah\""));
    }
}
