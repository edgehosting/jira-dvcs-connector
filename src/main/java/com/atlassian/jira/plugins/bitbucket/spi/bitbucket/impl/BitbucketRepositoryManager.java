package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.ApplicationProperties;
import com.opensymphony.util.TextUtils;

public class BitbucketRepositoryManager extends DvcsRepositoryManager
{
	private final RepositoryPersister repositoryPersister;
	private final Communicator bitbucketCommunicator;
	
	public BitbucketRepositoryManager(RepositoryPersister repositoryPersister, Communicator communicator, Encryptor encryptor, ApplicationProperties applicationProperties)
	{
        super(encryptor, applicationProperties);
		this.repositoryPersister = repositoryPersister;
		this.bitbucketCommunicator = communicator;
	}

    public String getRepositoryTypeId() {
        return "bitbucketCommunicator";
    }

	public boolean canHandleUrl(String url)
	{
        // Valid URL 
        Pattern p = Pattern.compile("^(https|http)://[a-zA-Z0-9][-a-zA-Z0-9]*.[a-zA-Z0-9]+/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher m = p.matcher(url);
        return m.matches();
	}

    @Override
    public RepositoryPersister getRepositoryPersister() {
        return repositoryPersister;
    }

    @Override
    public Communicator getCommunicator() {
        return bitbucketCommunicator;
    }


	public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progressProvider)
	{
		return new BitbucketSynchronisation(key, this, bitbucketCommunicator, progressProvider);
	}

	public List<Changeset> parsePayload(SourceControlRepository repository, String payload)
	{
        List<Changeset> changesets = new ArrayList<Changeset>();
        try
		{
			JSONObject jsonPayload = new JSONObject(payload);
			JSONArray commits = jsonPayload.getJSONArray("commits");

			for (int i = 0; i < commits.length(); ++i)
			{
				changesets.add(BitbucketChangesetFactory.parse(repository.getId(), commits.getJSONObject(i)));
			}
		} catch (JSONException e)
		{
			throw new SourceControlException(e);
		}
        return changesets;

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
	                "<img src='" + getApplicationProperties().getBaseUrl() + "/download/resources/com.atlassian.jira.plugins.jira-bitbucketCommunicator-connector-plugin/images/document.jpg' align='center'> <span class='commit_date' style='color: #757575; font-size: 9pt;'>#formatted_commit_date</span>" +
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
	public void setupPostcommitHook(SourceControlRepository repo)
	{
		bitbucketCommunicator.setupPostcommitHook(repo, getPostCommitUrl(repo));
	}

	private String getPostCommitUrl(SourceControlRepository repo)
	{
		return getApplicationProperties().getBaseUrl() + "/rest/bitbucket/1.0/repository/"+repo.getId()+"/sync";
	}

	public void removePostcommitHook(SourceControlRepository repo)
	{
		bitbucketCommunicator.removePostcommitHook(repo, getPostCommitUrl(repo));
	}

}
