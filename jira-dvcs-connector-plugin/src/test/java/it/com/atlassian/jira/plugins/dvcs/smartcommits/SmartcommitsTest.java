package it.com.atlassian.jira.plugins.dvcs.smartcommits;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;

public class SmartcommitsTest
{

	@Test
	public void testCommentIssueCommand() throws IOException
	{

		HttpSenderUtils.sendPostHttpRequest("", EasyMap.build("payload", resource("SmartcommitsTest-bitbucket.json")));

	}

	private String resource(String name)
	{
		try
		{

			return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(name));

		} catch (IOException e)
		{
			throw new RuntimeException("Can not load resource " + name, e);
		}
	}
}
