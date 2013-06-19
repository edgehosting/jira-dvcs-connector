package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;

/**
 * ChangesetTransformer
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetTransformer {
    // parsing following JSON part: "raw_author": "Mary Anthony <manthony@172-28-13-105.staff.sf.atlassian.com>"
    private static final Pattern HGRC_MAIL_PATTERN = Pattern.compile("\\<(.*)\\>");


    private ChangesetTransformer() {}


    public static Changeset fromBitbucketChangeset(int repositoryId, BitbucketChangeset bitbucketChangeset)
    {
        List<ChangesetFile> changesetFiles = ChangesetFileTransformer.fromBitbucketChangesetFiles(
                bitbucketChangeset.getFiles());
        String authorEmail = parseEmailFromRawAuthor(bitbucketChangeset.getRawAuthor());

        Changeset changeset = new Changeset(
                repositoryId,
                bitbucketChangeset.getNode(),
                bitbucketChangeset.getRawAuthor(),
                bitbucketChangeset.getAuthor(),
                bitbucketChangeset.getUtctimestamp(),
                bitbucketChangeset.getRawNode(),
                bitbucketChangeset.getBranch(),
                bitbucketChangeset.getMessage(),
                bitbucketChangeset.getParents(),
                changesetFiles,
                changesetFiles.size(),
                authorEmail
        );

        return changeset;
    }

    public static Changeset fromBitbucketNewChangeset(int repositoryId, BitbucketNewChangeset bitbucketChangeset)
    {
        String authorEmail = null;
        String rawAuthor = null;
        if (bitbucketChangeset.getAuthor() != null)
        {
            authorEmail = parseEmailFromRawAuthor(bitbucketChangeset.getAuthor().getRaw());
            rawAuthor = bitbucketChangeset.getAuthor().getRaw();
        }

        Changeset changeset = new Changeset(
                repositoryId,
                bitbucketChangeset.getHash(),
                rawAuthor,
                null, // bitbucketChangeset.getAuthor(),
                bitbucketChangeset.getDate(),
                bitbucketChangeset.getHash(),
                null, // bitbucketChangeset.getBranch(),
                bitbucketChangeset.getMessage(),
                transformParents(bitbucketChangeset.getParents()),
                null,// changesetFiles,
                0,// changesetFiles.size(),
                authorEmail
        );

        return changeset;
    }

    private static List<String> transformParents(List<BitbucketNewChangeset> parents)
    {
        List<String> parentsList = new ArrayList<String>();
        for ( BitbucketNewChangeset parent : parents)
        {
            parentsList.add(parent.getHash());
        }
        return parentsList;
    }

    private static String parseEmailFromRawAuthor(String rawAuthor)
	{
		try
		{
			Matcher matcher = HGRC_MAIL_PATTERN.matcher(rawAuthor);
			matcher.find();
			return matcher.group(1).trim();
		} catch (Exception e)
		{
			// nop
			return null;
		}
	}
}
