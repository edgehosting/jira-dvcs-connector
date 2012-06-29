package it.com.atlassian.jira.plugins.dvcs.smartcommits;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class SmartcommitsTest
{

	@Test
	public void testCommentIssueCommand() throws IOException
	{

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
