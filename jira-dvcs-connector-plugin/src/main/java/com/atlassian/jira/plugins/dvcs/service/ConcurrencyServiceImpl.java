package com.atlassian.jira.plugins.dvcs.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private ConcurrentMap<LockKey, ConcurrentLock> locks = new ConcurrentHashMap<LockKey, ConcurrentLock>();

    /**
     * Concurrent lock - each lock must be marked as used by {@link #retain()} and released by appropriate {@link #retain()}.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private static final class ConcurrentLock
    {

        /**
         * Constant used by {@link #atomicReferenceCount} as mark that lock is new.
         */
        private static final int REFERENCE_COUNT_STATUS_NEW = -1;

        /**
         * Constant used by {@link #atomicReferenceCount} as mark that lock will be destroyed in short time and can not be used anymore!
         */
        private static final int REFERENCE_COUNT_STATUS_RELEASED = 0;

        /**
         * Internal reference counter.
         * 
         * @see #REFERENCE_COUNT_STATUS_NEW
         * @see #REFERENCE_COUNT_STATUS_RELEASED
         */
        private final AtomicInteger atomicReferenceCount = new AtomicInteger(REFERENCE_COUNT_STATUS_NEW);

        /**
         * Retains lock until next {@link #release()}.
         * 
         * @return returns true if retaining was successful, otherwise false means lock reference must be refreshed
         */
        public boolean retain()
        {
            int referenceCount;
            do
            {
                referenceCount = atomicReferenceCount.get();

                // if reference count is 0, whole lock is invalid and in short time it will be destroyed
                if (referenceCount == REFERENCE_COUNT_STATUS_RELEASED)
                {
                    return false;
                }

            } while (
            // checks that reference count was not changed and it can be increment safely
            !atomicReferenceCount.compareAndSet(referenceCount, // expected reference count
                    referenceCount == REFERENCE_COUNT_STATUS_NEW ? 1 : referenceCount + 1 // new valid reference count
                    ));

            return true;
        }

        /**
         * Releases lock.
         */
        public void release()
        {
            // decrements reference count
            atomicReferenceCount.decrementAndGet();
        }

        /**
         * @return true if the lock was released and can not be using anymore.
         */
        public boolean isReleased()
        {
            return atomicReferenceCount.get() == REFERENCE_COUNT_STATUS_RELEASED;
        }

    }

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
        ConcurrentLock lock = acquireLock(lockKey);

        try
        {
            synchronized (lock)
            {
                return synchronizedBlock.perform();
            }

        } finally
        {
            releaseLock(lockKey, lock);

        }
    }

    /**
     * Acquires lock for provided key.
     * 
     * @param lockKey
     * @return acquired lock
     */
    private ConcurrentLock acquireLock(LockKey lockKey)
    {
        ConcurrentLock result;
        do
        {
            ConcurrentLock value = new ConcurrentLock();
            if ((result = locks.putIfAbsent(lockKey, value)) == null)
            {
                result = value;
            }

            // increment lock references
        } while (!result.retain());

        return result;
    }

    /**
     * Releases lock for provided key.
     * 
     * @param lockKey
     *            key owner of lock
     * @param lock
     *            for releasing
     */
    private void releaseLock(LockKey lockKey, ConcurrentLock lock)
    {
        // decrement lock references
        lock.release();

        // if no other references are for lock, it means it was released, it will be removed from map
        // release method and removing from map must be called quickly, otherwise lock acquiring can be busy by waiting for lock removing
        if (lock.isReleased())
        {
            locks.remove(lockKey, lock);
        }
    }

}
