package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
