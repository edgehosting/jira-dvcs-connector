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

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test of BitbucketSynchronizeChangesetMessageSerializer.
 *
 * @since v6.0
 */
public class BitbucketSynchronizeChangesetMessageSerializerTest
{
    @Mock
    private RepositoryService repositoryService;

    @Mock
    private Synchronizer synchronizer;

    @Mock
    private BitbucketSynchronizeChangesetMessage payload;

    @Mock
    private Repository repository;

    private BitbucketSynchronizeChangesetMessageSerializer serializer;

    @BeforeClass
    public void initialiseMocks()
    {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeTest
    public void instantiateSerializer()
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

        //then
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

        //then
        assertThat(result, not(containsString("exclude")));
    }

    @Test
    public void testSerializeTestWithExclude() throws Exception
    {
        //given
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getRefreshAfterSynchronizedAt()).thenReturn(new Date());
        when(payload.getExclude()).thenReturn(singletonList("blah"));

        //when
        final String result = serializer.serialize(payload);

        //then
        assertThat(result, containsString("\"exclude\":\"blah\""));
    }
}
