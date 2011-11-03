package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.opensymphony.util.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

public abstract class DvcsRepositoryManager implements RepositoryManager
{

    private final RepositoryPersister repositoryPersister;
    private final Communicator communicator;
    private final Encryptor encryptor;
    private final ApplicationProperties applicationProperties;

	/* Maps ProjectMapping to SourceControlRepository */
	private final Function<ProjectMapping, SourceControlRepository> TO_SOURCE_CONTROL_REPOSITORY = new Function<ProjectMapping, SourceControlRepository>()
	{
		public SourceControlRepository apply(ProjectMapping pm)
		{
			String decryptedPassword = encryptor.decrypt(pm.getPassword(), pm.getProjectKey(), pm.getRepositoryUrl());
			String decryptedAdminPassword = encryptor.decrypt(pm.getAdminPassword(), pm.getProjectKey(),
					pm.getRepositoryUrl());
			return new DefaultSourceControlRepository(pm.getID(), RepositoryUri.parse(pm.getRepositoryUrl())
					.getRepositoryUrl(), pm.getProjectKey(), pm.getUsername(), decryptedPassword,
					pm.getAdminUsername(), decryptedAdminPassword, pm.getRepositoryType());
		}
	};

	private final Function<IssueMapping, Changeset> TO_CHANGESET = new Function<IssueMapping, Changeset>()
	{
		public Changeset apply(IssueMapping from)
		{
			ProjectMapping pm = repositoryPersister.getRepository(from.getRepositoryId());
			SourceControlRepository repository = TO_SOURCE_CONTROL_REPOSITORY.apply(pm);
			return getCommunicator().getChangeset(repository, from.getNode());
		}
	};

    public DvcsRepositoryManager(Communicator communicator, RepositoryPersister repositoryPersister, Encryptor encryptor, ApplicationProperties applicationProperties)
    {
        this.communicator = communicator;
        this.repositoryPersister = repositoryPersister;
        this.encryptor = encryptor;
        this.applicationProperties = applicationProperties;
    }

	public SourceControlRepository addRepository(String projectKey, String repositoryUrl, String username,
			String password, String adminUsername, String adminPassword)
	{
		// Remove trailing slashes from URL
		if (repositoryUrl.endsWith("/"))
		{
			repositoryUrl = repositoryUrl.substring(0, repositoryUrl.length() - 1);
		}

		// Set all URLs to HTTPS
		if (repositoryUrl.startsWith("http:"))
		{
			repositoryUrl = repositoryUrl.replaceFirst("http:", "https:");
		}

		String encryptedPassword = encryptor.encrypt(password, projectKey, repositoryUrl);
		String encryptedAdminPassword = encryptor.encrypt(adminPassword, projectKey, repositoryUrl);
		ProjectMapping pm = repositoryPersister.addRepository(projectKey, repositoryUrl, username,
				encryptedPassword, adminUsername, encryptedAdminPassword, getRepositoryType());
		return TO_SOURCE_CONTROL_REPOSITORY.apply(pm);
	}

	public SourceControlRepository getRepository(int repositoryId)
	{
		ProjectMapping repository = repositoryPersister.getRepository(repositoryId);
		return TO_SOURCE_CONTROL_REPOSITORY.apply(repository);
	}

	public List<SourceControlRepository> getRepositories(String projectKey)
	{
		List<ProjectMapping> repositories = repositoryPersister.getRepositories(projectKey, getRepositoryType());
		return Lists.transform(repositories, TO_SOURCE_CONTROL_REPOSITORY);
	}

	public List<Changeset> getChangesets(String issueKey)
	{
		List<IssueMapping> issueMappings = repositoryPersister.getIssueMappings(issueKey);
		return Lists.transform(issueMappings, TO_CHANGESET);
	}

	public void removeRepository(int id)
	{
		repositoryPersister.removeRepository(id);
		// TODO Should we also delete IssueMappings? Yes we should.
	}

	public void addChangeset(SourceControlRepository repository, String issueId, Changeset changeset)
	{
		repositoryPersister.addChangeset(issueId, changeset.getRepositoryId(), changeset.getNode());
	}

	public SourceControlUser getUser(SourceControlRepository repository, String username)
	{
		return getCommunicator().getUser(repository, username);
	}


