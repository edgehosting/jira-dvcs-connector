package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.ao.EntityBeanGenerator;
import com.beust.jcommander.internal.Sets;
import net.java.ao.RawEntity;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EntityBeanGeneratorTest
{
    @InjectMocks
    EntityBeanGenerator generator;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void beanShouldImplementSettersAndGetters() throws Exception
    {
        SubBean bean = generator.createInstanceOf(SubBean.class);
        assertNotNull(bean);

        final String author = "me";
        bean.setChildString(author);
        assertEquals(author, bean.getChildString());

        final Integer domainId = 123;
        bean.setInteger(domainId);
        assertEquals(domainId, bean.getInteger());
    }

    @Test
    public void beanShouldImplementEqualsAndHashcode() throws Exception
    {
        RepositoryPullRequestMapping bean1 = generator.createInstanceOf(RepositoryPullRequestMapping.class);
        RepositoryPullRequestMapping bean2 = generator.createInstanceOf(RepositoryPullRequestMapping.class);

        Set<RepositoryPullRequestMapping> set = Sets.newHashSet();
        set.add(bean1);
        set.add(bean2);
        assertEquals(set.size(), 2);

        set.remove(bean1);
        assertEquals(set.size(), 1);
        assertEquals(true, set.contains(bean2));
    }

    @Test(expectedExceptions = { UnsupportedOperationException.class })
    public void beanShouldNotImplementOtherEntityMethods() throws Exception
    {
        RepositoryPullRequestMapping bean = generator.createInstanceOf(RepositoryPullRequestMapping.class);
        assertNotNull(bean);

        // should throw
        bean.save();
    }

    @Test
    public void beanShouldReturnCorrectDefaultValuesForPrimitives() throws Exception
    {
        PrimitiveBean primitive = generator.createInstanceOf(PrimitiveBean.class);

        assertTrue(0 == primitive.getByte());
        assertTrue(0 == primitive.getShort());
        assertTrue(0 == primitive.getInt());
        assertTrue(0L == primitive.getLong());
        assertTrue(0.0f == primitive.getFloat());
        assertTrue(0.0d == primitive.getDouble());
        assertTrue('\u0000' == primitive.getChar());
        assertTrue(!primitive.isBoolean());
    }

    @Test
    public void beanShouldHaveSaneEqualsAndHashCode() throws Exception
    {
        PrimitiveBean primitive = generator.createInstanceOf(PrimitiveBean.class);

        assertThat(primitive, equalTo(primitive));
        assertThat(primitive.hashCode(), equalTo(primitive.hashCode()));
    }

    interface Bean extends RawEntity
    {
        Integer getInteger();
        void setInteger(Integer integer);
    }

    interface SubBean extends Bean
    {
        String getChildString();
        void setChildString(String childString);
    }

    interface PrimitiveBean extends RawEntity
    {
        byte getByte();
        short getShort();
        int getInt();
        long getLong();
        float getFloat();
        double getDouble();
        boolean isBoolean();
        char getChar();
    }
}
