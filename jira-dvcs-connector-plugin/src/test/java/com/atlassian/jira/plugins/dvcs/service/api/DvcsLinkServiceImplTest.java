package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

public class DvcsLinkServiceImplTest
{
    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private DvcsLinkServiceImpl dvcsLinkService;

    @BeforeMethod
    public void initializeMocks()
    {
        dvcsLinkService = null;
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetDvcsLink() throws Exception
    {
        final Organization org1 = new Organization();
        when(organizationService.get(42, true)).thenReturn(org1);
        final Organization org2 = new Organization();
        when(organizationService.get(43, false)).thenReturn(org2);

        assertSame(org1, dvcsLinkService.getDvcsLink(true, 42));
        assertSame(org2, dvcsLinkService.getDvcsLink(false, 43));
    }

    @Test
    public void testGetDvcsLinks() throws Exception
    {
        final Organization org = new Organization();
        when(organizationService.getAll(true)).thenReturn(Collections.singletonList(org));

        final List<Organization> links = dvcsLinkService.getDvcsLinks(true);
        assertSameIterable(links, org);
        assertImmutable(links);
    }

    @Test
    public void testGetDvcsLinksWithType() throws Exception
    {
        final Organization org = new Organization();
        when(organizationService.getAll(true, "Bitbucket")).thenReturn(Collections.singletonList(org));

        final List<Organization> links = dvcsLinkService.getDvcsLinks(true, "Bitbucket");
        assertSameIterable(links, org);
        assertImmutable(links);
    }

    private static <T> void assertSameIterable(final Iterable<T> actual, final T... expected)
    {
        int i = 0;
        for (T value : actual)
        {
            assertSame(expected[i++], value);
        }
        assertEquals(expected.length, i);
    }

    private static <T> void assertImmutable(final Iterable<T> actual)
    {
        final Iterator<T> i = actual.iterator();
        i.next();
        try
        {
            i.remove();
            fail("should have failed");
        }
        catch (UnsupportedOperationException e)
        {
            // no op
        }
    }

}
