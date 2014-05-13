package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.service.ConcurrencyService.SynchronizedBlock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test over {@link ConcurrencyService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class ConcurrencyServiceImplTest
{

    /**
     * Tested entity.
     */
    private ConcurrencyService testedObject;

    /**
     * Prepares test environment.
     */
    @Before
    public void before()
    {
        testedObject = new ConcurrencyServiceImpl();
    }

    /**
     * Test of {@link ConcurrencyService#synchronizedBlock(SynchronizedBlock, Object...)}. Whole test is based on pairs of workers, where
     * the result of all workers is count of loops multiply by delta. One of them add something other from pair adds value which creates
     * defined delta, both of them over shared instance.
     */
    @Test
    public void testSycnhronizedBlock()
    {
        class Worker implements Runnable
        {

            private final AtomicInteger val;
            private final int add;
            private final Object[] lockKey;

            public Worker(AtomicInteger val, int add, Object... lockKey)
            {
                this.val = val;
                this.add = add;
                this.lockKey = lockKey;
            }

            @Override
            public void run()
            {
                testedObject.synchronizedBlock(new SynchronizedBlock<Void, RuntimeException>()
                {

                    @Override
                    public Void perform() throws RuntimeException
                    {
                        int before = val.get();
                        Thread.yield(); // suggest switch to other process
                        val.set(before + add);
                        return null;
                    }

                }, lockKey);
            }

        }

        for (int attempt = 0; attempt < 1000; attempt++)
        {
            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(4, 4, Integer.MAX_VALUE, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());

            int delta1 = 0;
            Integer key1 = 1;
            AtomicInteger byKey1 = new AtomicInteger();
            Worker incWorker1 = new Worker(byKey1, 1, key1);
            Worker decWorker1 = new Worker(byKey1, -1 + delta1, key1);

            int delta2 = 5;
            Integer key2 = 2;
            AtomicInteger byKey2 = new AtomicInteger();
            Worker incWorker2 = new Worker(byKey2, 2, key2);
            Worker decWorker2 = new Worker(byKey2, -2 + delta2, key2);

            int loops = 1000;
            for (int i = 0; i < loops; i++)
            {
                threadPool.execute(incWorker1);
                threadPool.execute(decWorker2);
                threadPool.execute(incWorker2);
                threadPool.execute(decWorker1);
            }

            try
            {
                threadPool.shutdown();
                threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);

            } catch (InterruptedException e)
            {
                throw new RuntimeException(e);

            }

            Assert.assertEquals(loops * delta1, byKey1.get());
            Assert.assertEquals(loops * delta2, byKey2.get());
        }
    }

}
