package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.fugue.Option;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

public class RetryPredicateSupplier<T>
{
    private final Supplier<T> supplier;
    private final Predicate<T> predicate;
    private final int retryCount;

    public RetryPredicateSupplier(final Supplier<T> supplier, final Predicate<T> predicate)
    {
        this(supplier, predicate, 1);
    }

    public RetryPredicateSupplier(final Supplier<T> supplier, final Predicate<T> predicate, final int retryCount)
    {
        this.supplier = supplier;
        this.predicate = predicate;
        this.retryCount = retryCount;
    }

    public Option<T> get()
    {
        int i = 0;
        while (i < retryCount)
        {
            T result = supplier.get();
            if (predicate.apply(result))
            {
                return Option.some(result);
            }
        }
        return Option.none();
    }
}
