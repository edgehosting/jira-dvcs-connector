package com.atlassian.jira.plugins.dvcs.bitbucket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.DefaultRequestHelper;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandlerFactory;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketChangesetIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.google.common.collect.Iterables;

/**
 * Unit tests for {@link DvcsCommunicator}
 */
public class DefaultBitbucketTest
{

	@SuppressWarnings("rawtypes")
	@Mock
	private RequestFactory requestFactory;
	@Mock
	private AuthenticationFactory authenticationFactory;
	@Mock
	private Repository repository;
	@Mock
	private Request<?, ?> request;
	@Mock
	private ExtendedResponseHandlerFactory extendedResponseHandlerFactory;
	@Mock
	private ExtendedResponseHandler responseHandler;
	@Mock
	private BitbucketLinker linker;
    @Mock
    private PluginAccessor pluginAccessor;


	@Before
	public void setup() throws Exception
	{
		MockitoAnnotations.initMocks(this);
	}

	private String resource(String name) throws IOException
	{
		return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(name));
	}

	private void setupBitbucketConnection() throws IOException
	{

		when(requestFactory.createRequest(any(Request.MethodType.class), anyString())).thenReturn(request);
		when(repository.getRepositoryUrl()).thenReturn("https://bitbucket.org/atlassian/jira-bitbucket-connector");
		when(repository.getOrgName()).thenReturn("atlassian");
		when(repository.getSlug()).thenReturn("jira-bitbucket-connector");
		when(extendedResponseHandlerFactory.create()).thenReturn(responseHandler);

		when(responseHandler.getExtendedResponse()).thenReturn(
				new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesets-12.json"))).thenReturn(
				new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesetFiles.json")));

	}

	@Test
	public void testGetChangesetsLargeFromTip() throws Exception
	{
		setupBitbucketConnection();

		BitbucketCommunicator bitbucketCommunicator = createCommunicator();

		final BitbucketChangesetIterator changesetIterator = new BitbucketChangesetIterator(bitbucketCommunicator,
				repository, null);

		Iterable<Changeset> iterable = new Iterable<Changeset>()
		{
			@Override
			public Iterator<Changeset> iterator()
			{
				return changesetIterator;
			}
		};

		List<Changeset> list = new ArrayList<Changeset>();
		Iterables.addAll(list, iterable);

		assertEquals(15, list.size());

	}

    private BitbucketCommunicator createCommunicator()
    {
        return new BitbucketCommunicator(authenticationFactory, new DefaultRequestHelper(
                requestFactory, extendedResponseHandlerFactory), linker, pluginAccessor)
		{
		    @Override
            protected String getPluginVersion(PluginAccessor pluginAccessor)
            {
                return "123";
            }
		};
    }

	@Test
	public void testIteratorCyclesOnNext() throws Exception
	{
		setupBitbucketConnection();

        BitbucketCommunicator bitbucketCommunicator = createCommunicator();

		final BitbucketChangesetIterator changesetIterator = new BitbucketChangesetIterator(bitbucketCommunicator,
				repository, null);

		for (int i = 0; i < 15; i++)
		{
			try
			{
				changesetIterator.next();
			} catch (Exception e)
			{
				fail("next() failed at index [ " + i + " ]");
			}
		}
	}

}
