package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Martin Skurla
 */
public class ZipUtils
{
    private ZipUtils() {}


    public static void unzipFileIntoDirectory(ZipFile zipFile, File destinationDir)
    {
        Enumeration files = zipFile.entries();
        FileOutputStream fos = null;

        while (files.hasMoreElements())
        {
            try
            {
                ZipEntry entry = (ZipEntry) files.nextElement();
                InputStream eis = zipFile.getInputStream(entry);
                byte[] buffer = new byte[1024];
                int bytesRead = 0;

                File f = new File(destinationDir.getAbsolutePath() + File.separator + entry.getName());

                if (entry.isDirectory())
                {
                    f.mkdirs();
                    continue;
                } else
                {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                }

                fos = new FileOutputStream(f);

                while ((bytesRead = eis.read(buffer)) != -1)
                {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Error during extracting zip file: ", e);
            }
            finally
            {
                if (fos != null)
                {
                    try
                    {
                        fos.close();
                    } catch (IOException e) {} // ignore
                }
            }
        }
    }
}