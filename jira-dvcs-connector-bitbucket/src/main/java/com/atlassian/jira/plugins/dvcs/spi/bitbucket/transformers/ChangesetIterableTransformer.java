package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;

/**
 * ChangesetIterableTransformer
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetIterableTransformer {
    private static final Pattern HGRC_MAIL_PATTERN = Pattern.compile("\\<(.*)\\>");
    
    
    private ChangesetIterableTransformer() {}

    
    public static Iterable<Changeset> fromBitbucketChangesetIterable(final Repository repository,
            final Iterable<BitbucketChangeset> bitbucketChangesets)
    {
        return new Iterable<Changeset>() {

            @Override
            public Iterator<Changeset> iterator()
            {
                return new ChangesetIteratorAdapter(repository, bitbucketChangesets.iterator());
            }
        };
    }
    
    
    private static final class ChangesetIteratorAdapter implements Iterator<Changeset>
    {
        private final Iterator<BitbucketChangeset> bitbucketIterator;
        private final Repository repository;
        
        private ChangesetIteratorAdapter(Repository repository, Iterator<BitbucketChangeset> bitbucketIterator)
        {
            this.repository = repository;
            this.bitbucketIterator = bitbucketIterator;
        }

        @Override
        public boolean hasNext()
        {
            return bitbucketIterator.hasNext();
        }

        @Override
        public Changeset next()
        {
            BitbucketChangeset bitbucketChangeset = bitbucketIterator.next();
            //TODO finish this one
//            List<ChangesetFile> files =
//                    ChangesetFileTransformer.fromBitbucketCahngeetWithDiffstat(bitbucketChangeset.getFiles());
            
			Changeset changeset = new Changeset(repository.getId(),
                                                bitbucketChangeset.getNode(),
                                                null,
                                                bitbucketChangeset.getRawAuthor(),
                                                bitbucketChangeset.getAuthor(),
                                                bitbucketChangeset.getUtctimestamp(),
                                                bitbucketChangeset.getRawNode(),
                                                bitbucketChangeset.getBranch(), // TODO if null, set to "default"?
                                                bitbucketChangeset.getMessage(),
                                                bitbucketChangeset.getParents(),
                                                null,//files,
                                                0//files.size()
            );
			
			changeset.setAuthorEmail(parseUsersEmailHgRcFormatOrNull(bitbucketChangeset.getRawAuthor()));

			return changeset;
        }
        
        private static String parseUsersEmailHgRcFormatOrNull(String rawAuthor)
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

        @Override
        public void remove()
        {
            bitbucketIterator.remove();
        }
    }
}
