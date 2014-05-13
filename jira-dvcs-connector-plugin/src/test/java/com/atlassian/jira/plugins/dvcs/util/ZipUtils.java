package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.plugin.util.zip.FileUnzipper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Martin Skurla
 */
public class ZipUtils
{

    public static File extractRepoZipIntoTempDir(String pathToRepoZip) throws IOException, URISyntaxException
    {
        URL repoZipResource = ZipUtils.class.getClassLoader().getResource(pathToRepoZip);

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "" + System.currentTimeMillis());
        tempDir.mkdir();

        FileUnzipper fileUnzipper = new FileUnzipper(new File(repoZipResource.toURI()), tempDir);
        fileUnzipper.unzip();

        return tempDir;
    }
}
