package com.atlassian.jira.plugins.dvcs.service;

/**
 * Utilities services related to concurrency.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface ConcurrencyService
{

    /**
     * Kind of closure's simulation.
     * 
     * @author Stanislav Dvorscak
     * 
     * @param <R>
     *            result type of closure
     * @param <T>
     *            exception/throwable type of closure
     */
    interface SynchronizedBlock<R, T extends Throwable>
    {

        /**
         * @return closure result
         * @throws T
         *             closure's throwable
         */
        R perform() throws T;

    }

    /**
     * Synchronizes provided 'synchronized block' by provided lock key, with other words - it tries to acquire lock for provide key, if it
     * is free, provided block will be performed, otherwise the whole invocation will be blocked until key become free.
     * 
     * @param synchronizedBlock
     *            for performing
     * @param key
     *            for locking - two keys are equals, if each key part is equal
     * @return result of synchronized block
     * @throws T
     *             throwable of synchronized block
     */
    <R, T extends Throwable> R synchronizedBlock(SynchronizedBlock<R, T> synchronizedBlock, Object... key) throws T;

}
