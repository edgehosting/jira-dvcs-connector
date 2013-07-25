package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of {@link ConcurrencyService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class ConcurrencyServiceImpl implements ConcurrencyService
{

    /**
     * Maps between lock key and appropriate lock (which holds also locked threads, lock references).
     */
    private Map<LockKey, AtomicInteger> locks = new ConcurrentHashMap<LockKey, AtomicInteger>();

    /**
     * @see ConcurrencyServiceImpl#locks
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private final class LockKey
    {

        /**
         * Delegates of equals.
         */
        private final Object[] keyParts;

        /**
         * Constructor.
         * 
         * @param keyParts
         */
        public LockKey(Object... keyParts)
        {
            this.keyParts = keyParts;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            int result = 0;
            for (Object keyPart : keyParts)
            {
                result += keyPart.hashCode();
            }

            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof LockKey)
            {
                LockKey equalsDelegate = (LockKey) obj;

                if (keyParts.length != equalsDelegate.keyParts.length)
                {
                    return false;
                }

                for (int i = 0; i < keyParts.length; i++)
                {
                    if (!keyParts[i].equals(equalsDelegate.keyParts[i]))
                    {
                        return false;
                    }
                }

                return true;

            } else
            {
                return super.equals(obj);

            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R, T extends Throwable> R synchronizedBlock(SynchronizedBlock<R, T> synchronizedBlock, Object... key) throws T
    {
        LockKey lockKey = new LockKey(key);

        AtomicInteger lock;
        boolean acquired;

        synchronized (locks)
        {
            lock = locks.get(lockKey);
            acquired = lock == null;
            if (acquired)
            {
                locks.put(lockKey, lock = new AtomicInteger());

            }

            // increases lock references
            lock.incrementAndGet();
        }

        try
        {
            synchronized (lock)
            {
                return synchronizedBlock.perform();
            }

        } finally
        {
            // decreases lock references
            if (lock.decrementAndGet() == 0)
            {
                synchronized (locks)
                {
                    // double check, maybe it was referenced again
                    if (lock.get() == 0)
                    {
                        // if no other references are for lock, it will be freed
                        locks.remove(lockKey);

                    }
                }
            }

        }
    }
}