    public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset)
    {

            String htmlParentHashes = "";
            if (!changeset.getParents().isEmpty())
            {
                for (String node : changeset.getParents())
                {
                    // ehm ehm ... what is this? shouldn't this be
                    // htmlParentHashes+=
                    htmlParentHashes = "<tr><td style='color: #757575'>Parent:</td><td><a href='" + repository.getUrl() +
                            "/changeset/" + node + "' target='_new'>" + node + "</a></td></tr>";
                }
            }

            Map<String, String> mapFiles = new HashMap<String, String>();
            String htmlFile = "";
            if (!changeset.getFiles().isEmpty())
            {
                for (ChangesetFile file : changeset.getFiles())
                {
                    String fileName = file.getFile();
                    String color = file.getFileAction().getColor();
                    String fileActionName = file.getFileAction().toString();
                    String fileCommitURL = repository.getUrl() + "/src/" + changeset.getNode() + "/" + urlEncode(file.getFile());
                    htmlFile = "<li><span style='color:" + color + "; font-size: 8pt;'>" +
                            TextUtils.htmlEncode(fileActionName) + "</span> <a href='" +
                            fileCommitURL + "' target='_new'>" + fileName + "</a></li>";
                    mapFiles.put(fileName, htmlFile);
                }
            }

            String htmlFiles = "";
            String htmlFilesHiddenDescription = "";
            Integer numSeeMore = 0;
            Random randDivID = new Random(System.currentTimeMillis());

            // Sort and compose all files
            Iterator<String> it = mapFiles.keySet().iterator();
            Object obj;

            String htmlHiddenDiv = "";

            if (mapFiles.size() <= 5)
            {
                while (it.hasNext())
                {
                    obj = it.next();
                    htmlFiles += mapFiles.get(obj);
                }
                htmlFilesHiddenDescription = "";
            }
            else
            {
                Integer i = 0;

                while (it.hasNext())
                {
                    obj = it.next();

                    if (i <= 4)
                    {
                        htmlFiles += mapFiles.get(obj);
                    }
                    else
                    {
                        htmlHiddenDiv += mapFiles.get(obj);
                    }

                    i++;
                }

                numSeeMore = mapFiles.size() - 5;
                Integer divID = randDivID.nextInt();

                htmlFilesHiddenDescription = "<div class='see_more' id='see_more_" + divID.toString() + "' style='color: #3C78B5; cursor: pointer; text-decoration: underline;' onclick='toggleMoreFiles(" + divID.toString() + ")'>" +
                        "See " + numSeeMore.toString() + " more" +
                        "</div>" +
                        "<div class='hide_more' id='hide_more_" + divID.toString() + "' style='display: none; color: #3C78B5;  cursor: pointer; text-decoration: underline;' onclick='toggleMoreFiles(" + divID.toString() + ")'>Hide " + numSeeMore.toString() + " Files</div>";

                htmlHiddenDiv = htmlFilesHiddenDescription + "<div id='" + divID.toString() + "' style='display: none;'><ul>" + htmlHiddenDiv + "</ul></div>";

            }

            String htmlCommitEntry = "" +
                    "<table>" +
                    "<tr>" +
                    "<td valign='top' width='70px'><a href='#user_url' target='_new'><img src='#gravatar_url' border='0'></a></td>" +
                    "<td valign='top'>" +
                    "<div style='padding-bottom: 6px'><a href='#user_url' target='_new'>#user_name - #login</a></div>" +
                    "<table>" +
                    "<tr>" +
                    "<td>" +
                    "<div style='border-left: 2px solid #C9D9EF; background-color: #EAF3FF; color: #5D5F62; padding: 5px; margin-bottom: 10px;'>#commit_message</div>" +

                    "<ul>" +
                    htmlFiles +
                    "</ul>" +

                    htmlHiddenDiv +

                    "<div style='margin-top: 10px'>" +
                    "<img src='" + getApplicationProperties().getBaseUrl() + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg' align='center'> <span class='commit_date' style='color: #757575; font-size: 9pt;'>#formatted_commit_date</span>" +
                    "</div>" +

                    "</td>" +

                    "<td width='400' style='padding-top: 0px' valign='top'>" +
                    "<div style='border-left: 2px solid #cccccc; margin-left: 15px; margin-top: 0px; padding-top: 0px; padding-left: 10px'>" +
                    "<table style='margin-top: 0px; padding-top: 0px;'>" +
                    "<tr><td style='color: #757575'>Changeset:</td><td><a href='#commit_url' target='_new'>#commit_hash</a></td></tr>" +
                    htmlParentHashes +
                    "</table>" +
                    "</div>" +
                    "</td>" +

                    "</tr>" +
                    "</table>" +
                    "</td>" +
                    "</tr>" +
                    "</table>";

            String authorName = changeset.getRawAuthor();
            String login = changeset.getAuthor();
            String commitURL = changeset.getCommitURL(repository);
            SourceControlUser user = getUser(repository, changeset.getAuthor());
            String gravatarUrl = user.getAvatar().replace("s=32", "s=60");
            RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
            String baseRepositoryUrl = uri.getBaseUrl();

            htmlCommitEntry = htmlCommitEntry.replace("#gravatar_url", gravatarUrl);
            htmlCommitEntry = htmlCommitEntry.replace("#user_url", baseRepositoryUrl + "/" + urlEncode(login));
            htmlCommitEntry = htmlCommitEntry.replace("#login", TextUtils.htmlEncode(login));
            htmlCommitEntry = htmlCommitEntry.replace("#user_name", TextUtils.htmlEncode(authorName));
            htmlCommitEntry = htmlCommitEntry.replace("#commit_message", TextUtils.htmlEncode(changeset.getMessage()));
            htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_time", changeset.getTimestamp());
            htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_date", changeset.getTimestamp());
            htmlCommitEntry = htmlCommitEntry.replace("#commit_url", commitURL);
            htmlCommitEntry = htmlCommitEntry.replace("#commit_hash", changeset.getNode());
            //htmlCommitEntry = htmlCommitEntry.replace("#tree_url", "https://github.com/" + login + "/" + projectName + "/tree/" + commit_hash);
            //htmlCommitEntry = htmlCommitEntry.replace("#tree_hash", commitTree);
            return htmlCommitEntry;
    }


    public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progressProvider)
    {
        return new DefaultSynchronisationOperation(key, this, getCommunicator(), progressProvider);
    }

	public abstract String getRepositoryType();

	public abstract boolean canHandleUrl(String url);


    protected String urlEncode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("required encoding not found");
        }
    }

    public ApplicationProperties getApplicationProperties()
    {
        return applicationProperties;
    }

    public Communicator getCommunicator()
    {
        return communicator;
    }


}
