package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import net.java.ao.Entity;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActiveObjectsUtilsTest
{
    @Mock
    private ActiveObjects activeObjects;

    @BeforeMethod
    public void initializeMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDeletion()
    {
        final  List<EntityContext> entities = sampleEntitites();

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable
            {
                EntityStreamCallback<Entity, Integer> callback = (EntityStreamCallback<Entity, Integer>)invocation.getArguments()[2];
                for (EntityContext entityContext : entities)
                {
                    callback.onRowRead(entityContext.getEntity());
                }
                return null;
            }
        }).when(activeObjects).stream(any(Class.class), any(Query.class), any(EntityStreamCallback.class));

        when(activeObjects.find(any(Class.class), anyString(), Mockito.<Object>anyVararg())).then(new Answer<Entity[]>()
        {
            @Override
            public Entity[] answer(final InvocationOnMock invocation) throws Throwable
            {
                String criteria = (String) invocation.getArguments()[1];
                List<Entity> result = new ArrayList<Entity>();
                for (String id : Splitter.on(",").split(criteria.replaceAll("ID IN \\((.+)\\)", "$1")))
                {
                    result.add(entities.get(Integer.valueOf(id.trim())).getEntity());
                    System.out.println(id + " " + entities.get(Integer.valueOf(id.trim())).getEntity().getID());
                }

                return result.toArray(new Entity[result.size()]);
            }
        }
        );
        ActiveObjectsUtils.delete(activeObjects, Entity.class, Mockito.mock(Query.class));

        verify(activeObjects, atLeastOnce()).delete(Iterables.toArray(Iterables.transform(entities, new Function<EntityContext, Entity>()
        {
            @Override
            public Entity apply(@Nullable final EntityContext input)
            {
                return input.getCaptor().capture();
            }
        }), Entity.class));

        for (EntityContext entityContext : entities)
        {
            Assert.assertEquals(entityContext.getCaptor().getValue().getID(), entityContext.getEntity().getID());
        }
    }

    private List<EntityContext> sampleEntitites()
    {
        List<EntityContext> entities = new ArrayList<EntityContext>();
        for (int id = 0; id < 100 ; id++)
        {
            entities.add(new EntityContext(createEntity(id)));
        }

        return entities;
    }

    private Entity createEntity(int id)
    {
        Entity entity = Mockito.mock(Entity.class);
        when(entity.getID()).thenReturn(id);
        return entity;
    }

    private class EntityContext
    {
        private final Entity entity;
        private final ArgumentCaptor<Entity> captor;

        public EntityContext(Entity entity)
        {
            this.entity = entity;
            this.captor = ArgumentCaptor.forClass(Entity.class);
        }

        public Entity getEntity()
        {
            return entity;
        }

        public ArgumentCaptor<Entity> getCaptor()
        {
            return captor;
        }
    }
}
